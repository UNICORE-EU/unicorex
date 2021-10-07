/*********************************************************************************
 * Copyright (c) 2006 Forschungszentrum Juelich GmbH 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * (1) Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the disclaimer at the end. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * 
 * (2) Neither the name of Forschungszentrum Juelich GmbH nor the names of its 
 * contributors may be used to endorse or promote products derived from this 
 * software without specific prior written permission.
 * 
 * DISCLAIMER
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *********************************************************************************/


package de.fzj.unicore.xnjs.tsi.remote;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.logging.log4j.Logger;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.XNJSProperties;
import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.ActionStatus;
import de.fzj.unicore.xnjs.ems.BudgetInfo;
import de.fzj.unicore.xnjs.ems.ExecutionContext;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.idb.ApplicationInfo;
import de.fzj.unicore.xnjs.incarnation.ITweaker;
import de.fzj.unicore.xnjs.persistence.IActionStore;
import de.fzj.unicore.xnjs.tsi.BasicExecution;
import de.fzj.unicore.xnjs.tsi.TSI;
import de.fzj.unicore.xnjs.tsi.TSIFactory;
import de.fzj.unicore.xnjs.util.ErrorCode;
import de.fzj.unicore.xnjs.util.IOUtils;
import de.fzj.unicore.xnjs.util.LogUtil;
import eu.unicore.security.Client;
import eu.unicore.util.Log;

/**
 * IExecution interface implemented against a UNICORE TSI daemon<br>
 *
 * @author schuller
 */
@Singleton
public class Execution extends BasicExecution {

	// compute time budget, cached using Xlogin.toString() as key
	private final Cache<String, List<BudgetInfo>> computeBudgets;

	//some events should be logged to the "unicore.xnjs.tsi" category
	private static final Logger tsiLog=LogUtil.getLogger(LogUtil.TSI, Execution.class);

	/**
	 * grace time in MILLISECONDS: how much time to allow before deciding that an job was completed,
	 * in the case the qstat does no longer contain information about the job
	 */
	private int gracePeriod = 120*1000;

	private final TSIConnectionFactory connectionFactory;

	private final IBSSState bss;

	// timeout waiting for a TSI connection (before creating a new one)
	static final int timeout = 10000;

	//key for storing number of attempts of (re-)submission to BSS
	public static final String BSS_SUBMIT_COUNT="JSDL_de.fzj.unicore.xnjs.jsdl.JSDLProcessor_BSSSUBMITCOUNT";

	@Inject
	public Execution(XNJS xnjs, ITweaker tw, TSIConnectionFactory factory, IBSSState bss){
		this.connectionFactory = factory;
		this.bss = bss;
		computeBudgets = buildComputeBudgetCache();
	}

	private Cache<String, List<BudgetInfo>> buildComputeBudgetCache(){
		return CacheBuilder.newBuilder().maximumSize(100)
				.expireAfterAccess(3600, TimeUnit.SECONDS)
				.expireAfterWrite(3600, TimeUnit.SECONDS)
				.build();
	}

	@Override
	public int submit(Action job) throws ExecutionException {
		ApplicationInfo appDescription=job.getApplicationInfo();
		incarnationTweaker.preScript(appDescription, job, idb);
		ExecutionContext ec=job.getExecutionContext();
		int initialStatus = ActionStatus.QUEUED;
		boolean isFirstSubmit = null==job.getProcessingContext().get(BSS_SUBMIT_COUNT);
		boolean runOnLoginNode = ec.isRunOnLoginNode();
		boolean allocateOnly = appDescription.isAllocateOnly();
		String preferredTSIHost=ec.getPreferredExecutionHost();
		
		if(runOnLoginNode && isFirstSubmit){
			String msg = "Execution on login node" + 
		          (preferredTSIHost==null? "" : ", requested node: <"+preferredTSIHost+">");
			job.addLogTrace(msg);
		}

		String credentials = extractBSSCredentials(job);
		boolean addWaitingLoop = properties.getBooleanValue(XNJSProperties.STAGING_FS_WAIT);
		
		String tsiCmdInitial = runOnLoginNode ? 
				TSIUtils.makeExecuteAsyncScript(job,idb, credentials, addWaitingLoop) : 
				TSIUtils.makeSubmitCommand(job, idb, grounder, properties, credentials, addWaitingLoop);
		String tsiCmd=incarnationTweaker.postScript(appDescription, job, idb, tsiCmdInitial);

		String tsiHost=null;
		String msg;
		String res;
		String idLine="";
		
		try{
			Lock lock = null;
			try{
				try(TSIConnection conn = connectionFactory.getTSIConnection(job.getClient(),preferredTSIHost,-1)){
					tsiHost=conn.getTSIHostName();
					lock = runOnLoginNode ? bss.getNodeLock(tsiHost) : bss.getBSSLock();
					boolean locked = lock.tryLock(120, TimeUnit.SECONDS);
					if(!locked) {
						throw new ExecutionException(new ErrorCode(ErrorCode.ERR_TSI_COMMUNICATION,
								"Submission to TSI failed: Could not acquire lock (timeout)"));
					}
					res=conn.send(tsiCmd);
					idLine=conn.getIdLine();
					if(isFirstSubmit) {
						job.addLogTrace("Command is:");
						job.addLogTrace(tsiCmd);
					}
					if(res.contains("TSI_FAILED")){
						job.addLogTrace("TSI reply: FAILED.");
						throw new ExecutionException(new ErrorCode(ErrorCode.ERR_TSI_COMMUNICATION,"Submission to TSI failed. Reply was <"+res+">"));
					}
				}
				job.addLogTrace("TSI reply: submission OK.");
				String bssid=res.trim();

				msg="Submitted to TSI as ["+idLine+"] with BSSID="+bssid;
				
				String internalID = bssid;
				BSS_STATE initialState = BSS_STATE.QUEUED;
				
				if(runOnLoginNode || allocateOnly){
					long iPid = readPID(job, tsiHost);
					internalID = "INTERACTIVE_"+tsiHost+"_"+iPid;
					msg = "Submitted to TSI as ["+idLine+"] with PID="+iPid+" on ["+tsiHost+"]";
					job.getExecutionContext().setPreferredExecutionHost(tsiHost);
					if(!allocateOnly) {
						initialState = BSS_STATE.RUNNING;
						initialStatus = ActionStatus.RUNNING;
					}
				}
				job.setBSID(internalID);
				BSSInfo newJob=new BSSInfo(internalID,job.getUUID(), initialState);
				bss.putBSSInfo(newJob);
			}
			finally{
				if(lock!=null)lock.unlock();
			}
			jobExecLogger.debug(msg);
			job.addLogTrace(msg);

		}catch(Exception ex){
			jobExecLogger.error("Error submitting job.",ex);
			throw new ExecutionException(ex);
		}
		return initialStatus;
	}

	//for interactive execution, read the PID of the submitted script
	private long readPID(Action job, String preferredTSINode)throws IOException, ExecutionException, InterruptedException {
		Thread.sleep(3000); // async submit, so PID file may not yet be written - let's try and avoid errors later
		TSI tsi = tsiFactory.createTSI(job.getClient(), preferredTSINode);
		ExecutionContext ec=job.getExecutionContext();
		String pidFile=ec.getOutcomeDirectory()+"/"+ec.getPIDFileName();
		jobExecLogger.debug("Reading PID from "+pidFile);
		long pid=0;
		for(int i=0; i<3; i++){
			try{
				pid = Long.valueOf(IOUtils.readTSIFile(tsi, pidFile, 1024).trim());
				if(pid>0)return pid;
			}
			catch(Exception ex){
				String msg = Log.createFaultMessage("Error reading PID file <"+pidFile+"> on <"+preferredTSINode+"> (attempt "+(i+1)+")"+(i<2?", will retry":""), ex);
				tsiLog.warn(msg);
				Thread.sleep(3000+i*1000);
			}
		}
		throw new ExecutionException(ErrorCode.ERR_INTERACTIVE_SUBMIT_FAILURE, "Could not read PID file <"+pidFile+"> on <"+preferredTSINode+">");
	}
	
	private String readAllocationID(Action job, String preferredTSINode) throws IOException, ExecutionException, InterruptedException {
		TSI tsi = tsiFactory.createTSI(job.getClient(), preferredTSINode);
		ExecutionContext ec = job.getExecutionContext();
		String file = ec.getOutcomeDirectory()+"/ALLOCATION_ID";
		jobExecLogger.debug("Reading allocation ID from " + file);
		for(int i=0; i<3; i++){
			try{
				String pid = IOUtils.readTSIFile(tsi, file, 1024).trim();
				if(pid.length()>0)return pid;
			}
			catch(Exception ex){
				String msg = Log.createFaultMessage("Error reading file <"+file+"> on <"+preferredTSINode+"> (attempt "+(i+1)+")"+(i<2?", will retry":""), ex);
				tsiLog.warn(msg);
				Thread.sleep(3000+i*1000);
			}
		}
		throw new IOException("Could not read PID file <"+file+"> on <"+preferredTSINode+">");
	}
	
	/*
	 *  Some back-ends may require credentials for status checks or output retrieval.
	 *  This method extracts these from the job.
	 */
	protected String extractBSSCredentials(Action job){
		return null;
	}
	
	// TODO refactor into smaller pieces
	public void updateStatus(Action job) throws ExecutionException {
		try{		
			final String bssID=job.getBSID();
			final String jobID=job.getUUID();
			if(bssID==null){
				throw new Exception("Status check can't be done: action "+job.getUUID()+" does not have a batch system ID.");
			}
			BSSInfo info = bss.getBSSInfo(bssID);
			if(info==null) {
				jobExecLogger.debug("No status info for action {} bssid={}", job.getUUID(), bssID);
				return;
			}
			
			jobExecLogger.debug("Action {} bssid={} is {}",job.getUUID(), bssID, info.bssState);
			if(info.queue!=null){
				job.getExecutionContext().setBatchQueue(info.queue);
				job.setDirty();
			}

			// re-set grace period if we have a valid status
			if(!BSS_STATE.CHECKING_FOR_EXIT_CODE.equals(info.bssState)){
				resetGracePeriod(job);
			}
			
			if(BSS_STATE.QUEUED.equals(info.bssState)){
				job.setStatus(ActionStatus.QUEUED);
			}
			else if(BSS_STATE.RUNNING.equals(info.bssState)){
				//get progress indication 
				//TODO this is not working if the action is suspended...
				updateProgress(job);
				
				updateEstimatedEndtime(job);
				job.setStatus(ActionStatus.RUNNING);
			}
			else if(BSS_STATE.UNKNOWN.equals(info.bssState) || BSS_STATE.CHECKING_FOR_EXIT_CODE.equals(info.bssState)){
				//check if exit code can be read
				boolean haveExitCode=getExitCode(job);
				if(!haveExitCode){
					if(!hasGracePeriodPassed(job)){
						jobExecLogger.debug("Waiting for {} BSS id={} to finish and write exit code file.", jobID, bssID);
						info.bssState = BSS_STATE.CHECKING_FOR_EXIT_CODE;
					}
					else {
						jobExecLogger.debug("Assuming job  {} BSS id={} is completed.", jobID, bssID);
						info.bssState = BSS_STATE.COMPLETED;
					}
				}
				else{
					jobExecLogger.debug("Have exit code for {}, assuming it is completed.", jobID);
					info.bssState = BSS_STATE.COMPLETED;
				}
			}

			if (BSS_STATE.COMPLETED.equals(info.bssState)){
				//check exit code
				if(job.getExecutionContext().getExitCode()==null)getExitCode(job);
				Integer exitCode=job.getExecutionContext().getExitCode();

				if(exitCode!=null){
					if(job.getApplicationInfo().isAllocateOnly() &&
						job.getProcessingContext().get("ALLOCATION_COMPLETE")==null)
					{
						String allocID = readAllocationID(job, job.getExecutionContext().getPreferredExecutionHost());
						job.addLogTrace("Allocation successful, BSS ID = "+allocID);
						bss.removeBSSInfo(bssID);
						job.setBSID(allocID);
						bss.putBSSInfo(new BSSInfo(allocID, jobID, BSS_STATE.RUNNING));
						updateEstimatedEndtime(job);
						job.setStatus(ActionStatus.RUNNING);
						job.getProcessingContext().put("ALLOCATION_COMPLETE", "true");
						return;
					}
					job.addLogTrace("Job completed on BSS.");
					job.setStatus(ActionStatus.POSTPROCESSING);
					try{
						job.setBssDetails(getBSSJobDetails(job));
					}catch(Exception ex) {
						job.addLogTrace("Could not get BSS job details.");
					}
					bss.removeBSSInfo(bssID);
				}
				else{
					//No exit code. For example, due to NFS problems it might occur that we need to re-check
					if(job.getProcessingContext().get(EXITCODE_RECHECK)==null){
						job.getProcessingContext().put(EXITCODE_RECHECK, Boolean.TRUE);
						int timeout = 1000 * properties.getIntValue(XNJSProperties.STAGING_FS_GRACE);
						job.getProcessingContext().put(CUSTOM_GRACE_PERIOD, Integer.valueOf(timeout));
						job.getProcessingContext().put(GRACE_PERIOD_start, System.currentTimeMillis());
						job.setDirty();
						jobExecLogger.debug("Will allow job "+jobID+" a grace period of "+timeout+" millis for exit code file.");
					}
					else{
						if(hasGracePeriodPassed(job)){
							//Still no exit code -> assume the user script was not completed successfully!
							if(!job.getExecutionContext().isRunOnLoginNode()){
								try{
									String details=getBSSJobDetails(job);
									job.addLogTrace("Detailed job information from batch system: "+details);
									job.setBssDetails(details);
								}catch(ExecutionException ee){
									//we already logged	it
								}
								job.fail("Job did not complete normally on BSS, please check standard error file and job log for more information.");
							}
							else{
								job.fail("Execution was not completed (no exit code file found), please check standard error file <"+job.getExecutionContext().getStderr()+">");
							}
							bss.removeBSSInfo(bssID);
						}
					}
				}
			}
		}catch(Exception ex){
			jobExecLogger.error("Error updating job status.",ex);
			throw new ExecutionException(ex);
		}
	}

	private boolean hasGracePeriodPassed(Action job){
		int myGracePeriod = gracePeriod;
		Long timeOfFirstStatusCheck=(Long)job.getProcessingContext().get(GRACE_PERIOD_start);
		if(timeOfFirstStatusCheck==null){
			timeOfFirstStatusCheck = Long.valueOf(System.currentTimeMillis());
			job.getProcessingContext().put(GRACE_PERIOD_start, timeOfFirstStatusCheck);
			job.setDirty();
		}
		//check if a custom grace period has been defined
		Integer g=(Integer)job.getProcessingContext().get(CUSTOM_GRACE_PERIOD);
		if(g!=null)myGracePeriod = g;
		return System.currentTimeMillis()>timeOfFirstStatusCheck+myGracePeriod;
	}
	
	private void resetGracePeriod(Action job){
		job.getProcessingContext().remove(GRACE_PERIOD_start);
		job.setDirty();
	}


	@Override
	public void abort(Action job) throws ExecutionException {
		//if job has been submitted, abort it on the BSS
		final String bssid=job.getBSID();
		if(bssid==null){
			throw new IllegalArgumentException("Can't abort: no batch system ID.");
		}
		else{
			try{
				BSSInfo info = bss.getBSSInfo(bssid);
				BSS_STATE status  = info!=null ? info.bssState : null;
				if(status!=null) { 
					jobExecLogger.debug("Aborting job <"+bssid+"> on TSI server");
					if(job.getExecutionContext().isRunOnLoginNode()){
						terminateInteractiveJob(job, false);
					}
					else{
						runTSICommand(TSIUtils.makeAbortCommand(bssid), job.getClient(), null, true);
					}
				}
			}catch(Exception ex){//wrap it
				jobExecLogger.error("Error aborting.",ex);
				throw new ExecutionException(ex);
			}
		}
	}

	/**
	 * terminate the interactive job. 
	 * If the 'kill' flag is true, a SIGKILL will be sent instead of the default SIGTERM
	 */
	protected void terminateInteractiveJob(Action job, boolean kill) throws ExecutionException, IOException {
		String[] tok = job.getBSID().split("_");
		String pid = tok[tok.length-1];
		String tsiNode = job.getExecutionContext().getPreferredExecutionHost();
		String signal = kill? "-9 ": "";
		String script = "pkill "+signal+"-P "+pid+" ; kill "+signal+pid;
		try(TSIConnection conn = connectionFactory.getTSIConnection(job.getClient(), tsiNode, timeout)) {
			String res=conn.send(TSIUtils.makeExecuteScript(script, null, idb, extractBSSCredentials(job)));
			if(res==null || !res.startsWith("TSI_OK")){
				throw new ExecutionException("Could not get terminate process. TSI reply: "+res);
			}
		}
	}
	
	@Override
	public int getNumberOfQueuedJobs() {
		return bss.getBSSSummary().queued;
	}

	@Override
	public int getNumberOfRunningJobs() {
		return bss.getBSSSummary().running;
	}

	@Override
	public int getTotalNumberOfJobs() {
		return bss.getBSSSummary().total;
	}

	@Override
	public Map<String,Integer>getQueueFill(){
		return bss.getBSSSummary().queueFilling;
	}

	/**
	 * @see de.fzj.unicore.xnjs.tsi.BasicExecution#pause(de.fzj.unicore.xnjs.ems.Action)
	 */
	@Override
	public void pause(Action job) throws ExecutionException {
		final String bssid = job.getBSID();
		if(bssid == null) {
			throw new IllegalArgumentException("Can't abort: no batch system ID.");
		}
		try {
			BSSInfo info = bss.getBSSInfo(bssid);
			BSS_STATE status = info!=null ? info.bssState : null;
			if(status != null) {
				jobExecLogger.debug("Pausing job <"+bssid+"> on TSI server.");
				runTSICommand(TSIUtils.makePauseCommand(bssid), job.getClient(), null, true);
			}
		}
		catch (Exception e) {
			jobExecLogger.error("Error pausing.", e);
			throw new ExecutionException(e);
		}
	}

	/**
	 * @see de.fzj.unicore.xnjs.tsi.BasicExecution#resume(de.fzj.unicore.xnjs.ems.Action)
	 */
	@Override
	public void resume(Action job) throws ExecutionException {
		final String bssid = job.getBSID();
		if(bssid == null) {
			throw new IllegalArgumentException("Can't abort: no batch system ID.");
		}

		try {
			BSSInfo info = bss.getBSSInfo(bssid);
			BSS_STATE status = info!=null ? info.bssState : null;
			if(status != null) {
				jobExecLogger.debug("Resuming job <"+bssid+"> on TSI server.");
				runTSICommand(TSIUtils.makeResumeCommand(bssid), job.getClient(), null, true);
			}
		} catch (Exception e) {
			jobExecLogger.error("Error resuming.", e);
			throw new ExecutionException(e);
		}
	}

	/**
	 * try to obtain detailed info about a job from the BSS
	 * 
	 * @param job - the job to query
	 * @return job detail string (can be long!)
	 * @throws ExecutionException
	 */
	@Override
	public String getBSSJobDetails(Action job) throws ExecutionException {
		final String bssid = job.getBSID();
		if(bssid == null || (job.getExecutionContext()!=null && job.getExecutionContext().isRunOnLoginNode())) {
			return "N/A";
		}
		try {
			jobExecLogger.debug("Getting details for job <"+bssid+"> from TSI.");
			String det = runTSICommand(TSIUtils.makeGetJobInfoCommand(bssid, extractBSSCredentials(job)), job.getClient(), null, true);
			return det.replace("TSI_OK", "").trim();
		} catch (Exception e) {
			jobExecLogger.error("Error getting job details.", e);
			throw new ExecutionException(e);
		}
	}
	
	@Override
	public void initialise(IActionStore jobStore) throws Exception {
		Collection<String> ids = jobStore.getActiveUniqueIDs();
		int interactive = 0;
		for(String id: ids) {
			Action a = jobStore.get(id);
			if(a.getBSID()==null)continue;
			BSS_STATE state = BSS_STATE.UNKNOWN;
			if(a.getExecutionContext().isRunOnLoginNode()) {
				interactive++;
				state = BSS_STATE.RUNNING;
			}
			bss.putBSSInfo(new BSSInfo(a.getBSID(), a.getUUID(), state));
		}
		tsiLog.info("Have <"+ids.size()+"> active jobs, with <"+interactive+"> running on login node(s)");
		bss.toggleStatusUpdates(true);
	}

	@Override
	public List<BudgetInfo> getComputeTimeBudget(Client client) throws ExecutionException {
		try{
			if(client == null || client.getXlogin()==null || client.getXlogin().getUserName()==null){
				return super.getComputeTimeBudget(null);
			}
			String key = client.getXlogin().toString();
			return computeBudgets.get(key, new Loader(client, tsiFactory));
		}catch(Exception e){
			throw new ExecutionException(e);
		}
	}

	public static class Loader implements Callable<List<BudgetInfo>>{
		final Client client;
		final TSIFactory tsiFactory;
		public Loader(Client c, TSIFactory tsiFactory) {
			this.client = c;
			this.tsiFactory = tsiFactory; 
		}
		public List<BudgetInfo> call() throws Exception {
			tsiLog.info("Querying compute time for "+client.getXlogin());
			return ((RemoteTSI)tsiFactory.createTSI(client)).getComputeTimeBudget();
		}
	}
	/**
	 * send a command to the TSI
	 * 
	 * @param command - the command to send
	 * @param client
	 * @param preferredTSIHost - the preferred TSI node or <code>null</code> if you don't care
	 * @param check - whether to check the reply for the TSI_OK signal
	 * @return the TSI reply
	 * @throws Exception if the TSI reply is not "TSI_OK"
	 */
	protected String runTSICommand(String command, Client client, String preferredTSIHost, boolean check) throws Exception {
		try(TSIConnection conn = connectionFactory.getTSIConnection(client,preferredTSIHost,-1)){
			String res = conn.send(command);
			if(check && !res.contains("TSI_OK")){
				throw new Exception("Getting job details on TSI failed: reply was "+res);
			}
			return res;
		}
	}
	
	public static enum BSS_STATE {
		UNKNOWN, QUEUED, RUNNING, COMPLETED, SUSPENDED, CHECKING_FOR_EXIT_CODE;
	}
	
	public static class BSSInfo{
		String bssID;
		String jobID;
		BSS_STATE bssState;
		String queue;

		public BSSInfo(){}

		public BSSInfo(String bssID,String jobID, BSS_STATE bssState){
			this.bssID=bssID;
			this.jobID=jobID;
			this.bssState=bssState;
		}

		public String toString(){
			return "BSSID="+bssID+" JOBID="+jobID+" STATUS="+bssState+(queue!=null?" QUEUE="+queue:"");
		}

	}

	public static class BSSSummary{
		final int running;
		final int queued;
		final int total;
		final Map<String,Integer>queueFilling;


		public BSSSummary(List<BSSSummary> parts){
			Map<String,Integer> fill = new HashMap<>();
			int running = 0;
			int queued = 0;
			int total = 0;
			for(BSSSummary part: parts) {
				running+=part.running;
				queued+=part.queued;
				total+=part.total;
				if(part.queueFilling.size()>0) {
					fill = part.queueFilling;
				}
			}
			this.running = running;
			this.queued = queued;
			this.total = total;
			this.queueFilling = fill;
		}
		
		public BSSSummary(int running, int queued, int total, Map<String,Integer>queueFilling){
			this.running=running;
			this.queued=queued;
			this.total=total;
			this.queueFilling=queueFilling;
		}

		public BSSSummary(){
			this(0,0,0,new HashMap<>());
		}

		public String toString(){
			String res="BSS: running="+running+" queued="+queued+" total="+total;
			if(queueFilling.size()>0){
				res+=" queueFill="+queueFilling;
			}
			return res;
		}
	}

}
