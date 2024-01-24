package eu.unicore.xnjs.tsi;

import eu.unicore.xnjs.ems.Action;
import eu.unicore.xnjs.ems.ActionStatus;
import eu.unicore.xnjs.ems.ExecutionException;

/**
 * The basic interface to the execution system<br>
 * The various methods will change the action, for example set the BSID.
 * These are executed after the client has been authenticated, so there is no more 
 * reference to the client.<br>
 *
 * @author schuller
 */
public interface IExecution {
	
	/**
	 * uspace name of the (optional) file containing progress information.
	 * (this file may be written by the application or through some other means) 
	 */
	public static final String PROGRESS_FILENAME=".UNICORE_PROGRESS_INDICATION";
	public static final String PROGRESS_NOT_FOUND_KEY="UNICORE_PROGRESS_INDICATION_UNAVAILABLE";
	
	/**
	 * submit a job
	 * 
	 * @param job - the action 
	 * @return initial action state after submission - QUEUED or RUNNING, see {@link ActionStatus}
	 */
	public int submit(Action job) throws TSIBusyException,ExecutionException;
	
	/**
	 * abort a job
	 */
	public void abort(Action job) throws ExecutionException;
	
	/**
	 * pause a job
	 */
	public void pause(Action job) throws ExecutionException;
	
	/**
	 * resume a job
	 */
	public void resume(Action job) throws ExecutionException;
	
	/**
	 * checkpoint a job to allow job migration to a different machine
	 */
	public void checkpoint(Action job) throws ExecutionException;
	
	/**
	 * restart a checkpointed job
	 */
	public void restart(Action job) throws ExecutionException;
	
	/**
	 * update the status of the action on the target system
	 */
	public void updateStatus(Action job) throws ExecutionException;
	
	/**
	 * Get batch-system level job details
	 */
	public String getBSSJobDetails(Action job) throws ExecutionException;

	/**
	 * enable/disable (periodic) back-end job status checking
	 */
	public default void toggleStatusUpdates(boolean enable) {}
	
	public boolean isBeingTracked(Action job) throws ExecutionException;

}
