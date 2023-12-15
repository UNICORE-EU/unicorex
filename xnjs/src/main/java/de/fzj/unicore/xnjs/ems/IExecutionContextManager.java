package de.fzj.unicore.xnjs.ems;


/**
 * This interface allows to associate a context with an action
 * 
 * @author schuller
 */
public interface IExecutionContextManager {

	/**
	 * gets a context for the Action. If none exists, it will be created
	 */
	public ExecutionContext getContext(Action action) throws ExecutionException;
	
	/**
	 * create a working directory for the given action, if it does not yet exist. 
	 * The working directory is created in the given base directory, and is named
	 * using the action's uuid.
	 * 
	 * @param action - the action
	 * @param baseDirectory - the base directory
	 * @return the new uspace directory
	 * 
	 * @throws ExecutionException
	 */
	public String createUSpace(Action action, String baseDirectory) throws ExecutionException;
	
	/**
	 * destroys the Action's execution context
	 */
	public void destroyUSpace(Action action) throws ExecutionException;
	
	/**
	 * create a context that is a "child" of the parent's context
	 * this means the environment etc will be inherited, but stdout/stderr
	 * do not have the default names, but have a uuid appended
	 * 
	 * @param parentAction
	 * @param childAction
	 * @return ExecutionContext
	 */
	public ExecutionContext createChildContext(Action parentAction, Action childAction) throws ExecutionException;
	
}
