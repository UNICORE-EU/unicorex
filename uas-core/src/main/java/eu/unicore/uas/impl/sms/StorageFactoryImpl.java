package eu.unicore.uas.impl.sms;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import eu.unicore.services.Home;
import eu.unicore.services.InitParameters;
import eu.unicore.services.InitParameters.TerminationMode;
import eu.unicore.services.exceptions.ResourceUnknownException;
import eu.unicore.services.messaging.PullPoint;
import eu.unicore.uas.SMSProperties;
import eu.unicore.uas.UAS;
import eu.unicore.uas.impl.BaseResourceImpl;
import eu.unicore.uas.impl.sms.StorageManagementHomeImpl.StorageTypes;
import eu.unicore.uas.util.LogUtil;

/**
 * Implements the storage factory
 * 
 * @author schuller
 * @author daivandy
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
			model = new SMFModel();
		}
		super.initialise(initParams);
		logger.info("Storage factory <{}> created", getUniqueID());
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
		String clientName = (getClient()!=null?getClient().getDistinguishedName():"<no client>");
		Map<String, StorageDescription> factories = uasProperties.getStorageFactories();
		if(storageBackendType == null){
			List<String>types = new ArrayList<>();
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

		factoryDesc = factoryDesc.clone();
		if (name != null) {
			factoryDesc.setName(name);
		}
		// handle user-specified 'path'
		boolean appendUniqueID = true;
		String pathSpec = parameters.get("path");
		if (pathSpec!=null) {
			if(factoryDesc.isAllowUserdefinedPath()) {
				factoryDesc.setPathSpec(pathSpec);
				appendUniqueID = false;
				// do *not* delete this dir on destroy
				factoryDesc.setCleanup(false);
				if("DEFAULT".equals(storageBackendType))
				{
					if(pathSpec.contains("$")) {
						factoryDesc.setStorageType(StorageTypes.VARIABLE);
					}
				else {
					factoryDesc.setStorageType(StorageTypes.FIXEDPATH);
					}
				}
			}
			else {
				throw new IllegalArgumentException("Setting the 'path' is not allowed!");
			}
		}

		StorageInitParameters initMap = tt!=null? 
				new StorageInitParameters(null,tt) : new StorageInitParameters(null,TerminationMode.DEFAULT);
		initMap.appendUniqueID = appendUniqueID;	
		initMap.storageDescription = factoryDesc;
		initMap.factoryID = getUniqueID();

		if(parameters.get(SMSProperties.ENABLE_TRIGGER)!=null){
			factoryDesc.setEnableTrigger(Boolean.parseBoolean(parameters.get(SMSProperties.ENABLE_TRIGGER)));
			if(factoryDesc.isEnableTrigger() && !factoryDesc.isAllowTrigger()) {
				throw new IllegalArgumentException("Enabling the triggering feature is not allowed!");
			}
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
		initMap.userParameters.putAll(parameters);
		String smsID = createStorageResource(initMap);
		initStorage(smsID);
		logger.info("Created new <{}> StorageManagement resource <{}> for <{}>", storageBackendType, smsID, clientName);
		return smsID;
	}

	protected String createStorageResource(StorageInitParameters initParam)
			throws Exception{
		Home home = kernel.getHome(UAS.SMS);
		if(home==null){
			throw new ResourceUnknownException("Storage management service is not deployed.");
		}
		return home.createResource(initParam);
	}

	protected void initStorage(String uniqueID)throws Exception{
		StorageManagementHomeImpl smsHome = (StorageManagementHomeImpl)kernel.getHome(UAS.SMS);
		SMSBaseImpl sms=(SMSBaseImpl)smsHome.get(uniqueID);
		sms.getStorageAdapter().mkdir("/");
	}

	@Override
	public void processMessages(PullPoint p){
		//check for deleted SMSs and remove them...
		try{
			while(p.hasNext()){
				String m = (String)p.next().getBody();
				if(m.startsWith("deleted:")){
					String id = m.substring(m.indexOf(":")+1);
					logger.debug("Removing Storage with ID <{}>", id);
					getModel().removeChild(id);
				}
			}
		}catch(Exception e){
			LogUtil.logException(e.getMessage(),e,logger);
		}
	}

}
