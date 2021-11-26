package de.fzj.unicore.xnjs.util;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.XNJSConstants;
import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.ActionStatus;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.ems.IExecutionContextManager;
import de.fzj.unicore.xnjs.ems.InternalManager;
import de.fzj.unicore.xnjs.ems.Manager;
import de.fzj.unicore.xnjs.ems.processors.AsyncCommandProcessor;
import de.fzj.unicore.xnjs.ems.processors.AsyncCommandProcessor.SubCommand;
import de.fzj.unicore.xnjs.idb.ApplicationInfo;
import eu.unicore.security.Client;


/**
 * a helper class for using an asynchronous command on the front-end node 
 * (such as wget, scp, ...)
 * 
 * @see AsyncCommandProcessor
 * @author schuller
 */
public class AsyncCommandHelper {

	protected final XNJS xnjs;

	protected final String parentActionID;

	protected final Client client;

	protected final SubCommand subCommand;
	
	private String subActionID;

	private ResultHolder result;

	/**
	 * @param xnjs
	 * @param cmd - the command to run
	 * @param id - an ID suitable for disambiguating
	 * @param parentActionID - the ID of the parent action
	 * @param client - the client
	 */
	public AsyncCommandHelper(XNJS xnjs, String cmd, String id, String parentActionID, Client client){
		this(xnjs, cmd, id, parentActionID, client, null);
	}

	/**
	 * @param xnjs
	 * @param cmd - the command to run
	 * @param id - an ID suitable for disambiguating
	 * @param parentActionID - the ID of the parent action
	 * @param client  - the client
	 * @param workingDir - the working directory
	 */
	public AsyncCommandHelper(XNJS xnjs, String cmd, String id, String parentActionID, Client client, String workingDir){
		this.xnjs = xnjs;
		this.parentActionID = parentActionID;
		this.subCommand = new SubCommand();
		this.client = client;
		subCommand.id = id;
		subCommand.cmd = cmd;
		subCommand.workingDir = workingDir;
	}

	/**
	 * create and submit the command
	 * @throws ExecutionException
	 */
	public void submit()throws ExecutionException{
		subActionID=createAction();
	}

	public boolean isDone()throws ExecutionException{
		if(subActionID==null)throw new IllegalStateException("Not submitted yet.");
		Action sub=xnjs.get(InternalManager.class).getAction(subActionID);
		if(ActionStatus.DONE==sub.getStatus()){
			result=new ResultHolder(sub, xnjs);
			return true;
		}
		return false;
	}

	protected String createAction()throws ExecutionException{
		InternalManager manager=xnjs.get(InternalManager.class);
		if(parentActionID!=null){
			Action parent=manager.getAction(parentActionID);
			if(parent==null){
				throw new ExecutionException("Cannot create sub-action: parent action does not exist (any more).");
			}
			return manager.addSubAction(subCommand, XNJSConstants.asyncCommandType, parent, true);
		}
		else{
			Action a=new Action();
			a.setAjd(subCommand);
			a.setInternal(true);
			a.setType(XNJSConstants.asyncCommandType);
			a.setClient(client);
			a.setUmask(subCommand.umask);
			a.setApplicationInfo(new ApplicationInfo());
			xnjs.get(IExecutionContextManager.class).getContext(a);
			a.getExecutionContext().setOutcomeDirectory(subCommand.outcomeDir);
			return (String)manager.addInternalAction(a);
		}
	}

	public String getParentActionID() {
		return parentActionID;
	}

	/**
	 * get the ID of the action running the async command
	 */
	public String getActionID() {
		return subActionID;
	}

	public ResultHolder getResult(){
		return result;
	}

	public void setEnvironmentVariable(String key, String value){
		subCommand.env.put(key,value);
	}

	public String getPreferredExecutionHost() {
		return subCommand.preferredExecutionHost;
	}

	public void setPreferredExecutionHost(String preferredExecutionHost) {
		subCommand.preferredExecutionHost = preferredExecutionHost;
	}

	public String getStdout() {
		return subCommand.stdout;
	}

	public void setStdout(String stdout) {
		subCommand.stdout=stdout;
	}
	
	public String getStderr() {
		return subCommand.stderr;
	}

	public void setStderr(String stderr) {
		subCommand.stderr=stderr;
	}
	
	public void setUmask(String umask) {
		subCommand.umask=umask;
	}
	
	public SubCommand getSubCommand() {
		return subCommand;
	}
	
	public void abort()throws ExecutionException{
		if(subActionID!=null){
			xnjs.get(Manager.class).abort(subActionID, client);
		}
	}
}
