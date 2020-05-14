package de.fzj.unicore.xnjs.util;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.ExecutionContext;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.ems.IExecutionContextManager;
import de.fzj.unicore.xnjs.tsi.TSI;
import de.fzj.unicore.xnjs.tsi.TSIBusyException;
import eu.unicore.security.Client;

/**
 * helpers to execute a script on the TSI
 * 
 * @author schuller
 */
public class ExecuteScript {
	
	private ExecuteScript(){}
	
	/**
	 * Execute a shell script in a temporary directory.
	 * After execution, this directory is removed.
	 * 
	 * @param script - the script to execute
	 * @param client -  the client for which to execute the script
	 * @param config -  config object
	 * @throws ExecutionException
	 * @throws TSIBusyException
	 * @return a {@link ResultHolder} for retrieving the results
	 */
	public static ResultHolder executeScript(String script, Client client, XNJS config) throws ExecutionException, TSIBusyException{
		TSI tsi=config.getTargetSystemInterface(client);
		Action action=new Action();
		action.setType("Execute IDB script");
		ExecutionContext ec=config.get(IExecutionContextManager.class).getContext(action);
		tsi.execAndWait(script, ec);
		return new ResultHolder(action,config);
	}

	public static void executeAsync(String script, Action action, XNJS config)throws ExecutionException, TSIBusyException{
		throw new ExecutionException("not implemented!");
	}
	
}
