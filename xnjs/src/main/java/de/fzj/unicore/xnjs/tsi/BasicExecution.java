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


package de.fzj.unicore.xnjs.tsi;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.logging.log4j.Logger;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;

import de.fzj.unicore.xnjs.XNJSConstants;
import de.fzj.unicore.xnjs.XNJSProperties;
import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.ActionResult;
import de.fzj.unicore.xnjs.ems.ActionStatus;
import de.fzj.unicore.xnjs.ems.BudgetInfo;
import de.fzj.unicore.xnjs.ems.ExecutionContext;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.ems.InternalManager;
import de.fzj.unicore.xnjs.idb.ApplicationInfo;
import de.fzj.unicore.xnjs.idb.IDB;
import de.fzj.unicore.xnjs.idb.Incarnation;
import de.fzj.unicore.xnjs.io.IOProperties;
import de.fzj.unicore.xnjs.io.XnjsFile;
import de.fzj.unicore.xnjs.tsi.local.LocalExecution;
import de.fzj.unicore.xnjs.tsi.local.LocalTSIProperties;
import de.fzj.unicore.xnjs.tsi.remote.TSIMessages;
import de.fzj.unicore.xnjs.util.IOUtils;
import de.fzj.unicore.xnjs.util.LogUtil;
import eu.unicore.security.Client;

/**
 * Simple execution system<br>
 * 
 * @author schuller
 */
@Singleton
public class BasicExecution implements IExecution, IExecutionSystemInformation {

	protected static final Logger jobExecLogger = LogUtil.getLogger(LogUtil.JOBS, BasicExecution.class);

	@Inject
	protected XNJSProperties properties;

	@Inject
	protected IOProperties ioProperties;

	@Inject
	private LocalTSIProperties tsiProperties;
	
	@Inject
	protected IDB idb;
	
	@Inject
	protected Incarnation grounder;
	
	@Inject
	protected TSIFactory tsiFactory;
	
	@Inject
	protected TSIMessages tsiMessages;
	
	@Inject
	protected InternalManager manager;
	
	@Inject
	protected MetricRegistry metricRegistry;
	
	/**
	 * A custom grace period, defined in milliseconds. 
	 * Using this key, the usual grace period can be overridden 
	 * (and stored in the action's processing context)
	 */
	public static final String CUSTOM_GRACE_PERIOD="CLASSICTSI.statusupdate.grace.custom";

	protected static final String GRACE_PERIOD_start="CLASSICTSI.statusupdate.grace.start";
	protected static final String EXITCODE_RECHECK="CLASSICTSI.statusupdate.exitcode.recheck";

	// for limiting the number of concurrently running jobs (embedded TSI!)
	private final Set<String>runningJobUIDs=Collections.synchronizedSet(new HashSet<String>());
	private final AtomicInteger runningJobCount = new AtomicInteger(0);
	private final AtomicInteger jobIndex = new AtomicInteger(0);

	/**
	 * job submission, override in subclasses if using a BSS
	 */
	public int submit(Action job) throws TSIBusyException,ExecutionException {
		int limit = tsiProperties.getJobLimit();
		if(limit > 0){
			int current = runningJobCount.get();
			if(current >= limit)throw new TSIBusyException("Joblimit reached: there are <"+current+"> running jobs");
		}
		ApplicationInfo appDescription=job.getApplicationInfo();
		ExecutionContext ec=job.getExecutionContext();
		LocalExecution ex=new LocalExecution(job.getUUID(), tsiProperties, manager, buildCommand(job, idb), ec);
		ex.execute();
		job.addLogTrace("Submitted executable: "+appDescription.getExecutable());
		runningJobCount.incrementAndGet();
		runningJobUIDs.add(job.getUUID());
		job.setBSID("INTERNAL-TSI-"+jobIndex.incrementAndGet());
		return ActionStatus.QUEUED;
	}


	/**
	 * builds the command to be executed
	 * 
	 * @param appDescription
	 * @param job
	 * @param grounder
	 * @return
	 * @throws ExecutionException
	 */
	private String buildCommand(Action job, IDB idb)throws ExecutionException{
		String result;
		boolean useShell = tsiProperties.isUseShell();
		if(useShell &&  System.getProperty("os.name").toLowerCase().indexOf("windows") == -1){
			result = buildShellWrappedCommand(job, idb);
			job.getProcessingContext().put("localts.mode.shell",Boolean.TRUE);
			job.setDirty();
		}
		else{
			result = buildDirectCommand(job, idb);
		}
		return result;
	}

	private String buildShellWrappedCommand(Action jsdlAction, IDB idb)throws ExecutionException{
		String workDir=jsdlAction.getExecutionContext().getWorkingDirectory();
		//handle case that workDir is not absolute
		File wd=new File(workDir);
		if(!wd.isAbsolute()){
			workDir=wd.getAbsolutePath();
			jsdlAction.getExecutionContext().setWorkingDirectory(workDir);
		}
		String tmpName="TSI_submit_"+System.currentTimeMillis();
		String cmdFile=workDir+tmpName;
		String cmd = tsiMessages.makeSubmitCommand(jsdlAction, null);
		//write to file
		OutputStreamWriter writer=null;
		try{
			TSI tsi = tsiFactory.createTSI(jsdlAction.getClient());
			writer=new OutputStreamWriter(tsi.getOutputStream(cmdFile));
			writer.write(cmd);
		}catch(IOException ioe){
			throw new ExecutionException(ioe);
		}
		finally{
			IOUtils.closeQuietly(writer);
		}
		return tsiProperties.getShell()+" "+cmdFile;
	}


	protected String buildDirectCommand(Action job, IDB idb){
		ApplicationInfo appDescription=job.getApplicationInfo();
		StringBuffer res=new StringBuffer();
		res.append(appDescription.getExecutable());
		//append arguments
		for(String at: appDescription.getArguments()){
			res.append(" "+at);
		}
		return res.toString();
	}

	/* (non-Javadoc)
	 * @see de.fzj.unicore.xnjs.ems.ExecutionInterface#abort(de.fzj.unicore.xnjs.ems.Action)
	 */
	public void abort(Action job) throws ExecutionException {
		try{
			if(job.getStatus()==ActionStatus.RUNNING){
				//check whether the job is still running
				//in the LocalExecution
				if(LocalExecution.isRunning(job.getUUID())){
					LocalExecution.abort(job.getUUID());
					job.addLogTrace("User aborted.");
					job.setStatus(ActionStatus.DONE);
					job.addLogTrace("Status set to DONE.");
					job.setResult(new ActionResult(ActionResult.USER_ABORTED));
				}
			}
		}catch(Exception ex){//just wrap it
			throw new ExecutionException(ex);
		}
	}

	/* (non-Javadoc)
	 * @see de.fzj.unicore.xnjs.ems.ExecutionInterface#pause(de.fzj.unicore.xnjs.ems.Action)
	 */
	public void pause(Action job) throws ExecutionException {
		throw new IllegalStateException("Operation is not supported.");
	}

	/* (non-Javadoc)
	 * @see de.fzj.unicore.xnjs.ems.ExecutionInterface#resume(de.fzj.unicore.xnjs.ems.Action)
	 */
	public void resume(Action job) throws ExecutionException {
		throw new IllegalStateException("Operation is not supported.");
	}

	/* (non-Javadoc)
	 * @see de.fzj.unicore.xnjs.ems.ExecutionInterface#checkpoint(de.fzj.unicore.xnjs.ems.Action)
	 */
	public void checkpoint(Action job) throws ExecutionException {
		throw new IllegalStateException("Operation is not supported.");
	}

	/* (non-Javadoc)
	 * @see de.fzj.unicore.xnjs.ems.ExecutionInterface#restart(de.fzj.unicore.xnjs.ems.Action)
	 */
	public void restart(Action job) throws ExecutionException {
		throw new IllegalStateException("Operation is not supported.");
	}


	public void updateStatus(Action job) throws ExecutionException {
		try{
			//skip QUEUED state for embedded TSI
			if(job.getStatus()==ActionStatus.QUEUED){
				job.setStatus(ActionStatus.RUNNING);
			}
			else if(job.getStatus()==ActionStatus.RUNNING){
				//get progress
				updateProgress(job);
				//check for the exit code
				boolean shellMode=job.getProcessingContext().get("localts.mode.shell")!=null;
				if(shellMode){
					updateExitCodeShellMode(job);
				}
				else{
					updateExitCodeLocal(job);
				}
			}
		}catch(Exception ex){
			throw new ExecutionException(ex);
		}
	}

	private void updateExitCodeLocal(Action job){
		String uid=job.getUUID();
		if(!LocalExecution.isRunning(uid)){
			Integer exit=LocalExecution.getExitCode(uid);
			if(exit==null){
				//failure during execution
				job.fail();
			}
			else{
				job.getExecutionContext().setExitCode(exit);
				job.setStatus(ActionStatus.POSTPROCESSING);
				jobExecLogger.debug("["+uid+"] Status set to POSTPROCESSING.");
				job.addLogTrace("Status set to POSTPROCESSING.");
				decrementJobCounter(uid);
			}
		}
	}

	private void updateExitCodeShellMode(Action job)throws Exception{
		String uid=job.getUUID();
		if(LocalExecution.isRunning(uid))return;

		decrementJobCounter(job.getUUID());
		
		boolean haveExitCode=getExitCode(job);
		if(!haveExitCode){
			int myGracePeriod=0;

			Long timeOfFirstStatusCheck=(Long)job.getProcessingContext().get(GRACE_PERIOD_start);
			if(timeOfFirstStatusCheck==null){
				timeOfFirstStatusCheck=Long.valueOf(System.currentTimeMillis());
				job.getProcessingContext().put(GRACE_PERIOD_start, timeOfFirstStatusCheck);
				job.setDirty();
			}

			//check for a custom grace period
			Integer g=(Integer)job.getProcessingContext().get(CUSTOM_GRACE_PERIOD);
			if(g!=null)myGracePeriod=g.intValue();

			if(System.currentTimeMillis()<timeOfFirstStatusCheck+myGracePeriod){
				jobExecLogger.debug("No exit code found for {}, assuming it is still running.", uid);
			}
			else {
				jobExecLogger.debug("No BSS status found for {}, assuming it is completed.", uid);
				job.setStatus(ActionStatus.POSTPROCESSING);
			}
		}
		else{
			jobExecLogger.debug("Have exit code for {}, assuming it is completed.", uid);
			job.setStatus(ActionStatus.POSTPROCESSING);
		}
	}

	private void decrementJobCounter(String uid){
		if(runningJobUIDs.remove(uid)){
			runningJobCount.decrementAndGet();
		}
	}

	/**
	 * get the exit code by reading the exit code file
	 * @return true if exit code was found
	 * @param job
	 */
	protected boolean getExitCode(Action job)throws Exception{
		if(job.getExecutionContext().getExitCode()!=null)return true;
		TSI tsi = tsiFactory.createTSI(job.getClient());
		ExecutionContext ctx=job.getExecutionContext();
		tsi.setStorageRoot(ctx.getOutputDirectory());
		XnjsFile f=tsi.getProperties(ctx.getExitCodeFileName());
		if(f==null)return false;
		try(BufferedReader br = new BufferedReader(new InputStreamReader(
				tsi.getInputStream(ctx.getExitCodeFileName())))){
			try{
				String s=br.readLine();
				if(s!=null){
					int i=Integer.parseInt(s);
					ctx.setExitCode(i);
					job.addLogTrace("Exit code "+i);
					jobExecLogger.debug("Script exited with code <"+i+">");
					return true;
				}
			}catch(Exception e){
				jobExecLogger.debug("Could not retrieve exit code.",e);
			}
			return false;
		}
	}

	/**
	 * update the estimated end time for the given job
	 */
	protected void updateEstimatedEndtime(Action job){
		try{
			int runTime = TSIMessages.getRuntime(job.getExecutionContext().getResourceRequest());
			if(runTime>0){
				job.getExecutionContext().setEstimatedEndtime(System.currentTimeMillis()+1000*runTime);
			}
		}catch(Exception e){}
	}

	/**
	 * update the progress indication for the given job
	 * @param job - the action to update the progress value for
	 */
	protected void updateProgress(Action job){
		BufferedReader br=null;
		try{
			//limit the number of unsuccessful attempts to get the progress
			Integer j=(Integer)job.getProcessingContext().get(PROGRESS_NOT_FOUND_KEY);
			if(j!=null && j>3)return;
			TSI tsi = tsiFactory.createTSI(job.getClient());
			InputStream is=tsi.getInputStream(job.getExecutionContext().getWorkingDirectory()+"/"+PROGRESS_FILENAME);
			br=new BufferedReader(new InputStreamReader(is));
			String s=br.readLine();
			if(s!=null){
				Float f=Float.parseFloat(s);
				jobExecLogger.info("Found progress value <"+f+"> for job "+job.getUUID());
				job.getExecutionContext().setProgress(f);
				job.setDirty();
			}
			else {
				if(j==null){
					j=Integer.valueOf(1);
				}
				j++;
				job.getProcessingContext().put(PROGRESS_NOT_FOUND_KEY,j);
				job.setDirty();
			}

		}catch(NumberFormatException nfe){
			//progress file exists but weird value
			jobExecLogger.warn("Application wrote faulty progress file for action "+job.getUUID());
		}catch(ExecutionException ee){
			//no progress file written
		}catch(IOException e){
			//no progress file written	
		}
		finally{
			IOUtils.closeQuietly(br);
		}
	}

	public int getNumberOfQueuedJobs() {
		return getTotalNumberOfJobs()-getNumberOfRunningJobs();
	}

	public int getNumberOfRunningJobs() {
		return LocalExecution.getNumberOfRunningJobs();
	}

	public int getTotalNumberOfJobs() {
		return LocalExecution.getTotalNumberOfJobs();
	}

	public Map<String,Integer>getQueueFill(){
		return null;
	}

	public long getMeanTimeQueued(){
		try{
			Histogram stats = metricRegistry.getHistograms().get(XNJSConstants.MEAN_TIME_QUEUED);
			return (long)stats.getSnapshot().getMean();
		}catch(Exception ex){}
		return -1l;
	}

	@Override
	public String getBSSJobDetails(Action job) throws ExecutionException {
		return "N/A";
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public List<BudgetInfo> getComputeTimeBudget(Client client) throws ExecutionException {
		return Collections.EMPTY_LIST;
	}
}
