package eu.unicore.uas.impl.sms;

import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;

import eu.unicore.services.Home;
import eu.unicore.services.InitParameters;
import eu.unicore.services.Resource;
import eu.unicore.util.Log;
import eu.unicore.xnjs.ems.Action;
import eu.unicore.xnjs.ems.ActionStatus;
import eu.unicore.xnjs.ems.ExecutionException;

/**
 * A storage serving files from a job's working directory
 *
 * @author schuller
 */
public class UspaceStorageImpl extends SMSBaseImpl {

	@Override
	public void initialise(InitParameters initobjs)throws Exception{
		initobjs.resourceState = ResourceStatus.INITIALIZING;
		super.initialise(initobjs);
		updateWorkdir();
	}

	// must update workdir since it is created asynchronously
	private void updateWorkdir()throws ExecutionException{
		if(getModel().workdir==null){
			SMSModel m = getModel();
			String actionId = m.storageDescription.getPathSpec();
			Action a = getXNJSFacade().getAction(actionId);
			String workdir = a.getExecutionContext().getWorkingDirectory();
			m.workdir = workdir;
			if(workdir!=null) {
				setResourceStatus(ResourceStatus.READY);
				persistChanges(workdir);
			}
			else if (a.getStatus()==ActionStatus.DONE){
				setResourceStatus(ResourceStatus.ERROR);
				persistChanges(workdir);
			}
		}
	}

	@Override
	public ResourceStatus getResourceStatus() {
		try{
			updateWorkdir();
		}catch(Exception e) {}
		return super.getResourceStatus();
	}

	@Override
	public String getStorageRoot() {
		try{
			updateWorkdir();
		}catch(ExecutionException e) {}
		return getModel().workdir;
	}
	
	private void persistChanges(final String workdir) {
		new AsynchCallback<UspaceStorageImpl>(getHome(), getUniqueID()) 
		{
			@Override
			public void callback(UspaceStorageImpl resource) {
				resource.getModel().setWorkdir(workdir);
				resource.setResourceStatus(ResourceStatus.READY);
			}
		}.submit();
	}

	// TODO generic class from USE 5.1.0
	public static abstract class AsynchCallback<T extends Resource> implements Runnable{
		private final static Logger log=Log.getLogger(Log.UNICORE, AsynchCallback.class);

		private final Home home;
		private final String resourceID;
		private final int delay;

		public AsynchCallback(Home home, String resourceID, int delay){
			this.home=home;
			this.resourceID=resourceID;
			this.delay = 200;
		}

		public AsynchCallback(Home home, String resourceID){
			this(home, resourceID, 200);
		}

		@SuppressWarnings("unchecked")
		public void run(){
			try(T resource = (T)home.getForUpdate(resourceID)){
				callback(resource);
			}
			catch(Exception ex){
				Log.logException("Error", ex, log);
			}
		}

		/**
		 * perform callback
		 * @param resource - the resource
		 */
		public abstract void callback(T resource);

		public void submit() {
			home.getKernel().getContainerProperties().getThreadingServices().
			getScheduledExecutorService().schedule(this, delay, TimeUnit.MILLISECONDS);
		}
	}
}
