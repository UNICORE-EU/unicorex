package eu.unicore.xnjs.tsi;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.Logger;

import com.codahale.metrics.Histogram;

import eu.unicore.security.Client;
import eu.unicore.xnjs.XNJSProperties;
import eu.unicore.xnjs.ems.Action;
import eu.unicore.xnjs.ems.ActionResult;
import eu.unicore.xnjs.ems.ActionStatus;
import eu.unicore.xnjs.ems.BudgetInfo;
import eu.unicore.xnjs.ems.ExecutionContext;
import eu.unicore.xnjs.ems.ExecutionException;
import eu.unicore.xnjs.ems.InternalManager;
import eu.unicore.xnjs.idb.ApplicationInfo;
import eu.unicore.xnjs.idb.IDB;
import eu.unicore.xnjs.idb.Incarnation;
import eu.unicore.xnjs.io.IOProperties;
import eu.unicore.xnjs.io.XnjsFile;
import eu.unicore.xnjs.tsi.local.LocalExecution;
import eu.unicore.xnjs.tsi.local.LocalTSIProperties;
import eu.unicore.xnjs.tsi.remote.TSIMessages;
import eu.unicore.xnjs.util.LogUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
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
	protected Histogram mtq;

	/**
	 * A custom grace period, defined in milliseconds. 
	 * Using this key, the usual grace period can be overridden 
	 * (and stored in the action's processing context)
	 */
	public static final String CUSTOM_GRACE_PERIOD="CLASSICTSI.statusupdate.grace.custom";

	protected static final String GRACE_PERIOD_start="CLASSICTSI.statusupdate.grace.start";
	protected static final String EXITCODE_RECHECK="CLASSICTSI.statusupdate.exitcode.recheck";

	// for limiting the number of concurrently running jobs (embedded TSI!)
	private final Set<String>runningJobUIDs = Collections.synchronizedSet(new HashSet<>());
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

	private String buildShellWrappedCommand(Action action, IDB idb)throws ExecutionException{
		String workDir = action.getExecutionContext().getWorkingDirectory();
		//handle case that workDir is not absolute
		File wd=new File(workDir);
		if(!wd.isAbsolute()){
			workDir=wd.getAbsolutePath();
			action.getExecutionContext().setWorkingDirectory(workDir);
		}
		String tmpName="TSI_submit_"+System.currentTimeMillis();
		String cmdFile=workDir+tmpName;
		String cmd = tsiMessages.makeSubmitCommand(action, null);
		//write to file
		TSI tsi = tsiFactory.createTSI(action.getClient(), null);
		try(OutputStreamWriter writer = new OutputStreamWriter(tsi.getOutputStream(cmdFile))){
			writer.write(cmd);
		}catch(IOException ioe){
			throw new ExecutionException(ioe);
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

	@Override
	public void abort(Action job) throws ExecutionException {
		try{
			if(job.getStatus()==ActionStatus.RUNNING){
				//check whether the job is still running
				//in the LocalExecution
				if(LocalExecution.isRunning(job.getUUID())){
					LocalExecution.abort(job.getUUID());
					decrementJobCounter(job.getUUID());
					job.addLogTrace("User aborted.");
					job.setStatus(ActionStatus.DONE);
					job.addLogTrace("Status set to DONE.");
					job.setResult(new ActionResult(ActionResult.USER_ABORTED));
				}
			}
		}catch(Exception ex){
			throw new ExecutionException(ex);
		}
	}

	@Override
	public void pause(Action job) throws ExecutionException {
		throw new IllegalStateException("Operation is not supported.");
	}

	@Override
	public void resume(Action job) throws ExecutionException {
		throw new IllegalStateException("Operation is not supported.");
	}

	@Override
	public void checkpoint(Action job) throws ExecutionException {
		throw new IllegalStateException("Operation is not supported.");
	}

	@Override
	public void restart(Action job) throws ExecutionException {
		throw new IllegalStateException("Operation is not supported.");
	}

	@Override
	public void updateStatus(Action job) throws ExecutionException {
		// embedded TSI does not really have QUEUED state
		if(job.getStatus()==ActionStatus.QUEUED){
			job.setStatus(ActionStatus.RUNNING);
		}
		else if(job.getStatus()==ActionStatus.RUNNING){
			updateProgress(job);
			boolean shellMode=job.getProcessingContext().get("localts.mode.shell")!=null;
			if(shellMode){
				updateExitCodeShellMode(job);
			}
			else{
				updateExitCodeLocal(job);
			}
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
				jobExecLogger.debug("[{}] Status set to POSTPROCESSING.", uid);
				job.addLogTrace("Status set to POSTPROCESSING.");
				decrementJobCounter(uid);
			}
		}
	}

	private void updateExitCodeShellMode(Action job) throws ExecutionException {
		String uid=job.getUUID();
		if(LocalExecution.isRunning(uid))return;

		decrementJobCounter(job.getUUID());
		
		boolean haveExitCode=readExitCode(job);
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
	 * attempt to read the exit code from the exit code file
	 *
	 * @return true if exit code file exists and exit code could be read
	 * @param job
	 */
	protected boolean readExitCode(Action job)throws ExecutionException {
		if(job.getExecutionContext().getExitCode()!=null)return true;
		TSI tsi = tsiFactory.createTSI(job.getClient(),
				job.getExecutionContext().getPreferredExecutionHost());
		ExecutionContext ctx=job.getExecutionContext();
		tsi.setStorageRoot(ctx.getOutputDirectory());
		XnjsFile f=tsi.getProperties(ctx.getExitCodeFileName());
		if(f==null)return false;
		try(BufferedReader br = new BufferedReader(new InputStreamReader(
				tsi.getInputStream(ctx.getExitCodeFileName())))){
			String s=br.readLine();
			if(s!=null){
				int i=Integer.parseInt(s);
				ctx.setExitCode(i);
				job.addLogTrace("Exit code "+i);
				jobExecLogger.debug("Script exited with code <{}>",i);
				return true;
			}
			return false;
		}catch(Exception ioe) {
			throw ExecutionException.wrapped(ioe);
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
		try{
			//limit the number of unsuccessful attempts to get the progress
			Integer j=(Integer)job.getProcessingContext().get(PROGRESS_NOT_FOUND_KEY);
			if(j!=null && j>3)return;
			TSI tsi = tsiFactory.createTSI(job.getClient(), null);
			String progressFile = job.getExecutionContext().getWorkingDirectory()+"/"+PROGRESS_FILENAME;
			String s = null;
			try(BufferedReader br=new BufferedReader(new InputStreamReader(tsi.getInputStream(progressFile)))) {
				s=br.readLine();
			}
			if(s!=null){
				Float f=Float.parseFloat(s);
				jobExecLogger.info("Found progress value <{}> for job <{}>", f, job.getUUID());
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
			jobExecLogger.warn("Application wrote faulty progress file for action <{}>", job.getUUID());
		}catch(IOException e){
			//no progress file written	
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
			return (long)mtq.getSnapshot().getMean();
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
	
	public boolean isBeingTracked(Action job) throws ExecutionException{
		return job!=null && runningJobUIDs.contains(job.getUUID());
	}

	@Override
	public void toggleStatusUpdates(boolean set) {
		// NOP
	}
}
