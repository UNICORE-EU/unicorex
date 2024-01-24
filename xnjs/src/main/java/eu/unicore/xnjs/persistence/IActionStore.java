package eu.unicore.xnjs.persistence;

import java.util.Collection;
import java.util.concurrent.TimeoutException;

import eu.unicore.persist.PersistenceException;
import eu.unicore.xnjs.ems.Action;

/**
 * Interface to the action storage<br/>
 * Also maintains the XNJS work queue, i.e. the set of those actions that are currently
 * being processed
 * 
 * @author schuller
 */
public interface IActionStore {
	
	/**
	 * property key, if set to true persistence layer will 
	 * clear everything on startup 
	 */
	public static final String CLEAR_ON_STARTUP="de.fzj.unicore.xnjs.persistence.clearAllOnStartup";
	
	/**
	 * set the ID of this store
	 * 
	 * @param id
	 */
	public void setName(String id);
	
	
	/**
	 * retrieve an action
	 * 
	 * @param uuid - the action's unique ID
	 */
	public Action get(String uuid) throws Exception;
	
	/**
	 * Retrieve an action and aquire the right for 
	 * making persistent changes to it<br/>
	 * 
	 * This method must be matched with a corresponding call to put()
	 * 
	 * @param uuid
	 * 
	 * @return action
	 * @throws TimeoutException
	 * @throws PersistenceException 
	 */
	public Action getForUpdate(String uuid) throws TimeoutException, Exception;
	
	/**
	 * store an action but don't add it to the work queue
	 * @param key
	 * @param value
	 * @throws PersistenceException
	 */
	public void put(String key, Action value) throws Exception;
	
	/**
	 * remove an action
	 * @param action - the {@link Action} to remove
	 */
	public void remove(Action action) throws Exception,TimeoutException;

	/**
	 * get all IDs
	 */
	public Collection<String> getUniqueIDs() throws Exception;
	
	/**
	 * get all IDs where the action status is not DONE
	 */
	public Collection<String> getActiveUniqueIDs() throws Exception;
	
	/**
	 * get the total number of entries
	 */
	public int size() throws Exception;
	
	/**
	 * get the total number of entries with the given status
	 */
	public int size(int actionStatus) throws Exception;
	
	/**
	 * print some information about this store
	 * 
	 * @return String with diagnostic info
	 */
	public String printDiagnostics();
	
	/**
	 * delete everything
	 * @throws Exception 
	 */
	public void removeAll() throws Exception;
}