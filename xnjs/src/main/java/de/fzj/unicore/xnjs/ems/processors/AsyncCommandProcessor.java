package de.fzj.unicore.xnjs.ems.processors;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.ActionResult;
import de.fzj.unicore.xnjs.ems.ActionStatus;
import de.fzj.unicore.xnjs.ems.ExecutionContext;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.ems.ProcessingException;
import de.fzj.unicore.xnjs.idb.ApplicationInfo;
import de.fzj.unicore.xnjs.tsi.IExecution;
import de.fzj.unicore.xnjs.tsi.TSI;
import de.fzj.unicore.xnjs.tsi.TSIBusyException;
import de.fzj.unicore.xnjs.tsi.remote.TSIUtils;
import de.fzj.unicore.xnjs.util.LogUtil;

/**
 * used to asynchronously run commands on the cluster login node
 *
 * @author schuller
 */
public class AsyncCommandProcessor extends DefaultProcessor {
	
	private static final Logger logger=LogUtil.getLogger(LogUtil.JOBS, AsyncCommandProcessor.class);
	
	private final IExecution exec;
	
	public AsyncCommandProcessor(XNJS xnjs){
		super(xnjs);
		exec=xnjs.get(IExecution.class);
	}
	
	@Override
	protected void handleCreated()throws ProcessingException{
		try{
			submit();
		}catch(ExecutionException e){
			throw new ProcessingException(e);
		}
	}
	
	protected void submit()throws ExecutionException{
		SubCommand subCommand = (SubCommand)action.getAjd();
		ExecutionContext ec=action.getExecutionContext();
		TSI tsi = xnjs.getTargetSystemInterface(action.getClient());
		tsi.setUmask(subCommand.umask);
		if(subCommand.workingDir != null) ec.setWorkingDirectory(subCommand.workingDir);
		if(subCommand.outcomeDir != null){
			ec.setOutcomeDirectory(subCommand.outcomeDir);
		}else{
			ec.setOutcomeDirectory(ec.getWorkingDirectory()+".UNICORE_"+subCommand.id+tsi.getFileSeparator());	
		}
		tsi.mkdir(ec.getOutcomeDirectory());
		ec.setExitCodeFileName(subCommand.id+"_"+TSIUtils.EXITCODE_FILENAME);
		ec.setRunOnLoginNode(true);
		ec.setPIDFileName(subCommand.id+"_"+TSIUtils.PID_FILENAME);
		ec.getEnvironment().putAll(subCommand.env);
		ec.setPreferredExecutionHost(subCommand.preferredExecutionHost);
		ec.setStderr(subCommand.stderr);
		ec.setStdout(subCommand.stdout);
		ec.setUmask(subCommand.umask);
		action.addLogTrace("Executing command: "+subCommand.cmd);
		action.setStatus(ActionStatus.PENDING);
		ApplicationInfo appInfo=action.getApplicationInfo();
		appInfo.setExecutable(subCommand.cmd);
	}
	
	@Override
	protected void handlePending()throws ProcessingException{
		try{
			action.setStatus(ActionStatus.QUEUED);
			exec.submit(action);
			sleep(3000);
		}catch(ExecutionException e){
			throw new ProcessingException(e);
		}catch(TSIBusyException tbe){
			logger.debug("Could not submit action",tbe);
			//will retry later
			action.setWaiting(false);
		}	
	}
	
	protected void handleQueued()throws ProcessingException{
		if(logger.isTraceEnabled())logger.trace("Handling QUEUED state for Action "+action.getUUID());
		try{
			exec.updateStatus(action);
			if(action.getStatus()==ActionStatus.QUEUED){
				sleep(3000);
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
		try{
			exec.updateStatus(action);
			if(action.getStatus()==ActionStatus.RUNNING){
				sleep(3000);
			}
		}catch(ExecutionException ex){
			String msg="Could not update status for action "+ex.getMessage();
			action.addLogTrace(msg);
			throw new ProcessingException(msg,ex);
		}
	}
	
	
	@Override
	protected void handlePostProcessing()throws ProcessingException{
		setToSuccess();
	}	
	
	protected void setToSuccess(){
		action.setStatus(ActionStatus.DONE);
		action.setResult(new ActionResult(ActionResult.SUCCESSFUL));
		SubCommand subCommand = (SubCommand)action.getAjd();
		if(subCommand.destroyWhenDone){
			action.setTransitionalStatus(ActionStatus.TRANSITION_REMOVING);
		}
	}
	
	@Override
	protected void handleRemoving()throws ProcessingException{
		action.setStatus(ActionStatus.DESTROYED);
	}
	
	public static class SubCommand implements Serializable{
		private final static long serialVersionUID=1l;
		public String id;   //identifier of this subcommand
		
		public String cmd;  //the command to execute
		public String workingDir; // working directory for the command
		public String preferredExecutionHost; //preferred TSI host
		
		//outcome directory for the command (if null, outcome goes to working dir)
		public String outcomeDir;
		public String stdout="stdout";
		public String stderr="stderr";
		public String umask;
		
		public Map<String,String>env=new HashMap<String, String>();
		
		// if true, the job and its working directory will be destroyed when
		// the command is finished
		public boolean destroyWhenDone=false;
		
		public String toString(){
			return "SubCommand ['"+cmd+"' in '"+workingDir+"']";
		}
	}
	
}
