package eu.unicore.xnjs.tsi.remote;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import eu.unicore.security.Client;
import eu.unicore.util.Log;
import eu.unicore.xnjs.XNJSConstants;
import eu.unicore.xnjs.ems.Action;
import eu.unicore.xnjs.ems.ActionStatus;
import eu.unicore.xnjs.ems.BudgetInfo;
import eu.unicore.xnjs.ems.ExecutionContext;
import eu.unicore.xnjs.ems.ExecutionException;
import eu.unicore.xnjs.ems.processors.AsyncCommandProcessor.SubCommand;
import eu.unicore.xnjs.idb.ApplicationInfo;
import eu.unicore.xnjs.idb.Partition;
import eu.unicore.xnjs.io.IOProperties;
import eu.unicore.xnjs.persistence.IActionStore;
import eu.unicore.xnjs.resources.IntResource;
import eu.unicore.xnjs.resources.Resource.Category;
import eu.unicore.xnjs.tsi.BasicExecution;
import eu.unicore.xnjs.tsi.TSI;
import eu.unicore.xnjs.tsi.TSIBusyException;
import eu.unicore.xnjs.tsi.TSIFactory;
import eu.unicore.xnjs.tsi.TSIProblem;
import eu.unicore.xnjs.util.ErrorCode;
import eu.unicore.xnjs.util.IOUtils;
import eu.unicore.xnjs.util.LogUtil;
import eu.unicore.xnjs.util.UFTPUtils;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

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

	private final TSIMessages tsiMessages;

	private final IBSSState bss;

	private final TSIProperties tsiProperties;

	// timeout waiting for a TSI connection (before creating a new one)
	static final int timeout = 10000;

	//key for storing number of attempts of (re-)submission to BSS
	public static final String BSS_SUBMIT_COUNT="JSDL_de.fzj.unicore.xnjs.jsdl.JSDLProcessor_BSSSUBMITCOUNT";

	@Inject
	public Execution(TSIConnectionFactory factory, IBSSState bss, TSIMessages tsiMessages, TSIProperties tsiProperties){
		this.connectionFactory = factory;
		this.tsiProperties = tsiProperties;
		this.bss = bss;
		this.tsiMessages = tsiMessages;
		this.bss.init();
		computeBudgets = buildComputeBudgetCache();
	}

	private Cache<String, List<BudgetInfo>> buildComputeBudgetCache(){
		return CacheBuilder.newBuilder().maximumSize(100)
				.expireAfterAccess(3600, TimeUnit.SECONDS)
				.expireAfterWrite(3600, TimeUnit.SECONDS)
				.build();
	}

	@Override
	public int submit(Action job) throws ExecutionException, TSIBusyException {
		int serverLimit = tsiProperties.getIntValue(TSIProperties.JOBLIMIT);
		Integer customLimit = job.getProcessingContext().getAs("CLASSICTSI.jobLimit", Integer.class);
		int jobLimit = customLimit!=null? customLimit.intValue() : serverLimit;
		if(jobLimit>0) {
			int numJobs = getNumberOfRunningJobs() + getNumberOfQueuedJobs();
			if(numJobs>=jobLimit) {
				throw new TSIBusyException("Too many running jobs.");
			}
		}
		ApplicationInfo appDescription = job.getApplicationInfo();
		ExecutionContext ec = job.getExecutionContext();
		int initialStatus = ActionStatus.QUEUED;
		boolean isFirstSubmit = null==job.getProcessingContext().get(BSS_SUBMIT_COUNT);
		boolean runOnLoginNode = ec.isRunOnLoginNode();
		boolean allocateOnly = appDescription.isAllocateOnly();
		if(job.getProcessingContext().get(TSIMessages.ALLOCATION_ID)!=null && isFirstSubmit) {
			String allocationID = job.getProcessingContext().getAs(TSIMessages.ALLOCATION_ID, String.class);
			job.addLogTrace("Submitting into allocation with ID <"+allocationID+">");
		}
		String preferredTSIHost = ec.getPreferredExecutionHost();
		if(runOnLoginNode && isFirstSubmit){
			String msg = "Execution on login node" + 
		          (preferredTSIHost==null? "" : ", requested node: <"+preferredTSIHost+">");
			job.addLogTrace(msg);
		}
		String tsiHost = null;
		String msg;
		String res;
		String idLine = "";

		Lock lock = null;
		boolean locked = false;
		boolean runOnLoginSupport = false;
		try{
			try(TSIConnection conn = connectionFactory.getTSIConnection(job.getClient(),preferredTSIHost,-1)){
				runOnLoginSupport = conn.compareVersion("10.2.0");
				String tsiCmd = createTSIScript(job, runOnLoginSupport);
				tsiHost = conn.getTSIHostName();
				lock = runOnLoginNode ? bss.getNodeLock(tsiHost) : bss.getBSSLock();
				locked = lock.tryLock(120, TimeUnit.SECONDS);
				if(!locked) {
					throw new TSIProblem(preferredTSIHost, ErrorCode.ERR_TSI_COMMUNICATION,
							"Could not acquire TSI submit lock (timeout)", null);
				}
				res = conn.send(tsiCmd);
				idLine = conn.getIdLine();
				if(isFirstSubmit) {
					job.addLogTrace("Command is: "+tsiCmd);
				}
				if(res.contains("TSI_FAILED")){
					job.addLogTrace("Submission attempt failed: "+res);
					throw new TSIProblem(tsiHost, ErrorCode.ERR_TSI_EXECUTION, res, null);
				}
			}
			job.addLogTrace("TSI reply: submission OK.");
			String bssid = res.trim();
			msg="Submitted to TSI as ["+idLine+"] with BSSID="+bssid;
			String internalID = bssid;
			BSS_STATE initialState = BSS_STATE.QUEUED;
			if(runOnLoginNode || allocateOnly){
				boolean readPIDFromFile = !runOnLoginSupport || allocateOnly;
				long iPid = readPIDFromFile ? readPID(job, tsiHost) : Long.valueOf(bssid);
				internalID = "INTERACTIVE_"+tsiHost+"_"+iPid;
				msg = "Submitted to TSI as ["+idLine+"] with PID="+iPid+" on ["+tsiHost+"]";
				if(!allocateOnly) {
					initialState = BSS_STATE.RUNNING;
					initialStatus = ActionStatus.RUNNING;
				}
			}
			job.setBSID(internalID);
			ec.setLocation(tsiHost);
			BSSInfo newJob=new BSSInfo(internalID,job.getUUID(), initialState);
			newJob.wantsBSSStateChangeNotifications = 
					job.getNotificationURLs()!=null && 
					!job.getNotificationURLs().isEmpty()
					&& job.getNotifyBSSStates()!=null
					&& !job.getNotifyBSSStates().isEmpty()
					;
			bss.putBSSInfo(newJob);
			jobExecLogger.debug(msg);
			job.addLogTrace(msg);
		}catch(Exception ex){
			throw ExecutionException.wrapped(ex);
		}finally{
			if(locked)lock.unlock();
		}
		return initialStatus;
	}
	
	private String createTSIScript(Action job, boolean runOnLoginSupport) throws ExecutionException {
		if(XNJSConstants.asyncCommandType.equals(job.getType())){
			SubCommand sc = (SubCommand)job.getAjd();
			if(sc.type == SubCommand.UFTP) {
				try{
					return UFTPUtils.makeUFTPCommand(new JSONObject(sc.cmd), job.getExecutionContext());
				}catch(JSONException je) {
					throw new ExecutionException(je);
				}
			}
		}
		return createDefaultTSIScript(job, runOnLoginSupport);
	}

	private String createDefaultTSIScript(Action job, boolean runOnLoginSupport) throws ExecutionException {
		return job.getExecutionContext().isRunOnLoginNode() ? 
				tsiMessages.makeRunOnLoginNodeCommand(job, extractBSSCredentials(job), runOnLoginSupport) : 
				tsiMessages.makeSubmitCommand(job, extractBSSCredentials(job));
	}
	
	//for async script execution, read the PID of the submitted script
	private long readPID(Action job, String preferredTSINode)throws IOException, ExecutionException, InterruptedException {
		Thread.sleep(3000); // async submit, so PID file may not yet be written - let's try and avoid errors later
		TSI tsi = tsiFactory.createTSI(job.getClient(), preferredTSINode);
		ExecutionContext ec=job.getExecutionContext();
		String pidFile=ec.getOutputDirectory()+"/"+ec.getPIDFileName();
		jobExecLogger.debug("Reading PID from <{}>",pidFile);
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
		throw new ExecutionException(ErrorCode.ERR_INTERACTIVE_SUBMIT_FAILURE,
				"Could not read PID file <"+pidFile+"> on <"+preferredTSINode+">");
	}

	private String readAllocationID(Action job, String preferredTSINode) throws IOException, ExecutionException, InterruptedException {
		TSI tsi = tsiFactory.createTSI(job.getClient(), preferredTSINode);
		ExecutionContext ec = job.getExecutionContext();
		String file = ec.getOutputDirectory()+"/"+TSIMessages.ALLOCATION_ID;
		jobExecLogger.debug("Reading allocation ID from <{}>", file);
		for(int i=0; i<3; i++){
			try{
				String[] allocationInfo = tsiMessages.readAllocationID(IOUtils.readTSIFile(tsi, file, 1024));
				String allocationID = allocationInfo[0];
				job.getProcessingContext().put(TSIMessages.ALLOCATION_ID, allocationInfo[1]);
				job.setDirty();
				return allocationID;
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

	public void updateStatus(Action job) throws ExecutionException {
		try{		
			final String bssID=job.getBSID();
			if(bssID==null){
				throw new Exception("Status check can't be done: action <"+job.getUUID()+"> does not have a batch system ID.");
			}
			BSSInfo info = bss.getBSSInfo(bssID);
			if(info==null) {
				jobExecLogger.debug("No status info for action <{}> bssid={}", job.getUUID(), bssID);
				return;
			}
			if (hasJobCompleted(job, info)){
				handleCompleted(job);
			}
		}catch(Exception ex){
			throw ExecutionException.wrapped(ex);
		}
	}

	private boolean hasJobCompleted(Action job, BSSInfo info) throws Exception {
		final String bssID = job.getBSID();
		final String jobID = job.getUUID();

		jobExecLogger.debug("Action <{}> bssid={} is <{}>", jobID, bssID, info.bssState);
		if(info.queue!=null){
			job.getExecutionContext().setBatchQueue(info.queue);
			job.setDirty();
		}
		// re-set grace period if we have a valid status
		if(!BSS_STATE.CHECKING_FOR_EXIT_CODE.equals(info.bssState)){
			resetGracePeriod(job);
		}

		switch (info.bssState) {

		case QUEUED:
			job.setStatus(ActionStatus.QUEUED);
			break;

		case RUNNING:
			//TODO progress update is not working if action is suspended
			updateProgress(job);
			updateEstimatedEndtime(job);
			job.setStatus(ActionStatus.RUNNING);
			break;

		case CHECKING_FOR_EXIT_CODE:
			boolean haveExitCode = readExitCode(job);
			if(!haveExitCode){
				if(!hasGracePeriodPassed(job)){
					jobExecLogger.debug("Waiting for job <{}> BSS id={} to finish and write exit code file.", jobID, bssID);
					info.bssState = BSS_STATE.CHECKING_FOR_EXIT_CODE;
				}
				else {
					jobExecLogger.debug("Assuming job <{}> BSS id={} is completed.", jobID, bssID);
					info.bssState = BSS_STATE.COMPLETED;
				}
			}
			else{
				jobExecLogger.debug("Have exit code for job <{}>, assuming it is completed.", jobID);
				info.bssState = BSS_STATE.COMPLETED;
			}
			break;

		default:
			break;
		}

		return BSS_STATE.COMPLETED.equals(info.bssState);
	}

	private void handleCompleted(Action job) throws Exception {
		final String bssID = job.getBSID();
		final String jobID = job.getUUID();

		//check exit code
		if(job.getExecutionContext().getExitCode()==null)readExitCode(job);
		Integer exitCode=job.getExecutionContext().getExitCode();

		if(exitCode!=null){
			if(job.getApplicationInfo().isAllocateOnly() &&
				job.getProcessingContext().get("ALLOCATION_COMPLETE")==null) {
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
				int timeout = 1000 * ioProperties.getIntValue(IOProperties.STAGING_FS_GRACE);
				job.getProcessingContext().put(CUSTOM_GRACE_PERIOD, Integer.valueOf(timeout));
				job.getProcessingContext().put(GRACE_PERIOD_start, System.currentTimeMillis());
				job.setDirty();
				jobExecLogger.debug("Will allow job {} a grace period of {} millis for exit code file.", jobID, timeout);
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
				bss.removeBSSInfo(bssid);
				BSS_STATE status  = info!=null ? info.bssState : null;
				if(status!=null) { 
					jobExecLogger.debug("Aborting job <{}> on TSI server", bssid);
					if(job.getExecutionContext().isRunOnLoginNode()){
						terminateInteractiveJob(job);
					}
					else{
						runTSICommand(tsiMessages.makeAbortCommand(bssid), job.getClient(), null, true);
					}
				}
			}catch(Exception ex){
				throw ExecutionException.wrapped(ex);
			}
		}
	}

	/**
	 * terminate the interactive job. 
	 * If the 'kill' flag is true, a SIGKILL will be sent instead of the default SIGTERM
	 */
	protected void terminateInteractiveJob(Action job) throws ExecutionException, IOException {
		String[] tok = job.getBSID().split("_");
		String pid = tok[tok.length-1];
		String tsiNode = job.getExecutionContext().getLocation();
		String script = tsiMessages.getAbortProcessCommand(pid);
		if(!TSIConnection.doCompareVersions(connectionFactory.getTSIVersion(),"9.1.1")){
			script = "pkill -P "+pid+"; kill "+pid;
		}
		try(TSIConnection conn = connectionFactory.getTSIConnection(job.getClient(), tsiNode, timeout)) {
			String res = conn.send(tsiMessages.makeExecuteScript(script, null, extractBSSCredentials(job)));
			TSIMessages.checkNoErrors(res, conn.getTSIHostName());
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

	@Override
	public void pause(Action job) throws ExecutionException {
		final String bssid = job.getBSID();
		if(bssid == null) {
			throw new IllegalArgumentException("Can't pause: no batch system ID.");
		}
		try {
			BSSInfo info = bss.getBSSInfo(bssid);
			BSS_STATE status = info!=null ? info.bssState : null;
			if(status != null) {
				jobExecLogger.debug("Pausing job <{}> on TSI server.", bssid);
				runTSICommand(tsiMessages.makePauseCommand(bssid), job.getClient(), null, true);
			}
		}
		catch (Exception e) {
			throw ExecutionException.wrapped(e);
		}
	}

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
				jobExecLogger.debug("Resuming job <{}> on TSI server.", bssid);
				runTSICommand(tsiMessages.makeResumeCommand(bssid), job.getClient(), null, true);
			}
		} catch (Exception e) {
			throw ExecutionException.wrapped(e);
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
		try{
			jobExecLogger.debug("Getting details for job <{}> from TSI.", bssid);
			String det = runTSICommand(tsiMessages.makeGetJobInfoCommand(bssid, extractBSSCredentials(job)), job.getClient(), null, true);
			return det.replace("TSI_OK", "").trim();
		}catch(Exception ex) {
			throw ExecutionException.wrapped(ex);
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
			if(a.getApplicationInfo().isAllocateOnly()) {
				state = BSS_STATE.RUNNING;
			}
			bss.putBSSInfo(new BSSInfo(a.getBSID(), a.getUUID(), state));
		}
		tsiLog.info("Have <{}> active jobs, with <{}> running on login node(s)",
				ids.size(), interactive);
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
			throw ExecutionException.wrapped(e);
		}
	}

	public static class Loader implements Callable<List<BudgetInfo>>{
		final Client client;
		final TSIFactory tsiFactory;
		public Loader(Client c, TSIFactory tsiFactory) {
			this.client = c;
			this.tsiFactory = tsiFactory;
		}
		public List<BudgetInfo> call() throws ExecutionException {
			tsiLog.debug("Querying compute time for xlogin <{}>", client.getXlogin());
			return ((RemoteTSI)tsiFactory.createTSI(client, null)).getComputeTimeBudget();
		}
	}
	/**
	 * send a command to the TSI
	 * 
	 * @param command - the command to send
	 * @param client - if null, the "generic" {@link TSIProperties#getBSSUser()} is used
	 * @param preferredTSIHost - the preferred TSI node or <code>null</code> if you don't care
	 * @param check - whether to check the reply for the TSI_OK signal
	 * @return the TSI reply
	 * @throws Exception if the TSI reply is not "TSI_OK"
	 */
	protected String runTSICommand(String command, Client client, String preferredTSIHost, boolean check) throws Exception {
		try(TSIConnection conn = client!=null ?
				connectionFactory.getTSIConnection(client, preferredTSIHost, -1) :
				connectionFactory.getTSIConnection(tsiProperties.getBSSUser(), "NONE", preferredTSIHost, -1))
		{
			String res = conn.send(command);
			if(check && !res.contains("TSI_OK")){
				throw new ExecutionException("TSI call failed: reply was "+res);
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
		String rawBSSState;
		boolean wantsBSSStateChangeNotifications = false;

		public BSSInfo(String bssID,String jobID, BSS_STATE bssState){
			this.bssID=bssID;
			this.jobID=jobID;
			this.bssState=bssState;
		}

	}

	public static class BSSSummary{
		int running;
		int queued;
		int total;
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

	public boolean isBeingTracked(Action job) throws ExecutionException{
		return job!=null && job.getBSID()!=null && bss.getBSSInfo(job.getBSID())!=null;
	}

	public Collection<Partition> getPartitionInfo() throws Exception {
		Collection<Partition> result = new HashSet<>();
		String infoS = runTSICommand(tsiMessages.makeGetPartitionsCommand(), null, null, true);
		infoS = infoS.replace("TSI_OK", "").trim();
		System.out.println(infoS);
		JSONObject jo = new JSONObject(infoS);
		for(String partitionName: jo.keySet()) {
			JSONObject partitionInfo = jo.getJSONObject(partitionName);
			Partition p = new Partition();
			p.setName(partitionName);
			p.setDefaultPartition(partitionInfo.getBoolean("isDefault"));
			// minimum info is number of nodes
			long n = partitionInfo.getInt("Nodes");
			IntResource nodes = new IntResource("Nodes", null, n, 1l, Category.PROCESSING);
			p.getResources().putResource(nodes);
			result.add(p);
		}
		return result;
	}

}
