package eu.unicore.xnjs.ems.processors;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;

import com.codahale.metrics.Histogram;

import eu.unicore.security.Client;
import eu.unicore.util.Log;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.XNJSConstants;
import eu.unicore.xnjs.ems.Action;
import eu.unicore.xnjs.ems.ActionResult;
import eu.unicore.xnjs.ems.ActionStatus;
import eu.unicore.xnjs.ems.ApplicationExecutionStatus;
import eu.unicore.xnjs.ems.ExecutionContext;
import eu.unicore.xnjs.ems.ExecutionException;
import eu.unicore.xnjs.ems.IExecutionContextManager;
import eu.unicore.xnjs.ems.Manager;
import eu.unicore.xnjs.ems.ProcessingContext;
import eu.unicore.xnjs.ems.event.ContinueProcessingEvent;
import eu.unicore.xnjs.ems.event.XnjsEvent;
import eu.unicore.xnjs.ems.processors.AsyncCommandProcessor.SubCommand;
import eu.unicore.xnjs.idb.ApplicationInfo;
import eu.unicore.xnjs.idb.IDB;
import eu.unicore.xnjs.io.DataStageInInfo;
import eu.unicore.xnjs.io.DataStageOutInfo;
import eu.unicore.xnjs.io.DataStagingInfo;
import eu.unicore.xnjs.io.IOProperties;
import eu.unicore.xnjs.io.StagingInfo;
import eu.unicore.xnjs.tsi.IExecution;
import eu.unicore.xnjs.tsi.TSI;
import eu.unicore.xnjs.tsi.TSIBusyException;
import eu.unicore.xnjs.tsi.remote.Execution;
import eu.unicore.xnjs.util.ErrorCode;
import eu.unicore.xnjs.util.LogUtil;

/**
 * Base processor for UNICORE jobs<br/>
 * Handles just about everything, except parsing the job description.
 *
 * @param T - the type containing the job description
 * 
 * @author schuller
 */
public abstract class JobProcessor<T> extends DefaultProcessor {

	private static final Logger logger=LogUtil.getLogger(LogUtil.JOBS,JobProcessor.class);

	protected static final String subactionkey_in="JSDL_de.fzj.unicore.xnjs.jsdl.JSDLProcessor_SUBACTION_STAGEIN";
	public static final String KEY_DELETEONTERMINATION="JSDL_DELETEFILES";
	protected static final String subactionkey_out="JSDL_de.fzj.unicore.xnjs.jsdl.JSDLProcessor_SUBACTION_STAGEOUT";
	protected static final String subactionkey_pre="JSDL_de.fzj.unicore.xnjs.jsdl.JSDLProcessor_SUBACTION_PRECOMMAND";
	protected static final String subactionkey_post="JSDL_de.fzj.unicore.xnjs.jsdl.JSDLProcessor_SUBACTION_POSTCOMMAND";

	//time stamp names
	private static final String TIME_START = "_TIME_START";
	private static final String TIME_END_STAGEIN ="_TIME_END_STAGEIN";
	private static final String TIME_START_PRE= "_TIME_START_PRE";
	private static final String TIME_SUBMITTED = "_TIME_SUBMITTED";
	private static final String TIME_START_MAIN = "_TIME_START_MAIN";
	private static final String TIME_END_MAIN = "_TIME_END_MAIN";
	private static final String TIME_START_POST= "_TIME_START_POST";
	private static final String TIME_START_STAGEOUT="_TIME_START_STAGEOUT";
	private static final String TIME_END = "_TIME_END";

	// for re-trying to create the job directory
	private static final String USPACE_CREATE_ATTEMPTS = "_USPACE_CREATE_ATTEMPTS";

	/**
	 * the execution interface
	 */
	protected final IExecution exec;

	/**
	 * the execution context manager
	 */
	protected final IExecutionContextManager ecm;

	protected final Manager ems;

	public JobProcessor(XNJS xnjs){
		super(xnjs);
		exec = xnjs.get(IExecution.class);
		ecm = xnjs.get(IExecutionContextManager.class);
		ems = xnjs.get(Manager.class);
	}

	/**
	 * get the job description document
	 */
	@SuppressWarnings("unchecked")
	protected T getJobDescriptionDocument(){
		return (T)action.getAjd();
	}

	/**
	 * extract any notification URLs from the job description and store them in the action
	 */
	protected abstract void setupNotifications();

	/**
	 * returns <code>true</code> if the job description is empty, i.e. 
	 * there is nothing to do for this job 
	 */
	protected abstract boolean isEmptyJob();

	/**
	 * extract the job name from the job description
	 */
	protected abstract String getJobName();

	/**
	 * extract the job umask from the job description
	 */
	protected abstract String getUmask();

	/**
	 * returns <code>true</code> if the job contains data stage-in
	 */
	protected abstract boolean hasStageIn();

	/**
	 * returns <code>true</code> if the job contains data stage-out
	 */
	protected abstract boolean hasStageOut();

	/**
	 * handle state "CREATED"
	 */
	protected void handleCreated()throws ExecutionException{
		try{
			if(getTimeStamp(TIME_START)==null) {
				storeTimeStamp(TIME_START);
				setupNotifications();
			}
			boolean ok = createJobDirectory();
			if(!ok) {
				// wait a bit and re-try
				sleep(5, TimeUnit.SECONDS);
				return;
			}
			action.addLogTrace("Created job directory <"+
					action.getExecutionContext().getWorkingDirectory()+">");
			if(isEmptyJob()){
				action.addLogTrace("Empty job description, nothing to do.");
				setToDoneSuccessfully();
				return;
			}
			extractFromJobDescription();
			setEnvironmentVariables();
			if(hasStageIn()){
				action.setStatus(ActionStatus.PREPROCESSING);
				action.addLogTrace("Status set to PREPROCESSING (staging in).");
				addStageIn();
				action.setWaiting(true);
			}
			else{
				setToReady();
			}
		}catch(Exception ex){
			String msg="ERROR: "+ex.getMessage();
			action.addLogTrace(msg);
			throw ExecutionException.wrapped(ex);
		}
	}

	/*
	 * Create the job working directory. Handles repeated attempts to
	 * do this in case of errors
	 *
	 * @return <code>true</code> if action should continue,
	 *     <code>false</code> if action should re-try after a pause
	 * @throws ExecutionException if giving up after too many attempts
	 */
	protected boolean createJobDirectory() throws ExecutionException {
		try{
			ecm.createUSpace(action);
			return true;
		}catch(ExecutionException e) {
			// track number of attempts
			Integer attempts =(Integer)action.getProcessingContext().get(USPACE_CREATE_ATTEMPTS);
			if(attempts==null)attempts = Integer.valueOf(0);
			attempts++;
			action.getProcessingContext().put(USPACE_CREATE_ATTEMPTS, attempts);
			String msg = Log.createFaultMessage("Problem creating working directory", e);
			if(attempts<3) {
				action.addLogTrace(msg+". Re-trying.");
			}
			else {
				action.addLogTrace(msg+". Giving up.");
				rewriteJobDescription(null);
				throw ExecutionException.wrapped(e);
			}
		}
		return false;
	}

	/**
	 * handle state "PreProcessing" aka "Staging in"
	 */
	protected void handlePreProcessing() {
		try{
			String stageInActionID=(String)action.getProcessingContext().get(subactionkey_in);
			if(stageInActionID!=null){
				handleStagingIn(stageInActionID);
			}
		}catch(Exception ex){
			LogUtil.logException("Error processing stage-in.", ex, logger);
			String reason=LogUtil.createFaultMessage("Error processing stage-in.", ex);
			setToDoneAndFailed(reason);
		}
	}

	@SuppressWarnings("unchecked")
	protected void handleStagingIn(String id)throws Exception{
		ActionResult res=checkSubAction(id, "Stage in", false);
		if(res!=null){
			action.setStageIns(null); // save some space in the DB
			if(!res.isSuccessful()){
				//TODO: policy on this or not?
				setToDoneAndFailed("Staging in failed. Reason: "+res.getErrorMessage());
			}else {
				Action subAction=manager.getAction(id);
				List<String> files=(List<String>)subAction.getProcessingContext().get(KEY_DELETEONTERMINATION);
				if(files!=null){
					action.getProcessingContext().put(KEY_DELETEONTERMINATION,files);
				}
				ems.destroy(id, action.getClient());
				setToReady();
			}
		}
	}

	/**
	 * set state to "ready"
	 */
	protected void setToReady(){
		storeTimeStamp(TIME_END_STAGEIN);	
		action.setStatus(ActionStatus.READY);
		action.addLogTrace("Status set to READY.");
	}

	/**
	 * handle "READY" state
	 * There are two cases: 
	 * - the job needs to be explicitly started by the client
	 * - job starts automatically
	 */
	protected void handleReady() throws ExecutionException {
		logger.trace("Handling READY state for Action {}",action.getUUID());

		//handle scheduled processing
		extractNotBefore();

		if(xnjs.getXNJSProperties().isAutoSubmitWhenReady()){
			goToPending();
		}
		else{
			if(action.getProcessingContext().get(Action.AUTO_SUBMIT)==null){
				//action will sleep until client starts it
				action.setWaiting(true);
				return;
			}
			else {
				goToPending();
			}
		}
	}

	/**
	 * set state to "pending"
	 */
	protected void goToPending(){
		action.setStatus(ActionStatus.PENDING);
		action.addLogTrace("Status set to PENDING.");
		return;
	}

	/**
	 * handle "PENDING" state
	 */
	protected void handlePending() throws Exception{
		logger.trace("Handling PENDING state for Action {}",action.getUUID());

		ApplicationExecutionStatus aes=action.getProcessingContext().get(ApplicationExecutionStatus.class);
		if(aes==null){
			aes=new ApplicationExecutionStatus();
			action.getProcessingContext().set(aes);
			action.setDirty();
		}

		switch(aes.get()){
		case ApplicationExecutionStatus.CREATED: 
			setupPreCommand();
			break;

		case ApplicationExecutionStatus.PRECOMMAND_EXECUTION:
			handlePreCommandRunning();
			break;

		case ApplicationExecutionStatus.PRECOMMAND_DONE:
			submitMainExecutable();
			break;

		default:
			throw new IllegalStateException("Illegal precommand state <+"+aes.get()+"> in PENDING.");
		}
	}

	@SuppressWarnings("unchecked")
	private List<String>getOrCreateList(String key){
		List<String>ids=(List<String>)action.getProcessingContext().get(key);
		if(ids==null){
			ids = new ArrayList<>();
			action.getProcessingContext().put(key, ids);
		}
		return ids;
	}

	/**
	 * initialise pre-command execution
	 */
	protected void setupPreCommand() throws ExecutionException {
		try{
			boolean done = true;
			int index=0;
			StringBuilder pre = new StringBuilder();
			if(action.getApplicationInfo().getPreCommand()!=null) {
				pre.append(action.getApplicationInfo().getPreCommand());
			}

			// user-defined pre-command
			String userPre = action.getApplicationInfo().getUserPreCommand();
			if(userPre != null && action.getApplicationInfo().isUserPreCommandOnLoginNode()){
				if(pre.length()>0)pre.append("\n");
				pre.append(userPre);
			}

			if(pre.length()>0) {
				done = false;
				SubCommand cmd = new SubCommand();
				cmd.id = "PRE_"+(index++);
				cmd.cmd = pre.toString();
				cmd.workingDir = action.getExecutionContext().getWorkingDirectory();
				cmd.ignoreExitCode = action.getApplicationInfo().isUserPreCommandIgnoreExitCode();
				cmd.env.putAll(action.getExecutionContext().getEnvironment());
				cmd.preferredExecutionHost = action.getApplicationInfo().getPreferredLoginNode();
				String subID = createPrePostAction(cmd);
				action.addLogTrace("Launched pre command <"+cmd.cmd+">");
				getOrCreateList(subactionkey_pre).add(subID);
			}

			if(done) 
			{
				action.getProcessingContext().set(ApplicationExecutionStatus.precommandDone());
			}
			else
			{
				storeTimeStamp(TIME_START_PRE);
				action.getProcessingContext().set(ApplicationExecutionStatus.precommandRunning());
			}
			action.setDirty();

		}catch(Exception ex){
			String msg="Could not setup pre-command: "+ex.getMessage();
			action.addLogTrace(msg);
			setToDoneAndFailed(msg);
			throw ExecutionException.wrapped(ex);
		}	
	}


	@SuppressWarnings("unchecked")
	protected void handlePreCommandRunning()throws Exception{
		try{
			List<String>ids=action.getProcessingContext().getAs(subactionkey_pre,List.class);
			Iterator<String>iter=ids.iterator();
			StringBuilder errors=new StringBuilder();
			while(iter.hasNext()){
				String id=iter.next();	
				ActionResult res=checkSubAction(id, "Pre-commands", true);
				if(res!=null){
					if(!res.isSuccessful()){
						errors.append("[").append(res.getErrorMessage()).append("]");
					}
					iter.remove();
				}
			}
			if(ids.size()==0){
				action.getProcessingContext().set(ApplicationExecutionStatus.precommandDone());
			}
			if(errors.length()>0){
				setToDoneAndFailed("Pre-command(s) failed: "+errors.toString());
				return;
			}
			if(ids.size()>0){
				action.setWaiting(true);
			}
			action.setDirty();
		}
		catch(ExecutionException ee){
			throw ExecutionException.wrapped(ee);
		}
	}

	protected boolean checkMainExecutionSuccess() throws ExecutionException {
		if(action.getApplicationInfo().ignoreNonZeroExitCode())return true;
		Integer exitCode = action.getExecutionContext().getExitCode();
		if(exitCode!=null && exitCode!=0){
			String msg = "User application exited with non-zero exit code: <" + exitCode + ">."
						+ " More information might be available in the job's working directory '"
						+ action.getExecutionContext().getWorkingDirectory() + "'";
			setToDoneAndFailed(msg);
			return false;
		}
		return true;
	}

	/**
	 * initialise post-command execution
	 */
	protected void setupPostCommand() throws ExecutionException{
		try{
			boolean done = true;
			int index=0;
			
			StringBuilder post = new StringBuilder();
			if(action.getApplicationInfo().getPostCommand()!=null) {
				post.append(action.getApplicationInfo().getPostCommand());
			}
			
			String userPost = action.getApplicationInfo().getUserPostCommand();
			if(userPost != null && action.getApplicationInfo().isUserPostCommandOnLoginNode()){
				if(post.length()>0)post.append("\n");
				post.append(userPost);
			}
			if(post.length()>0) {
				done = false;
				SubCommand cmd = new SubCommand();
				cmd.id = "POST_"+(index++);
				cmd.cmd = post.toString();
				cmd.workingDir = action.getExecutionContext().getWorkingDirectory();
				cmd.env.putAll(action.getExecutionContext().getEnvironment());
				cmd.preferredExecutionHost = action.getApplicationInfo().getPreferredLoginNode();
				String subID = createPrePostAction(cmd);
				action.addLogTrace("Launched post command <"+cmd.cmd+">");
				getOrCreateList(subactionkey_post).add(subID);
			}

			if(done) 
			{
				action.getProcessingContext().set(ApplicationExecutionStatus.done());
			}
			else
			{
				storeTimeStamp(TIME_START_POST);
				action.getProcessingContext().set(ApplicationExecutionStatus.postcommandRunning());
			}
			action.setDirty();
		}catch(Exception ex){
			String msg="Could not setup post-command: "+ex.getMessage();
			action.addLogTrace(msg);
			setToDoneAndFailed(msg);
			throw ExecutionException.wrapped(ex);
		}	
	}

	@SuppressWarnings("unchecked")
	protected void handlePostCommandRunning()throws Exception{
		List<String>ids=action.getProcessingContext().getAs(subactionkey_post,List.class);
		Iterator<String>iter=ids.iterator();
		StringBuilder errors=new StringBuilder();
		while(iter.hasNext()){
			String id=iter.next();	
			ActionResult res=checkSubAction(id, "Post-commands", true);
			if(res!=null){
				if(!res.isSuccessful()){
					errors.append("[").append(res.getErrorMessage()).append("]");
				}
				iter.remove();
			}
		}
		if(ids.size()==0){
			action.getProcessingContext().set(ApplicationExecutionStatus.done());
		}
		if(errors.length()>0){
			setToDoneAndFailed("Post-command(s) failed: "+errors.toString());
			return;
		}
		if(ids.size()>0){
			action.setWaiting(true);
		}
	}

	/**
	 * submits the given command array as a sub action
	 */
	protected String createPrePostAction(SubCommand cmd)throws Exception{
		return manager.addSubAction(cmd, XNJSConstants.asyncCommandType, action, true);
	}

	/**
	 * submit the main application
	 */
	protected void submitMainExecutable() throws ExecutionException {
		try{
			ApplicationInfo appInfo=action.getApplicationInfo();
			if(appInfo!=null && appInfo.isAllocateOnly()) {
				action.addLogTrace("Job type: 'allocate'.");	
			}
			else if(appInfo==null || appInfo.getExecutable()==null){
				action.addLogTrace("No application to execute, changing action status to POSTPROCESSING");
				action.setStatus(ActionStatus.POSTPROCESSING);
				return;
			}
			action.setWaiting(true);
			int initialState = exec.submit(action);
			ApplicationExecutionStatus aes=action.getProcessingContext().get(ApplicationExecutionStatus.class);
			aes.set(ApplicationExecutionStatus.MAIN_EXECUTION);
			action.setStatus(initialState);
			storeTimeStamp(TIME_SUBMITTED);
		} catch(ExecutionException ex){
			String msg="Could not submit job: "+ex.getMessage();

			Integer submitCount=(Integer)action.getProcessingContext().get(Execution.BSS_SUBMIT_COUNT);
			if(submitCount==null)submitCount = Integer.valueOf(0);
			submitCount++;
			action.getProcessingContext().put(Execution.BSS_SUBMIT_COUNT, submitCount);
			int maxSubmitCount = xnjs.getXNJSProperties().getResubmitCount();

			if(!isRecoverable(ex) || submitCount.intValue()>maxSubmitCount){
				action.addLogTrace(msg);
				setToDoneAndFailed(msg);
				throw new ExecutionException(msg,ex);
			}

			action.addLogTrace("Submit attempt "+submitCount+" (of "+maxSubmitCount+") failed: "+ex.getMessage());

			pauseExecution(xnjs.getXNJSProperties().getResubmitDelay(), TimeUnit.SECONDS);
		}	
		catch(TSIBusyException tbe){
			//no problem, will retry
			Serializable logged = action.getProcessingContext().get("__delayed__submit__");
			if(logged==null) {
				action.addLogTrace("Submit delayed due to load limit.");
				action.getProcessingContext().put("__delayed__submit__", "true");
			}
			sleep(30, TimeUnit.SECONDS);
		}
	}

	protected boolean isRecoverable(ExecutionException ex){
		if(ErrorCode.isWrongResourceSpec(ex.getErrorCode()))return false;
		if(ErrorCode.isNonRecoverableSubmissionError(ex.getErrorCode()))return false;
		return true;
	}

	/**
	 * handle "queued" state
	 */
	protected void handleQueued()throws ExecutionException{
		logger.trace("Handling QUEUED state for Action {}", action.getUUID());
		try{
			exec.updateStatus(action);
			if(action.getStatus()==ActionStatus.QUEUED){
				action.setWaiting(true);
			}
		}catch(ExecutionException ex){
			String msg="Could not update status: "+ex.getMessage();
			action.addLogTrace(msg);
			throw ExecutionException.wrapped(ex);
		}
	}

	/**
	 * handle "RUNNING" state
	 */
	protected void handleRunning()throws ExecutionException{
		logger.trace("Handling RUNNING state for Action {}", action.getUUID());
		if(getTimeStamp(TIME_START_MAIN)==null){
			storeTimeStamp(TIME_START_MAIN);
		}
		try{
			exec.updateStatus(action);
			if(action.getStatus()==ActionStatus.RUNNING){
				action.setWaiting(true);
			}
		}catch(ExecutionException ex){
			String msg="Could not update status for action "+ex.getMessage();
			action.addLogTrace(msg);
			throw ExecutionException.wrapped(ex);
		}
	}

	@Override
	protected void handlePostProcessing() throws Exception{
		ApplicationExecutionStatus aes=action.getProcessingContext().get(ApplicationExecutionStatus.class);

		switch(aes.get()){

		case ApplicationExecutionStatus.CREATED:
		case ApplicationExecutionStatus.PRECOMMAND_DONE:
		case ApplicationExecutionStatus.MAIN_EXECUTION:
		case ApplicationExecutionStatus.MAIN_EXECUTION_DONE:
			if(getTimeStamp(TIME_END_MAIN)==null){
				storeTimeStamp(TIME_END_MAIN);
			}
			if(checkMainExecutionSuccess()){
				setupPostCommand();
			}
			break;

		case ApplicationExecutionStatus.POSTCOMMAND_EXECUTION:
			handlePostCommandRunning();
			break;

		case ApplicationExecutionStatus.DONE:
			if(fileSystemReadyBeforeStageout())
			{
				handleStageOut();
			}
			break;

		default:
			throw new IllegalStateException("Illegal state: POSTPROCESSING with substate "+aes);
		}

	}

	protected boolean fileSystemReadyBeforeStageout() {

		List<DataStageOutInfo> stageOuts = action.getStageOuts();
		if(stageOuts==null  || stageOuts.size()==0){
			return true;
		}

		Object fsCheckedFlag = action.getProcessingContext().get(KEY_FS_CHECKED_FLAG);
		if(fsCheckedFlag != null)
		{
			// checked before
			return true; 
		}

		String uspace=action.getExecutionContext().getWorkingDirectory();

		TSI tsi = xnjs.getTargetSystemInterface(action.getClient());

		Set<String> toCheck = new HashSet<String>();
		String fileSep = "/";

		try {
			fileSep = tsi.getFileSeparator();

			for(DataStagingInfo dst:stageOuts){
				try{

					String workingDirectory=uspace;
					String fsName=dst.getFileSystemName();
					if(fsName!=null){
						String fs = xnjs.get(IDB.class).getFilespace(fsName);
						if(fs==null){
							continue;
						}
						workingDirectory=tsi.resolve(fs);

					}
					toCheck.add(workingDirectory+fileSep+dst.getFileName());
				}
				catch(Exception e)
				{
					// do nothing
				}
			}
		} catch (Exception e) {
			logger.warn("Unable to list stage-outs "+e.getMessage(), e);
		}

		String out = action.getExecutionContext().getStdout();
		if(out != null) 
		{
			toCheck.add(uspace+fileSep+out);
		}
		String err = action.getExecutionContext().getStderr();
		if(err != null) 
		{
			toCheck.add(uspace+fileSep+err);
		}
		if(toCheck.isEmpty())
		{
			action.getProcessingContext().put(KEY_FS_CHECKED_FLAG,Boolean.TRUE);	
			action.setDirty();
			return true;
		}

		boolean allFilesExist = true;
		try {
			for(String fullPath : toCheck)
			{
				if(tsi.getProperties(fullPath) == null)
				{
					allFilesExist = false;
					break;
				}
			}
		} catch (Exception e) {
			// NOP
		}
		if(allFilesExist) 
		{
			action.getProcessingContext().put(KEY_FS_CHECKED_FLAG,Boolean.TRUE);	
			action.setDirty();
			return true;
		}

		long timeout = 1000 * xnjs.getIOProperties().getIntValue(IOProperties.STAGING_FS_GRACE);
		Long firstFailure = (Long) action.getProcessingContext().get(KEY_FIRST_STAGEOUT_FAILURE);
		long currentTime = System.currentTimeMillis();
		if(firstFailure == null)
		{
			action.getProcessingContext().put(KEY_FIRST_STAGEOUT_FAILURE,currentTime);	
			action.setDirty();
		}
		else if(currentTime - firstFailure > timeout)
		{
			// waited longer than timeout..
			action.getProcessingContext().put(KEY_FS_CHECKED_FLAG,Boolean.TRUE);	
			action.setDirty();
			return true;
		}
		return false; // need to wait a little longer

	}



	/**
	 * handle "STAGING_OUT"
	 */
	protected void handleStageOut() throws Exception{
		if(getTimeStamp(TIME_START_STAGEOUT)==null)storeTimeStamp(TIME_START_STAGEOUT);

		//get id of sub action
		String id=(String)action.getProcessingContext().get(subactionkey_out);
		if(id!=null){
			ActionResult res=checkSubAction(id, "Stage-out", true);
			if(res!=null){
				if(!res.isSuccessful()){
					setToDoneAndFailed("Stage-out failed: "+res.getErrorMessage());	
				}
				else{
					setToDoneSuccessfully();	
				}
			}
		}
		else if(hasStageOut()){
			addStageOut();
		}
		else{
			setToDoneSuccessfully();
		}
	}

	/**
	 * check a subaction for its status.
	 * 
	 * @param id - the subaction id
	 * @param name - the name (pre-commands,post-commands,etc)
	 * @param deleteIfDone - whether to delete the action if it is finished
	 * @return <code>ActionResult</code> if done, null otherwise
	 */
	protected ActionResult checkSubAction(String id, String name, boolean deleteIfDone) throws Exception {
		if(manager.isActionDone(id)){
			Action subAction=manager.getAction(id);
			action.addLogTrace(name+" log:");
			action.appendLogTraceFrom(subAction);
			action.addLogTrace(name+" is DONE.");
			ActionResult r=subAction.getResult();
			if(!r.isSuccessful()){
				StringBuilder errorDescription=new StringBuilder(name+" was NOT SUCCESSFUL");
				if(r.getErrorMessage()!=null){
					errorDescription.append(": "+r.getErrorMessage());
				}
				action.addLogTrace(errorDescription.toString());
			}
			if(deleteIfDone){
				ems.destroy(id, action.getClient());
			}
			return r;
		}
		else{
			return null;
		}
	}

	/**
	 * this method extracts job info like {@link ApplicationInfo} from
	 * the job description and fills the proper fields in the current action
	 */
	protected abstract void extractFromJobDescription()throws Exception;

	/**
	 * extract the notBefore tag
	 */
	protected abstract void extractNotBefore() throws ExecutionException;

	/**
	 * Populate job environment with "interesting" stuff.
	 *  
	 * Client attributes whose names start with "UC_" are copied to 
	 * the environment.
	 */
	protected void setEnvironmentVariables(){
		Client c = action.getClient();
		ExecutionContext ec = action.getExecutionContext();
		if(c!=null && c.getExtraAttributes()!=null){
			for(Map.Entry<String, String>e : c.getExtraAttributes().entrySet()){
				if(e.getKey().startsWith("UC_")){
					ec.getEnvironment().put(e.getKey(), e.getValue());
				}
			}
		}
	}

	/**
	 * updates ExecutionContext with values from the effective application
	 * @param appDescription
	 */
	public void updateExecutionContext(ApplicationInfo appDescription){
		ExecutionContext ec=action.getExecutionContext();

		String executable=appDescription.getExecutable();
		action.getExecutionContext().setExecutable(executable);

		if(appDescription.getStdout()!=null)ec.setStdout(appDescription.getStdout());
		if(appDescription.getStderr()!=null)ec.setStderr(appDescription.getStderr());
		if(appDescription.getStdin()!=null)ec.setStdin(appDescription.getStdin());

		Map<String,String>map = ec.getEnvironment();
		map.putAll(appDescription.getEnvironment());

		IDB idb = xnjs.get(IDB.class);
		for(String fileSystemName: idb.getFilesystemNames()){
			String fs = idb.getFilespace(fileSystemName);
			map.put(fileSystemName, fs);
		}

		//interactive execution
		if(appDescription.isRunOnLoginNode() 
				// legacy environment variable
				|| Boolean.parseBoolean(appDescription.getEnvironment().get("UC_PREFER_INTERACTIVE_EXECUTION"))){
			ec.setRunOnLoginNode(true);
		}
		ec.setPreferredExecutionHost(appDescription.getPreferredLoginNode());

		action.setDirty();
	}

	private static Long getTimeQueued(ProcessingContext context){
		Long submitToBSS=(Long)context.get(TIME_SUBMITTED);
		Long startBSSExecution=(Long)context.get(TIME_START_MAIN);
		if(startBSSExecution!=null && submitToBSS!=null){
			return (startBSSExecution-submitToBSS)/1000;
		}
		else return null;
	}

	public static String getTimeProfile(ProcessingContext context){
		try{
			return new TimeProfile(context).toString();
		}catch(RuntimeException e){
			return "Time profile data not available";
		}
	}

	public static TimeProfile timeProfile(ProcessingContext context) {
		return new TimeProfile(context);
	}

	public void updateQueuedStats(ProcessingContext context){
		Long timeQueued = getTimeQueued(context);
		if(timeQueued!=null){
			Histogram h = (Histogram)xnjs.getMetrics().get(XNJSConstants.MEAN_TIME_QUEUED);
			if (h!=null)h.update(timeQueued.intValue());
		}
	}

	protected void addStageIn() throws Exception{
		List<DataStageInInfo>toStage = action.getStageIns();
		StagingInfo stageInfo = new StagingInfo(toStage);
		String subId = manager.addSubAction(stageInfo, XNJSConstants.jobStageInActionType, action, true);
		action.getProcessingContext().put(subactionkey_in,subId);
		logger.trace("Adding stage in subaction with id {}", subId);
		action.setWaiting(true);
	}

	protected void addStageOut() throws Exception{
		List<DataStageOutInfo>toStage = action.getStageOuts();
		if(toStage!=null && toStage.size()>0){
			StagingInfo stageOut = new StagingInfo(toStage);
			String subId = manager.addSubAction(stageOut, XNJSConstants.jobStageOutActionType, action, true);
			logger.trace("Adding stage out subaction with id {}", subId);
			action.getProcessingContext().put(subactionkey_out, subId);
			action.setWaiting(true);
		}
		else  {
			setToDoneSuccessfully();
		}
	}

	/**
	 * extract stage-in information from the job 
	 */
	protected abstract List<DataStageInInfo> extractStageInInfo() throws Exception;

	/**
	 * extract stage-out information from the job
	 */
	protected abstract List<DataStageOutInfo> extractStageOutInfo()throws Exception;

	/**
	 * useful if information is in the job description 
	 * that should not be permanently stored
	 * @param modified
	 */
	protected void rewriteJobDescription(T modified) {}

	//delete files from stage in that were marked "DeleteOnTermination"
	@SuppressWarnings("unchecked")
	private void deleteFiles(){
		try{
			List<String>files=(List<String>)action.getProcessingContext().get(KEY_DELETEONTERMINATION);
			if(files==null || files.size()==0)return;
			TSI tsi=xnjs.getTargetSystemInterface(action.getClient());
			tsi.setStorageRoot(action.getExecutionContext().getWorkingDirectory());
			int c=0;
			for(String file: files){
				try{
					tsi.rm(file);
					c++;
				}catch(ExecutionException ee){
					action.addLogTrace("Could not delete file <"+file+">");
				}
			}
			action.addLogTrace("Deleted "+c+" files.");
		}
		catch(Exception e){
			action.addLogTrace("Could not delete files.");
		}
	}

	public void setToDoneSuccessfully(){
		storeTimeStamp(TIME_END);
		action.setStatus(ActionStatus.DONE);
		action.getProcessingContext().set(ApplicationExecutionStatus.done());
		action.addLogTrace("End of processing - successful.");
		int exitcode=getExitCode();
		action.setResult(new ActionResult(ActionResult.SUCCESSFUL,"Success.",exitcode));
		deleteFiles();
		action.addLogTrace(getTimeProfile(action.getProcessingContext()));
		updateQueuedStats(action.getProcessingContext());
		logger.info("Action <{}> SUCCESSFUL.", action.getUUID());
	}

	@Override
	protected void setToDoneAndFailed(String reason){
		super.setToDoneAndFailed(reason);
		action.addLogTrace("Status set to DONE - failure.");
		updateQueuedStats(action.getProcessingContext());
		logger.info("Action <{}> FAILED{}", action.getUUID(),(reason!=null?": "+reason:"."));
	}

	/**
	 * get the exit code
	 */
	protected int getExitCode(){
		Integer i=action.getExecutionContext().getExitCode();
		return i==null? 0 : i.intValue();
	}

	@Override
	protected void handlePausing() throws ExecutionException {
		try{
			exec.pause(action);
			action.addLogTrace("Paused.");
		}catch(Exception ex){
			action.addLogTrace(LogUtil.createFaultMessage("Could not pause action on BSS", ex));
		}
		super.handlePausing();
	}
	
	@Override
	protected void handleResuming() throws ExecutionException {
		try{
			exec.resume(action);
			action.addLogTrace("Resumed.");
		}catch(Exception ex){
			action.addLogTrace(LogUtil.createFaultMessage("Could not resume action on BSS", ex));
		}
		super.handleResuming();
	}

	@Override
	protected void handleAborting() throws ExecutionException {
		try{
			exec.abort(action);
		}catch(Exception ex){
			action.addLogTrace(LogUtil.createFaultMessage("Could not abort action on BSS", ex));
		}
		try{
			abortFileTransfers();
		}catch(Exception ex){
			action.addLogTrace(LogUtil.createFaultMessage("Could not abort file transfers", ex));
		}	
		super.handleAborting();
	}

	protected void abortFileTransfers()throws Exception{
		String id=null;
		if(action.getStatus()==ActionStatus.PREPROCESSING){
			id=(String)action.getProcessingContext().get(subactionkey_in);
		}
		else if(action.getStatus()==ActionStatus.POSTPROCESSING){
			id=(String)action.getProcessingContext().get(subactionkey_out);
		}
		if(id!=null){
			ems.abort(id, action.getClient());
		}
	}

	@Override
	protected void handleRemoving() throws ExecutionException {
		try{
			if(ActionStatus.canAbort(action.getStatus()))exec.abort(action);
		}
		catch(ExecutionException ex){
			logger.debug("Could not abort action",ex);
		}
		try{
			ecm.destroyUSpace(action);
			if(action.getStatus()!=ActionStatus.DESTROYED){
				action.setStatus(ActionStatus.DESTROYED);
				action.setResult(new ActionResult(ActionResult.USER_ABORTED));
			}
		}finally {
			action.setTransitionalStatus(ActionStatus.TRANSITION_NONE);
		}
	}


	@Override
	protected void handleRestarting() throws Exception {
		action.addLogTrace("RESTARTING job.");
		action.getProcessingContext().remove(Execution.BSS_SUBMIT_COUNT);
		action.getProcessingContext().remove(ApplicationExecutionStatus.class);
		action.getProcessingContext().remove(TIME_SUBMITTED);
		action.getProcessingContext().remove(TIME_START_PRE);
		action.getProcessingContext().remove(TIME_START_MAIN);
		action.getProcessingContext().remove(TIME_END_MAIN);
		action.getProcessingContext().remove(TIME_START_POST);
		action.getProcessingContext().remove(TIME_START_STAGEOUT);
		action.setBSID(null);
		action.setStatus(ActionStatus.PENDING);
		action.addLogTrace("Status set to PENDING.");
	}

	/**
	 * sets the current action on "hold" for the time specified
	 * @param time
	 * @param units
	 */
	protected void pauseExecution(int time, TimeUnit units){
		action.setWaiting(true);
		XnjsEvent e=new ContinueProcessingEvent(action.getUUID());
		manager.scheduleEvent(e, time, units);
	}

	public static class TimeProfile {

		public Long total = null;
		public Long queued = null;
		public Long pre = null;
		public Long main = null;
		public Long post = null;
		public Long stageIn = null;
		public Long stageOut = null;

		public TimeProfile(ProcessingContext context) {
			Long start=(Long)context.get(TIME_START);
			Long endStageIn=(Long)context.get(TIME_END_STAGEIN);
			Long startStageOut=(Long)context.get(TIME_START_STAGEOUT);
			Long end=(Long)context.get(TIME_END);
			if(startStageOut==null)startStageOut = end;

			if(end!=null)total = (end-start)/1000;
			if(endStageIn!=null)stageIn = (endStageIn-start)/1000;
			try {
				pre = ((Long)context.get(TIME_SUBMITTED) - (Long)context.get(TIME_START_PRE))/1000;
			}catch(Exception ex) {}
			queued = getTimeQueued(context);
			try {
				main = ((Long)context.get(TIME_END_MAIN) - (Long)context.get(TIME_START_MAIN))/1000;
			}catch(Exception ex) {}
			try {
				post = (startStageOut - (Long)context.get(TIME_START_POST))/1000;
			}catch(Exception ex) {}
			try {
				stageOut = (end-startStageOut)/1000;
			}catch(Exception ex) {}
		}

		public String toString() {
			StringBuilder sb=new StringBuilder();
			if(total!=null) {
				sb.append("Total: "+String.format("%d sec.", total));
				sb.append(", ");
			}
			if(stageIn!=null) {
				sb.append("Stage-in: "+String.format("%d sec.", stageIn));
				sb.append(", ");
			}
			if(pre!=null) {
				sb.append("Pre: "+String.format("%d sec.", pre));
				sb.append(", ");
			}
			if(queued!=null) {
				sb.append("Queued: "+String.format("%d sec.", queued));
				sb.append(", ");
			}
			if(main!= null) {
				sb.append("Main: "+String.format("%d sec.", main));
				sb.append(", ");
			}
			if(post!=null) {
				sb.append("Post: "+String.format("%d sec.", post));
				sb.append(", ");
			}
			if(stageOut!=null) {
				sb.append("Stage-out: "+String.format("%d sec.", stageOut));
			}
			return sb.toString();
		}

		public Map<String,String> asMap(){
			Map<String,String>res = new HashMap<>();
			res.put("total", total!=null? String.valueOf(total) : "N/A");
			res.put("stage-in", stageIn!=null? String.valueOf(stageIn) : "N/A");
			res.put("preCommand", pre!=null? String.valueOf(pre) : "N/A");
			res.put("queued", queued!=null? String.valueOf(queued) : "N/A");
			res.put("main", main!=null? String.valueOf(main) : "N/A");
			res.put("postCommand", post!=null? String.valueOf(post) : "N/A");
			res.put("stage-out", stageOut!=null? String.valueOf(stageOut) : "N/A");
			return res;
		}
	}
}
