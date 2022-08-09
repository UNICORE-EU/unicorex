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
import de.fzj.unicore.xnjs.tsi.remote.TSIMessages;
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
		if(subCommand.outputDir != null){
			ec.setOutputDirectory(subCommand.outputDir);
		}else{
			ec.setOutputDirectory(ec.getWorkingDirectory()+".UNICORE_"+subCommand.id+tsi.getFileSeparator());	
		}
		tsi.mkdir(ec.getOutputDirectory());
		ec.setExitCodeFileName(subCommand.id+"_" + TSIMessages.EXITCODE_FILENAME);
		ec.setIgnoreExitCode(subCommand.ignoreExitCode);
		ec.setRunOnLoginNode(true);
		ec.setPIDFileName(subCommand.id+"_" + TSIMessages.PID_FILENAME);
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
			exec.submit(action);
			action.setStatus(ActionStatus.RUNNING);
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
		logger.trace("Handling QUEUED state for Action {}", action.getUUID());
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
		logger.trace("Handling RUNNING state for action {}", action.getUUID());
		try{
			exec.updateStatus(action);
			if(action.getStatus()==ActionStatus.RUNNING){
				sleep(30000);
			}
		}catch(ExecutionException ex){
			String msg="Could not update status for action "+ex.getMessage();
			action.addLogTrace(msg);
			throw new ProcessingException(msg,ex);
		}
	}

	@Override
	protected void handlePostProcessing()throws ProcessingException{
		Integer e = action.getExecutionContext().getExitCode();
		if(e!=null) {
			boolean ignore = action.getExecutionContext().isIgnoreExitCode();
			if(e!=0 && !ignore) {
				setToDoneAndFailed("Command exited with non-zero exit code: <" + e + ">."
						+ " More information might be available in the job's working directory '"
						+ action.getExecutionContext().getWorkingDirectory() + "'");
				return;
			}
		}
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
		
		public static final int NORMAL = 0;
		public static final int UFTP = 1;
		
		private final static long serialVersionUID=1l;
		
		public int type = NORMAL;
		
		public String id;   //identifier of this subcommand
		
		public String cmd;  //the command to execute
		public String workingDir; // working directory for the command
		public String preferredExecutionHost; //preferred TSI host
		
		//outcome directory for the command (if null, outcome goes to working dir)
		public String outputDir;
		public String stdout="stdout";
		public String stderr="stderr";
		public String umask;
		
		public final Map<String,String>env = new HashMap<>();
		
		// if true, the job and its working directory will be destroyed when
		// the command is finished
		public boolean destroyWhenDone=false;
		
		// if false, non-zero exit code will cause the command and parent job to FAIL
		public boolean ignoreExitCode=false;
		
		public String toString(){
			return "SubCommand ['"+cmd+"' in '"+workingDir+"']";
		}
	}
	
}
