package de.fzj.unicore.xnjs.tsi;

import java.util.List;
import java.util.Map;

import de.fzj.unicore.xnjs.ems.BudgetInfo;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.persistence.IActionStore;
import eu.unicore.security.Client;

/**
 * runtime information about the execution system
 *  
 * @author schuller
 */
public interface IExecutionSystemInformation {

	/**
	 * the (best guess for) the total number of jobs
	 * on the (batch) system, i.e. including those not
	 * controlled by UNICORE. This includes queued and 
	 * running jobs
	 */
	public int getTotalNumberOfJobs();
	
	/**
	 * the (best guess for) the total number of running jobs
	 * on the system, i.e. including those not
	 * controlled by UNICORE
	 */
	public int getNumberOfRunningJobs();
	
	
	/**
	 * the (best guess for) the total number of queued jobs
	 * on the system, i.e. including those not
	 * controlled by UNICORE
	 */
	public int getNumberOfQueuedJobs();
	
	/**
	 * get the number of jobs in the queues on the system. 
	 * This can be <code>null</code> if the system isn't 
	 * running a batch system. The set of queues need not be complete,
	 * and the jobs can be RUNNING or QUEUED
	 * 
	 * @return queue fill information or <code>null</code> if the system 
	 * does not provide this information or is not batch-queue based
	 */
	public Map<String,Integer>getQueueFill();
	
	/**
	 * get the remaining compute time budget for the given user
	 * 
	 * @return compute budget (one per project) or empty list if not applicable 
	 */
	public List<BudgetInfo> getComputeTimeBudget(Client client) throws ExecutionException;
	
	/**
	 * get (measured) mean time (seconds) for jobs in the queue(s)
	 */
	public long getMeanTimeQueued();

	/**
	 * on startup, initialize required data structures
	 * @param jobStore
	 */
	public default void initialise(IActionStore jobStore) throws Exception {}
}
