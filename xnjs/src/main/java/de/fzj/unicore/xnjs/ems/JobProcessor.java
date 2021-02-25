/*********************************************************************************
 * Copyright (c) 2011 Forschungszentrum Juelich GmbH 
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


package de.fzj.unicore.xnjs.ems;

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

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.XNJSConstants;
import de.fzj.unicore.xnjs.XNJSProperties;
import de.fzj.unicore.xnjs.ems.event.ContinueProcessingEvent;
import de.fzj.unicore.xnjs.ems.event.XnjsEvent;
import de.fzj.unicore.xnjs.ems.processors.AsyncCommandProcessor.SubCommand;
import de.fzj.unicore.xnjs.ems.processors.DefaultProcessor;
import de.fzj.unicore.xnjs.idb.ApplicationInfo;
import de.fzj.unicore.xnjs.idb.IDB;
import de.fzj.unicore.xnjs.incarnation.TweakerExecutionException;
import de.fzj.unicore.xnjs.io.DataStageInInfo;
import de.fzj.unicore.xnjs.io.DataStageOutInfo;
import de.fzj.unicore.xnjs.io.DataStagingInfo;
import de.fzj.unicore.xnjs.io.StagingInfo;
import de.fzj.unicore.xnjs.tsi.IExecution;
import de.fzj.unicore.xnjs.tsi.TSI;
import de.fzj.unicore.xnjs.tsi.TSIBusyException;
import de.fzj.unicore.xnjs.tsi.remote.Execution;
import de.fzj.unicore.xnjs.util.LogUtil;
import eu.unicore.security.Client;

/**
 * abstract processor for JSDL-like actions, including pre-run, staging, post-run, etc
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
	 * get the job description document via cast: (T)action.getAjd();
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
	protected void handleCreated()throws ProcessingException{
		try{
			setupNotifications();
			
			if(isEmptyJob()){
				String msg="Empty job description. Setting to DONE.";
				action.addLogTrace(msg);
				setToDoneSuccessfully();
				return;
			}
			storeTimeStamp(TIME_START);
			try{
				extractFromJobDescription();
				setEnvironmentVariables();
			}
			catch(Exception ee){
				throw new ProcessingException(ee);
			}
			if(hasStageIn()){
				action.setStatus(ActionStatus.PREPROCESSING);
				action.addLogTrace("Status set to PREPROCESSING (staging in).");
				//check if we do stage in
				if(xnjs.haveProcessingFor(XNJSConstants.jsdlStageInActionType)){
					addStageIn();
					//sleep till stage-ins are done
					action.setWaiting(true);
				}
				else  {
					action.addLogTrace("Staging in not done (no processing configured).");
					setToReady();
					return;
				}
			}
			else{
				//nothing to stage in
				action.addLogTrace("No staging in needed.");
				setToReady();
			}
			//processing problems?	
		}catch(Exception ex){
			String msg="Error processing action "+ex.getMessage();
			action.addLogTrace(msg);
			throw new ProcessingException(msg,ex);
		}
	}
	
	/**
	 * handle state "PreProcessing" aka "Staging in"
	 */
	protected void handlePreProcessing() throws ProcessingException {
		try{
			//get id of stage in sub action
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
	protected void handleStagingIn(String id)throws ExecutionException, ProcessingException{
		ActionResult res=checkSubAction(id, "Stage in", false);
		if(res!=null){
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
	protected void handleReady() throws ProcessingException {
		if(logger.isTraceEnabled())logger.trace("Handling READY state for Action "
				+action.getUUID());

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
	protected void handlePending() throws ProcessingException{
		if(logger.isTraceEnabled())logger.trace("Handling PENDING state for Action "+action.getUUID());

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
			ids=new ArrayList<String>();
			action.getProcessingContext().put(key, ids);
		}
		return ids;
	}

	/**
	 * initialise pre-command execution
	 * @throws ProcessingException
	 */
	protected void setupPreCommand() throws ProcessingException{
		try{
			boolean done = true;
			int index=0;
			StringBuilder pre = new StringBuilder();
			if(action.getApplicationInfo().getPreCommand()!=null) {
				pre.append(action.getApplicationInfo().getPreCommand());
				pre.append("\n");
			}
			
			// user-defined pre-command
			String userPre = action.getApplicationInfo().getUserPreCommand();
			if(userPre != null && action.getApplicationInfo().isUserPreCommandOnLoginNode()){
				pre.append(userPre);
			}
			
			if(pre.length()>0) {
				done = false;
				SubCommand cmd = new SubCommand();
				cmd.id = "PRE_"+(index++);
				cmd.cmd = pre.toString();
				cmd.workingDir = action.getExecutionContext().getWorkingDirectory();
				cmd.ignoreExitCode = action.getApplicationInfo().isUserPreCommandIgnoreExitCode();
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
			throw new ProcessingException(msg,ex);
		}	
	}


	@SuppressWarnings("unchecked")
	protected void handlePreCommandRunning()throws ProcessingException{
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
			if(errors.length()>0){
				setToDoneAndFailed("Pre-command(s) failed: "+errors.toString());
			}
			if(ids.size()==0){
				action.getProcessingContext().set(ApplicationExecutionStatus.precommandDone());
			}
			action.setDirty();
		}
		catch(ExecutionException ee){
			throw new ProcessingException(ee);
		}
	}

	protected boolean checkMainExecutionSuccess() throws ProcessingException {
		if(action.getApplicationInfo().ignoreNonZeroExitCode())return true;
		Integer exitCode = action.getExecutionContext().getExitCode();
		if(exitCode!=null && exitCode!=0){
			String msg = "User application exited with non-zero exit code: <"+exitCode+">";
			setToDoneAndFailed(msg);
			return false;
		}
		return true;
	}

	/**
	 * initialise post-command execution
	 * @throws ProcessingException
	 */
	protected void setupPostCommand() throws ProcessingException{
		try{
			boolean done = true;
			int index=0;
						
			String userPost = action.getApplicationInfo().getUserPostCommand();
			if(userPost != null && action.getApplicationInfo().isUserPostCommandOnLoginNode()){
				done = false;
				SubCommand cmd = new SubCommand();
				cmd.id = "POST_"+(index++);
				cmd.cmd = userPost;
				cmd.workingDir = action.getExecutionContext().getWorkingDirectory();
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
			throw new ProcessingException(msg,ex);
		}	
	}

	@SuppressWarnings("unchecked")
	protected void handlePostCommandRunning()throws ProcessingException{
		try{
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
			if(errors.length()>0){
				setToDoneAndFailed("Post-command(s) failed: "+errors.toString());
			}
			if(ids.size()==0){
				action.getProcessingContext().set(ApplicationExecutionStatus.done());
			}
		}
		catch(ExecutionException ee){
			throw new ProcessingException(ee);
		}

	}

	/**
	 * submits the given command array as a sub action
	 */
	protected String createPrePostAction(SubCommand cmd)throws ExecutionException{
		return manager.addSubAction(cmd, XNJSConstants.asyncCommandType, action, true);
	}

	/**
	 * submit the main application
	 */
	protected void submitMainExecutable() throws ProcessingException{
		try{
			ApplicationInfo appInfo=action.getApplicationInfo();
			if(appInfo==null || appInfo.getExecutable()==null){
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
		}catch(TweakerExecutionException tex){
			String msg="Could not submit job: "+tex.getMessage();

			action.addLogTrace(msg);
			setToDoneAndFailed(msg);
			throw new ProcessingException(msg,tex);

		}catch(ExecutionException ex){
			String msg="Could not submit job: "+ex.getMessage();

			Integer submitCount=(Integer)action.getProcessingContext().get(Execution.BSS_SUBMIT_COUNT);
			if(submitCount==null)submitCount = Integer.valueOf(0);
			submitCount++;
			action.getProcessingContext().put(Execution.BSS_SUBMIT_COUNT, submitCount);
			int maxSubmitCount = xnjs.getXNJSProperties().getResubmitCount();
			
			if(!isRecoverable(ex) || submitCount.intValue()>maxSubmitCount){
				action.addLogTrace(msg);
				setToDoneAndFailed(msg);
				throw new ProcessingException(msg,ex);
			}

			action.addLogTrace("Submit attempt "+submitCount+" (of "+maxSubmitCount+") failed: "+ex.getMessage());
			
			pauseExecution(xnjs.getXNJSProperties().getResubmitDelay(), TimeUnit.SECONDS);

		}	
		catch(TSIBusyException tbe){
			//no problem, will retry
			action.setWaiting(false);
		}
	}

	protected boolean isRecoverable(ExecutionException ex){
		if(ex.getErrorCode().isWrongResourceSpec())return false;
		
		return true;
	}

	/**
	 * handle "queued" state
	 */
	protected void handleQueued()throws ProcessingException{
		if(logger.isTraceEnabled())logger.trace("Handling QUEUED state for Action "+action.getUUID());
		try{
			exec.updateStatus(action);
			if(action.getStatus()==ActionStatus.QUEUED){
				action.setWaiting(true);
			}
		}catch(ExecutionException ex){
			String msg="Could not update status: "+ex.getMessage();
			action.addLogTrace(msg);
			throw new ProcessingException(msg,ex);
		}
	}


	/**
	 * handle "RUNNING" state
	 */
	protected void handleRunning()throws ProcessingException{
		if(logger.isTraceEnabled())logger.trace("Handling RUNNING state for Action "+action.getUUID());
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
			throw new ProcessingException(msg,ex);
		}
	}

	@Override
	protected void handlePostProcessing() throws ProcessingException{
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

		long timeout = 1000 * xnjs.getXNJSProperties().getIntValue(XNJSProperties.STAGING_FS_GRACE);
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
	protected void handleStageOut() throws ProcessingException{
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
	protected ActionResult checkSubAction(String id, String name, boolean deleteIfDone)throws ProcessingException{
		try{
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
		}catch(Exception ex){
			throw new ProcessingException("Can't check subaction for "+name,ex);
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
	protected abstract void extractNotBefore() throws ProcessingException;

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

		HashMap<String,String>map=ec.getEnvironment();
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
			e.printStackTrace();
			return "Time profile data not available";
		}
	}

	public static TimeProfile timeProfile(ProcessingContext context) {
		return new TimeProfile(context);
	}
			
	public void updateQueuedStats(ProcessingContext context){
		Long timeQueued = getTimeQueued(context);
		if(timeQueued!=null){
			Histogram h = xnjs.getMetricRegistry().getHistograms().get(XNJSConstants.MEAN_TIME_QUEUED);
			if (h!=null)h.update(timeQueued.intValue());
		}
	}
	
	//deal with adding new stage-in action
	protected void addStageIn() throws ProcessingException{
		try{
			List<DataStageInInfo>toStage = extractStageInInfo();
			StagingInfo stageInfo = new StagingInfo(toStage);
			String subId=manager.addSubAction(stageInfo,
					XNJSConstants.jsdlStageInActionType,action,true);
			action.getProcessingContext().put(subactionkey_in,subId);
			action.addLogTrace("Adding stage in subaction with id="+subId);
		}catch(Exception ex){
			throw new ProcessingException(ex);
		}
	}

	//deal with adding new stage-out action
	protected void addStageOut() throws ProcessingException{
		//check if we need stage out	
		try{
			if(hasStageOut()){
				//check if we can process STAGE_OUT
				if(xnjs.haveProcessingFor(XNJSConstants.jsdlStageOutActionType)){
					List<DataStageOutInfo>toStage=extractStageOutInfo();
					StagingInfo stageOut = new StagingInfo(toStage);
					String subId=manager.addSubAction((Serializable)stageOut,
							XNJSConstants.jsdlStageOutActionType,action,true);
					action.addLogTrace("Adding stage out subaction with id="+subId);
					action.getProcessingContext().put(subactionkey_out, subId);
					action.setWaiting(true);
				}
				else  {
					action.addLogTrace("Staging out not done.");
					setToDoneSuccessfully();
				}
			}
			else{
				//nothing to stage in, go to "DONE"
				action.addLogTrace("No staging out needed.");
				setToDoneSuccessfully();
			}
			//processing problems?	
		}catch(Exception ex){
			String msg="Error processing action: "+ex.getMessage();
			action.addLogTrace(msg);
			throw new ProcessingException(ex);
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


	//delete files from stage in that were marked "DeleteOnTermination"
	@SuppressWarnings("unchecked")
	private void deleteFiles(){
		try{
			List<String>files=(List<String>)action.getProcessingContext().get(KEY_DELETEONTERMINATION);
			if(files==null || files.size()==0)return;
			//get uspace TSI			
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
		action.addLogTrace("Status set to DONE.");
		int exitcode=getExitCode();
		action.setResult(new ActionResult(ActionResult.SUCCESSFUL,"Success.",exitcode));
		action.addLogTrace("Result: Success.");
		deleteFiles();
		action.addLogTrace(getTimeProfile(action.getProcessingContext()));
		updateQueuedStats(action.getProcessingContext());
		logger.info("Action "+action.getUUID()+ " SUCCESSFUL.");
	}

	@Override
	protected void setToDoneAndFailed(String reason){
		super.setToDoneAndFailed(reason);
		updateQueuedStats(action.getProcessingContext());
		logger.info("Action "+action.getUUID()+ " FAILED"+(reason!=null?": "+reason:"."));
	}

	/**
	 * get the exit code
	 */
	protected int getExitCode(){
		Integer i=action.getExecutionContext().getExitCode();
		if(i==null)return 0;
		return i.intValue();
	}

	@Override
	protected void handleAborting() throws ProcessingException {
		try{
			exec.abort(action);
		}catch(Exception ex){
			String msg=LogUtil.createFaultMessage("Could not abort action on BSS", ex);
			action.addLogTrace(msg);
		}
		try{
			abortFileTransfers();
		}catch(Exception ex){
			String msg=LogUtil.createFaultMessage("Could not abort file transfers", ex);
			action.addLogTrace(msg);
		}	
		super.handleAborting();
	}

	protected void abortFileTransfers()throws ExecutionException{
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
	protected void handleRemoving() throws ProcessingException {
		try{
			if(ActionStatus.canAbort(action.getStatus()))exec.abort(action);
		}
		catch(ExecutionException ex){
			logger.warn("Could not abort action",ex);
		}
		try{
			ecm.destroyUSpace(action);
			if(action.getStatus()!=ActionStatus.DESTROYED){
				action.setStatus(ActionStatus.DESTROYED);
				action.setResult(new ActionResult(ActionResult.USER_ABORTED));
			}
		}catch(Exception e){
			throw new ProcessingException(e);
		}
		finally{
			action.setTransitionalStatus(ActionStatus.TRANSITION_NONE);
		}
	}


	@Override
	protected void handleRestarting() throws ProcessingException {
		try{
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
		catch(Exception ex){
			throw new ProcessingException("Could not reset action state for restart",ex);
		}
		super.handleRestarting();
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
