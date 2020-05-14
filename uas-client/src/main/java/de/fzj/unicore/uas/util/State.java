package de.fzj.unicore.uas.util;

/**
 * The State interface encapsulates two notions: 
 * <ul>
 * <li> a target object is transitioned from the current state to the 
 *      next using the next() method </li> 
 * <li> when errors occur, recovery actions can be taken, or simply a retry 
 *      can occur, as handled by the onError() method </li>
 * </ul>
 * 
 * @author schuller
 */
public interface State<T> {

	/**
	 * transition to the next state
	 * @param target - the target object
	 * @return next State, or <code>null</code> if there is no next state
	 * @throws Exception
	 */
	public State<T> next(T target) throws Exception;
	
	/**
	 * decide how to handle an error which occurred while processing next()
	 * 
	 * @param target - the target object
	 * @param exception - the error that occurred
	 * @return next State, or <code>null</code> if there is no next state
	 * @throws Exception
	 */
	public State<T> onError(T target, Exception exception) throws Exception;

	/**
	 * get this state's name, unique for the state machine, which is both 
	 * useful for humans, and which can be used as e.g. hash map key 
	 */
	public String getName();
	
	/**
	 * whether the state machine can pause after this state:
	 * if <code>false</code> the state machine must always process the 
	 * next state
	 */
	public boolean isPausable();
	
	/**
	 * if this is larger than zero, failed state transitions will be re-tried 
	 * (at most the given number of times)
	 */
	public int getNumberOfRetries();
	
	
	/**
	 * if this is larger than zero, it is used to delay a retry by the given number 
	 * of milliseconds
	 */
	public int getRetryDelay();
}
