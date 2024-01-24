package eu.unicore.xnjs.ems;

import java.util.Collection;

import eu.unicore.security.Client;

/**
 * The frontend EMS interface<br>
 * It serves as the liason to the front-controller 
 * that can add, pause, etc actions<br>
 * 
 * @author schuller
 */
public interface Manager {

	public void start() throws Exception;
	
	public void stop();
	
	/**
	 * retrieve an action by id.
	 * @param id
	 * @return Action or null if not found
	 */
	public Action getAction(String id) throws Exception;
	
	/**
	 * add a new job into the EMS on behalf of the client
	 * @param job
	 * @param client
	 * @return job id for querying
	 * 
	 * @throws ExecutionException
	 */
	public Object add(Action job, Client client) throws Exception;

	/** 
	 * run an Action that is ready
	 * 
	 * @param id the ID of the action to run
	 * @param client
	 * @return status 
	 * @throws ExecutionException
	 */
	public Object run(String id, Client client) throws Exception;

	/**
	 * get the status of the Action identified by id
	 * 
	 * @see ActionStatus
	 * 
	 * @param id Which action
	 * @param client Who wants the status?
	 * @return A status object
	 * @throws ExecutionException
	 */
	public Integer getStatus(String id, Client client) throws Exception;
	
	/**
	 * pause a running Action
	 * 
	 * @param id Which action
	 * @param client Who
	 * @return A status object
	 * @throws ExecutionException
	 */
	public Object pause(String id, Client client) throws Exception;
	
	/** 
	 * resume an Action that has been paused
	 * 
	 * @param id Which action
	 * @param client Who
	 * @return A status object
	 * @throws ExecutionException
	 */
	public Object resume(String id, Client client) throws Exception;
	
	/** 
	 * abort a running/paused action
	 * 
	 * @param id Which action
	 * @param client Who
	 * @return A status object
	 * @throws ExecutionException
	 */
	public Object abort(String id, Client client) throws Exception;

	
	/** 
	 * restart a done action (without any stage-ins!): status will be set back to "PENDING"
	 * 
	 * @param id Which action
	 * @param client Who
	 * @return A status object
	 * @throws ExecutionException
	 */
	public Object restart(String id, Client client) throws Exception;

	/**
	 * destroys an action and cleans up 
	 * 
	 * @param job -  the id of the job to destroy
	 * @param client - the client
	 * 
	 * @throws ExecutionException
	 */
	public void destroy(String job, Client client) throws Exception;

	
	/**
	 * list all jobs the client may see
	 *  
	 * @param client
	 * @return list of job UUIDs
	 * @throws ExecutionException
	 */
	public Collection<String> list(Client client) throws Exception;
}
