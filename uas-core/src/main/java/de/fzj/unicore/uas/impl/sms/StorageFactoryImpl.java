package de.fzj.unicore.uas.impl.sms;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.xml.namespace.QName;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.uas.SMSProperties;
import de.fzj.unicore.uas.StorageFactory;
import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.impl.BaseInitParameters;
import de.fzj.unicore.uas.impl.BaseResourceImpl;
import de.fzj.unicore.uas.impl.enumeration.EnumerationInitParameters;
import de.fzj.unicore.uas.impl.sms.ConsolidateStorageFactoryInstance.UpdateSMSLists;
import de.fzj.unicore.uas.util.LogUtil;
import eu.unicore.services.Home;
import eu.unicore.services.InitParameters;
import eu.unicore.services.InitParameters.TerminationMode;
import eu.unicore.services.exceptions.ResourceUnknownException;
import eu.unicore.services.messaging.PullPoint;

/**
 * Implements the storage factory
 * 
 * @author schuller
 * @author daivandy
 * @since 6.3
 */
public class StorageFactoryImpl extends BaseResourceImpl {

	private static final Logger logger = LogUtil.getLogger(LogUtil.DATA, StorageFactoryImpl.class);

	public StorageFactoryImpl(){
		super();
	}

	@Override
	public SMFModel getModel(){
		return (SMFModel)model;
	}

	@Override
	public void initialise(InitParameters initParams)
			throws Exception {
		if(model==null){
			model=new SMFModel();
		}
		SMFModel model = getModel();
		initParams.resourceState = ResourceStatus.INITIALIZING;
		super.initialise(initParams);
		
		model.accessibleSMSEnumerationID = createEnumeration(StorageFactory.RPAccessibleSMSReferences);
		model.setXnjsReference(((BaseInitParameters)initParams).xnjsReference);
		logger.info("Storage factory <"+getUniqueID()+"> created");
		setStatusMessage("OK");

		UpdateSMSLists task = new UpdateSMSLists(kernel, getUniqueID());
		kernel.getContainerProperties().getThreadingServices().getScheduledExecutorService().schedule(
				new ConsolidateStorageFactoryInstance(task, kernel, getUniqueID()), 
				5000, TimeUnit.MILLISECONDS);
	}

	/**
	 * get the owner for a given sms
	 * @param smsID
	 * @return owner DN
	 */
	public String getOwnerForSMS(String smsID){
		return getModel().smsOwners.get(smsID);
	}


	/**
	 * create a new SMS resource
	 * @param storageBackendType - the type of backend. If null, the default one will be chosen, if possible.
	 * @param name - (optional) name of the new storage
	 * @param tt - (optional) termination time
	 * @param parameters - any settings for the new storage
	 * @return the unique ID of the new resource
	 * @throws Exception
	 */
	public String createSMS(String storageBackendType, String name, Calendar tt, Map<String,String>parameters)
	throws Exception 
	{
		String clientName=(getClient()!=null?getClient().getDistinguishedName():"<no client>");
		Map<String, StorageDescription> factories = uasProperties.getStorageFactories();

		//choose default backend type. If there is only one, or if there is one
		//named "DEFAULT", it is used. Otherwise, an exception is thrown.
		if(storageBackendType == null){
			List<String>types=new ArrayList<String>();
			types.addAll(factories.keySet());
			if(types.contains("DEFAULT")){
				storageBackendType="DEFAULT";
			}
			else if(types.size()==1){
				storageBackendType=types.get(0);
			}
			else{
				throw new IllegalArgumentException("Please specify the storage backend type. Available are "+types);
			}
		}
		
		StorageDescription factoryDesc = factories.get(storageBackendType);
		if (factoryDesc == null)
			throw new IllegalArgumentException("Unknown type of storage factory: " + storageBackendType);

		//we don't want to mess the global configuration of SMSFies
		factoryDesc = factoryDesc.clone();

		// we allow clients to overwrite the name 
		if (name != null) {
			factoryDesc.setName(name);
		}

		boolean appendUniqueID = true;
		if (factoryDesc.isAllowUserdefinedPath()) {
			// we allow clients to overwrite the path
			String pathSpec = parameters.get("path");
			if(pathSpec!=null){
				factoryDesc.setPathSpec(pathSpec);
				appendUniqueID = false;
				// and do *not* delete this dir on destroy
				factoryDesc.setCleanup(false);
			}
			else {
				// might even be mandatory...
				if(factoryDesc.getPathSpec() == null){
					throw new IllegalArgumentException("No 'path' parameter was given!");
				}
			}
		}

		StorageInitParameters initMap = tt!=null? 
				new StorageInitParameters(null,tt) : new StorageInitParameters(null,TerminationMode.DEFAULT);
				
		// use configured base and append the unique ID
		initMap.appendUniqueID = appendUniqueID;	
		initMap.storageDescription = factoryDesc;
		initMap.factoryID = getUniqueID();
		
		// allow users to set/override some of the parameters
		if(parameters.get(SMSProperties.ENABLE_TRIGGER)!=null){
			factoryDesc.setEnableTrigger(Boolean.parseBoolean(parameters.get(SMSProperties.ENABLE_TRIGGER)));
		}
		if(parameters.get(SMSProperties.DISABLE_METADATA)!=null){
			factoryDesc.setDisableMetadata(Boolean.parseBoolean(parameters.get(SMSProperties.DISABLE_METADATA)));
		}
		if(parameters.get(SMSProperties.DESCRIPTION)!=null){
			factoryDesc.setDescription(parameters.get(SMSProperties.DESCRIPTION));
		}
		if(parameters.get(SMSProperties.UMASK_KEY)!=null){
			factoryDesc.setDefaultUmask(parameters.get(SMSProperties.UMASK_KEY));
		}

		// put user parameters into init map, let implementation decide which one to use
		// and which default ones can be overwritten
		initMap.userParameters.putAll(parameters);
		String smsID=createStorageResource(initMap);

		initStorage(smsID);
		logger.info("Created new StorageManagement resource <"+smsID+"> for "+clientName);
		getModel().smsOwners.put(smsID, clientName);
		getModel().getSmsIDs().add(smsID);
		return smsID;
	}

	/**
	 * @return the UID of the new Enumeration
	 */
	protected String createEnumeration(QName rp)throws Exception{
		EnumerationInitParameters init = new EnumerationInitParameters(null, TerminationMode.NEVER);
		init.parentUUID = getUniqueID();
		init.parentServiceName = getServiceName();
		init.targetServiceRP = rp;
		Home h=kernel.getHome(UAS.ENUMERATION);
		if(h==null)throw new Exception("Enumeration service is not deployed!");
		return h.createResource(init);
	}


	/**
	 * create new storage management service and return its epr
	 */
	protected String createStorageResource(StorageInitParameters initParam)
			throws Exception{
		Home home=kernel.getHome(UAS.SMS);
		if(home==null){
			throw new ResourceUnknownException("Storage management service is not deployed.");
		}
		return home.createResource(initParam);
	}

	protected void initStorage(String uniqueID)throws Exception{
		StorageManagementHomeImpl smsHome=(StorageManagementHomeImpl)kernel.getHome(UAS.SMS);
		SMSBaseImpl sms=(SMSBaseImpl)smsHome.get(uniqueID);
		sms.getStorageAdapter().mkdir("/");
	}

	@Override
	public void processMessages(PullPoint p){
		//check for deleted SMSs and remove them...
		try{
			while(p.hasNext()){
				String m=(String)p.next().getBody();
				if(m.startsWith("deleted:")){
					String id=m.substring(m.indexOf(":")+1);
					logger.debug("Removing Storage with ID "+id+"...");
					getModel().removeChild(id);
				}
			}
		}catch(Exception e){
			LogUtil.logException(e.getMessage(),e,logger);
		}
	}

}
