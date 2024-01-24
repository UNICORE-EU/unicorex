package eu.unicore.uas.impl.sms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import eu.unicore.services.ExtendedResourceStatus.ResourceStatus;
import eu.unicore.services.Home;
import eu.unicore.services.Kernel;
import eu.unicore.services.events.AsynchActionWithCallback;
import eu.unicore.uas.UAS;
import eu.unicore.uas.util.LogUtil;


/**
 * after server restart, this class fixes any inconsistencies in the list of
 * SMS IDs and SMS owners in the StorageFactory
 * 
 * @author schuller
 */
public class ConsolidateStorageFactoryInstance extends AsynchActionWithCallback<StorageFactoryImpl> {

	private static final Logger logger = LogUtil.getLogger(LogUtil.DATA,ConsolidateStorageFactoryInstance.class);
	
	private final UpdateSMSLists updateTask;
	
	public ConsolidateStorageFactoryInstance(UpdateSMSLists updateTask, Kernel kernel, String resourceID) {
		super(updateTask, kernel.getHome(UAS.SMF), resourceID);
		this.updateTask = updateTask;
	}

	@Override
	public void taskFinished(StorageFactoryImpl resource) {
		Map<String,String> owners = updateTask.getSMSOwners();
		List<String>deletedSMS = updateTask.getDeletedSMS();
		SMFModel smfModel = resource.getModel();
		for(String id: deletedSMS){
			smfModel.removeChild(id);
		}
		smfModel.getSmsOwners().putAll(owners);
		smfModel.getSmsIDs().addAll(owners.keySet());
		
		resource.setStatusMessage("OK");
		resource.setResourceStatus(ResourceStatus.READY);
	}

	@Override
	public void taskFailed(StorageFactoryImpl resource, RuntimeException error) {
		logger.error("Could not consolidate storage factory data",error);
	}

	public static final class UpdateSMSLists implements Runnable{
		
		private final Kernel kernel;
		private final String resourceID;
		private final Map<String,String>smsOwners=new HashMap<String, String>();
		private final List<String>deletedSMSs=new ArrayList<String>();
		
		public UpdateSMSLists(Kernel kernel,String resourceID){
			this.kernel = kernel;
			this.resourceID=resourceID;
		}
		
		public Map<String,String>getSMSOwners(){
			return smsOwners;
		}
		

		public List<String>getDeletedSMS(){
			return deletedSMSs;
		}
		
		public void run(){
			try{
				logger.info("Refreshing StorageFactory service instance <"+resourceID+">");
				// find all SMSs belonging to this factory
				Home h = kernel.getHome(UAS.SMS);
				if(h == null)throw new IllegalStateException("SMS Home not available!");
				Collection<String>currentSMSIDs=h.getStore().getUniqueIDs();
				
				Home smfHome = kernel.getHome(UAS.SMF);
				if(smfHome == null)throw new IllegalStateException("StorageFactory Home not available!");
				SMFModel smfModel = (SMFModel)smfHome.get(resourceID).getModel(); 
				
				// check for deleted SMSs that are still in the SMF list
				for(String id: smfModel.getSmsIDs()){
					if(!currentSMSIDs.contains(id)){
						deletedSMSs.add(id);
					}
				}
				
				// now check for SMSs that belong to us
				for(String id: currentSMSIDs){
					SMSBaseImpl sms = (SMSBaseImpl)h.get(id);
					if(sms.getModel()!=null && resourceID.equals(sms.getModel().getParentUID())){
						smsOwners.put(id,sms.getOwner());
					}
				}
			}catch(Exception ex){
				throw new RuntimeException(ex);
			}
		}
	}
}
