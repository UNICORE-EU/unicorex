package eu.unicore.xnjs.persistence;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import eu.unicore.persist.DataVersionException;
import eu.unicore.persist.Persist;
import eu.unicore.persist.PersistenceException;
import eu.unicore.persist.PersistenceFactory;
import eu.unicore.persist.PersistenceProperties;
import eu.unicore.persist.impl.PersistImpl;
import eu.unicore.persist.impl.PersistenceDescriptor;
import eu.unicore.util.Log;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.ems.Action;
import eu.unicore.xnjs.ems.ActionStatus;
import jakarta.inject.Inject;

/**
 * Persistence for actions based on JDBC
 * 
 * @author schuller
 */
public class JDBCActionStore extends AbstractActionStore {

	private Persist<Action>activeJobs;

	private Persist<DoneAction>doneJobs;

	@Inject
	private PersistenceProperties properties;
	
	@Inject
	private XNJS xnjs;
	
	//max time (seconds) to wait for a lock
	private int getForUpdateTimeoutPeriod=5; 

	@Override
	protected Action doGet(String id) throws Exception {
		Action a=null;
		DoneAction da=doneJobs.read(id);
		if(da!=null)a=da.getAction();
		if(a==null){
			a = activeJobs.read(id);
		}
		return a;
	}

	@Override
	protected Action doGetForUpdate(String id)throws Exception, TimeoutException {
		try{
			Action a=null;
			DoneAction da=null;
			da=doneJobs.getForUpdate(id, getForUpdateTimeoutPeriod, TimeUnit.SECONDS);
			if(da!=null)a=da.getAction();
			if(a!=null){
				return a;
			}
			else{
				a=activeJobs.getForUpdate(id, getForUpdateTimeoutPeriod, TimeUnit.SECONDS);
			}
			return a;
		}catch(InterruptedException te){
			throw new TimeoutException(te.getMessage());
		}
	}

	protected Action tryGetForUpdate(String id)throws Exception, TimeoutException {
		try{
			return activeJobs.tryGetForUpdate(id);
		}catch(InterruptedException te){
			throw new TimeoutException(te.getMessage());
		}
	}
	
	protected void start()throws Exception {
		PersistenceFactory pf = PersistenceFactory.get(properties);
		PersistenceDescriptor pd1 = PersistenceDescriptor.get(Action.class);
		if(!"1".equals(xnjs.getID())) {
			pd1.setTableName(pd1.getTableName()+"_"+xnjs.getID());
		}
		activeJobs = (PersistImpl<Action>)pf.getPersist(Action.class, pd1);
		checkVersion(activeJobs, "JOBS");
		PersistenceDescriptor pd2 = PersistenceDescriptor.get(DoneAction.class);
		if(!"1".equals(xnjs.getID())) {
			pd2.setTableName(pd2.getTableName()+"_"+xnjs.getID());
		}
		doneJobs = pf.getPersist(DoneAction.class, pd2);
		doneJobs.setLockSupport(((PersistImpl<Action>)activeJobs).getLockSupport());
	}

	/**
	 * check that the data from the given Persist instance can be used.
	 * If not, check system property and drop all data
	 * @param p - the persist impl to check
	 */
	protected void checkVersion(Persist<?> p, String tableName)throws Exception{
		Collection<String>ids=p.getIDs();
		try{
			if(ids.size()>0){
				p.read(ids.iterator().next());
			}
		}catch(DataVersionException v){
			//check if we really should delete all unreadable data
			if(!Boolean.getBoolean("unicore.update.force")){
				throw v;
			}
			logger.info("Removing unreadable data from table "+tableName);
			for(String id: ids){
				try{
					p.read(id);
				}
				catch(DataVersionException ex){
					try{
						p.remove(id);
					}catch(PersistenceException pe){
						Log.logException("Error removing "+id+" from table "+tableName, pe, logger);
					}
				}
			}
		}
	}

	@Override
	protected void doRemove(Action a)throws Exception {
		activeJobs.remove(a.getUUID());
		doneJobs.remove(a.getUUID());
	}

	@Override
	protected void doStore(Action action)throws Exception{
		if(action.isDirty()){
			if(action.getStatus()!=ActionStatus.DONE){
				activeJobs.write(action);
				if(action.getTransitionalStatus()==ActionStatus.TRANSITION_RESTARTING) {
					doneJobs.delete(action.getUUID());
				}
			}else{
				doneJobs.write(new DoneAction(action));
				activeJobs.delete(action.getUUID());
			}
		}
		else{
			activeJobs.unlock(action);
		}
	}

	@Override
	public Collection<String> getUniqueIDs()throws Exception  {
		Collection<String>all=activeJobs.getIDs();
		all.addAll(doneJobs.getIDs());
		return all;
	}

	@Override
	public Collection<String> getActiveUniqueIDs()throws Exception  {
		return activeJobs.getIDs();
	}

	@Override
	public int size()throws Exception {
		int all=activeJobs.getRowCount();
		all+=doneJobs.getRowCount();
		return all;
	}

	public String toString(){
		try{
			return super.toString()+"\n"+printDiagnostics();
		}
		catch(Exception e){
			return "N/A. An error occurred: ["+e.getClass().getName()+"] message: +"+e.getMessage();
		}
	}

	public void setTimeoutPeriod(int time){
		this.getForUpdateTimeoutPeriod=time;
	}

	public void removeAll()throws Exception{
		activeJobs.removeAll();
		doneJobs.removeAll();
	}

	public Persist<Action>getActiveJobsStorage(){
		return activeJobs;
	}

	public Persist<DoneAction>getDoneJobsStorage(){
		return doneJobs;
	}

}
