package eu.unicore.xnjs.persistence;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.Logger;

import eu.unicore.persist.PersistenceException;
import eu.unicore.util.Log;
import eu.unicore.xnjs.ems.Action;
import eu.unicore.xnjs.ems.ActionStatus;
import eu.unicore.xnjs.util.LogUtil;

/**
 * Stores actions in some storage back-end.
 * 
 * @author schuller
 */
public abstract class AbstractActionStore implements IActionStore{

	protected static final Logger logger=LogUtil.getLogger(LogUtil.PERSISTENCE,AbstractActionStore.class);	

	/**
	 * interval (seconds) in which to refill the work queue from the database
	 * default: 10
	 */
	public static final String QUEUE_REFILL_INTERVAL="xnjs.queue.refill.delay";

	private static final AtomicInteger idGenerator=new AtomicInteger(0);

	protected String id = String.valueOf(idGenerator.incrementAndGet());

	protected String name;

	protected final Map<String,Integer>states = new ConcurrentHashMap<>();
	
	public void setName(String name){
		this.name=name;
	}
	
	public String getName(){
		return name;
	}
	
	/**
	 * removes all actions from the persistence DB
	 *
	 * @throws PersistenceException
	 * @throws TimeoutException
	 */
	public void doCleanup()throws Exception, TimeoutException{
		for(String key: getUniqueIDs()){
			Action a=get(key);
			if(a!=null)remove(a);
		}
	}

	public Action get(String key) throws Exception{
		return doGet(key);
	}

	public Action getForUpdate(String key) throws TimeoutException, Exception{
		Action a=doGetForUpdate(key);
		if(a!=null){
			states.put(key, a.getStatus());
			a.setWaiting(false);
			logger.debug("GET FOR UPDATE {}", key);
		}
		return a;
	}

	/**
	 * Attempts to lock and get an ACTIVE action. 
	 * Returns <code>null</code> immediately if the action cannot be locked
	 * 
	 * @param id
	 * 
	 * @throws PersistenceException
	 * @throws TimeoutException
	 */
	protected abstract Action tryGetForUpdate(String id)throws Exception,TimeoutException;

	public int getTotalActionsInStore(){
		try{
			return size();
		}catch(Exception pe){
			return -1;
		}
	}

	public String printStorageOverview(){
		return toString();
	}

	public void put(String key, Action value)throws Exception{
		doStore(value);
		states.put(key,value.getStatus());
	}

	public void remove(Action a)throws Exception{
		states.remove(a.getUUID());
		doRemove(a);
	}

	public abstract int size() throws Exception;

	public int size(int status) throws Exception{
		int i=0;
		for(Integer s: states.values()){
			if(s.intValue()==status)i++;
		}
		return i;
	}

	/**
	 * get the unique IDs of active actions (i.e. where status is not DONE)
	 * @throws PersistenceException
	 */
	public abstract Collection<String> getActiveUniqueIDs() throws Exception;

	/**
	 * store a DAO in the backend store. this method is responsible for checking the
	 * "dirty" status
	 */
	protected abstract void doStore(Action action) throws Exception;

	/**
	 * get a DAO from the backend store
	 */
	protected abstract Action doGet(String id) throws Exception;

	/**
	 * get a DAO from the backend store, aquiring a write lock
	 */
	protected abstract Action doGetForUpdate(String id) throws Exception, TimeoutException;

	/**
	 * delete a DAO in the backend store
	 */
	protected abstract void doRemove(Action a) throws Exception;

	public String printDiagnostics() {
		StringBuilder sb=new StringBuilder();
		long start=System.currentTimeMillis();
		String newline=System.getProperty("line.separator");
		sb.append("DIAGONSTIC INFO storage <"+name+"."+id+">"+newline);
		sb.append(newline);
		try {
			sb.append("Entries in database: "+getUniqueIDs().size()+newline);
			sb.append("DONE: "+size(ActionStatus.DONE)+newline);
			sb.append("RUNNING: "+size(ActionStatus.RUNNING)+newline);
			sb.append("READY: "+size(ActionStatus.READY)+newline);
			sb.append("PENDING: "+size(ActionStatus.PENDING)+newline);
			sb.append("QUEUED: "+size(ActionStatus.QUEUED)+newline);
			sb.append("PREPROCESSING: "+size(ActionStatus.PREPROCESSING)+newline);
			sb.append("POSTPROCESSING: "+size(ActionStatus.POSTPROCESSING)+newline);
		}catch(Exception e) {
			sb.append("ERROR: "+Log.createFaultMessage("", e));
		}
		long time=System.currentTimeMillis()-start;
		sb.append("Implementation: "+getClass().getName()+newline);
		sb.append("Time to generate diagnostic info: "+time+" ms."+newline);
		return sb.toString();
	}

}
