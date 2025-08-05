package eu.unicore.uas.impl.sms;

import eu.unicore.services.ExtendedResourceStatus;
import eu.unicore.services.InitParameters;
import eu.unicore.services.utils.AsyncCallback;
import eu.unicore.xnjs.ems.Action;
import eu.unicore.xnjs.ems.ActionStatus;

/**
 * A storage serving files from a job's working directory
 *
 * @author schuller
 */
public class UspaceStorageImpl extends SMSBaseImpl implements ExtendedResourceStatus {

	@Override
	public void initialise(InitParameters initobjs)throws Exception{
		initobjs.resourceState = ResourceStatus.INITIALIZING;
		super.initialise(initobjs);
		updateWorkdir();
	}

	// must update workdir since it is created asynchronously
	private void updateWorkdir()throws Exception{
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
		}catch(Exception e) {}
		return getModel().workdir;
	}

	private void persistChanges(final String workdir) {
		new AsyncCallback<UspaceStorageImpl>(getHome(), getUniqueID()) 
		{
			@Override
			public void callback(UspaceStorageImpl resource) {
				resource.getModel().setWorkdir(workdir);
				resource.setResourceStatus(ResourceStatus.READY);
			}
		}.submit();
	}

}
