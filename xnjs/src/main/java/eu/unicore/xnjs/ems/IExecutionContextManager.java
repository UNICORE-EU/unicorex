package eu.unicore.xnjs.ems;

/**
 * setup the action's @see {@link ExecutionContext}
 *
 * @author schuller
 */
public interface IExecutionContextManager {

	/**
	 * initialise the context
	 */
	public void initialiseContext(Action action) throws ExecutionException;

	/**
	 * create a working directory for the given action, if it does not yet exist. 
	 * The working directory is created in the configured location
	 * (XNJSProperties.FILESPACE)
	 *
	 * @param action - the action
	 * @return the new uspace directory
	 *
	 * @throws ExecutionException
	 */
	public String createUSpace(Action action) throws ExecutionException;

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
	 * initialise the context of a "child" action. This means the environment etc will be inherited
	 * from the parent, but stdout/stderr do not have the default names, but have a uuid appended
	 *
	 * @param parentAction
	 * @param childAction
	 */
	public void initialiseChildContext(Action parentAction, Action childAction) throws ExecutionException;

}
