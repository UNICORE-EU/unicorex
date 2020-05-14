package de.fzj.unicore.uas.impl.sms;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.oasisOpen.docs.wsrf.rl2.DestroyDocument;
import org.oasisOpen.docs.wsrf.rl2.DestroyResponseDocument;
import org.oasisOpen.docs.wsrf.rl2.SetTerminationTimeDocument;
import org.oasisOpen.docs.wsrf.rl2.SetTerminationTimeResponseDocument;
import org.unigrids.services.atomic.types.PropertyType;
import org.unigrids.x2006.x04.services.smf.CreateSMSDocument;
import org.unigrids.x2006.x04.services.smf.CreateSMSResponseDocument;
import org.unigrids.x2006.x04.services.smf.StorageBackendParametersDocument.StorageBackendParameters;
import org.unigrids.x2006.x04.services.smf.StorageDescriptionType;
import org.unigrids.x2006.x04.services.smf.StorageFactoryPropertiesDocument;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.SMSProperties;
import de.fzj.unicore.uas.StorageFactory;
import de.fzj.unicore.uas.StorageManagement;
import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.impl.BaseInitParameters;
import de.fzj.unicore.uas.impl.UASWSResourceImpl;
import de.fzj.unicore.uas.impl.enumeration.EnumerationInitParameters;
import de.fzj.unicore.uas.impl.sms.ConsolidateStorageFactoryInstance.UpdateSMSLists;
import de.fzj.unicore.uas.util.LogUtil;
import de.fzj.unicore.wsrflite.Home;
import de.fzj.unicore.wsrflite.InitParameters;
import de.fzj.unicore.wsrflite.InitParameters.TerminationMode;
import de.fzj.unicore.wsrflite.exceptions.ResourceUnknownException;
import de.fzj.unicore.wsrflite.messaging.PullPoint;
import de.fzj.unicore.wsrflite.xmlbeans.BaseFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.ResourceNotDestroyedFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.ResourceUnavailableFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.ResourceUnknownFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.TerminationTimeChangeRejectedFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.UnableToSetTerminationTimeFault;
import de.fzj.unicore.wsrflite.xmlbeans.renderers.AddressRenderer;
import eu.unicore.services.ws.utils.WSServerUtilities;

/**
 * Implements the storage factory
 * 
 * @author schuller
 * @author daivandy
 * @since 6.3
 */
public class StorageFactoryImpl extends UASWSResourceImpl implements StorageFactory{

	private static final Logger logger = LogUtil.getLogger(LogUtil.DATA, StorageFactoryImpl.class);

	public StorageFactoryImpl(){
		super();
		addRenderer(new AddressRenderer(this,RPAccessibleSMSEnumeration,false) {
			@Override
			protected String getServiceSpec() {
				return UAS.ENUMERATION+"?res="+getModel().accessibleSMSEnumerationID;
			}
		});
		addRenderer(new StorageDescriptionRP(this));

		//internal use
		addRenderer(new AccessibleSMSReferenceRP(this));
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
		
		model.accessibleSMSEnumerationID=createEnumeration(RPAccessibleSMSReferences);
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

	public CreateSMSResponseDocument CreateSMS(CreateSMSDocument in) throws BaseFault {
		if(logger.isTraceEnabled())logger.trace("CreateSMS: "+in);
		try{
			//extra settings for the to-be-created SMS
			Map<String,String>extraProperties=new HashMap<String,String>();

			String storageBackendType = null;
			String storageName = null;

			// extract requested back-end type and parameters, if any
			StorageDescriptionType sdType = in.getCreateSMS().getStorageDescription();
			if(sdType!=null) {
				if(sdType.getStorageBackendType()!=null){
					storageBackendType = sdType.getStorageBackendType();
				}
				if(sdType.getStorageBackendParameters()!=null){
					extraProperties.putAll(getAdditionClientConfigurationItems(
							sdType.getStorageBackendParameters()));
				}
				storageName = sdType.getFileSystem()!=null ? sdType.getFileSystem().getName() : null; 
			}
			Calendar tt = null;
			if(in.getCreateSMS().getTerminationTime() != null){
				tt = in.getCreateSMS().getTerminationTime().getCalendarValue();
			}
			
			String smsID=createSMS(storageBackendType,storageName,tt,extraProperties);
			EndpointReferenceType epr=WSServerUtilities.makeEPR(UAS.SMS, smsID, StorageManagement.SMS_PORT, true, kernel);
			CreateSMSResponseDocument resD=CreateSMSResponseDocument.Factory.newInstance();
			resD.addNewCreateSMSResponse().setSmsReference(epr);
			return resD;
			
		}catch(Exception ex){
			String clientName=(getClient()!=null?getClient().getDistinguishedName():"N/A");
			LogUtil.logException("Could not create Storage instance for <"+clientName+">", ex, logger);
			throw BaseFault.createFault("Could not create Storage instance. Please consult the site administrator.", ex);
		}
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
	 * replaces prefix of the storage factory into a normal one of SMS
	 * @param params - parameters provided by the client
	 */
	private Map<String, String> getAdditionClientConfigurationItems(StorageBackendParameters params) {
		Map<String, String> ret = new HashMap<String, String>();
		for(PropertyType p: params.getPropertyArray()){
			ret.put(p.getName(), p.getValue());
		}
		return ret;
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
	public QName getPortType() {
		return SMF_PORT;
	}

	@Override
	public QName getResourcePropertyDocumentQName() {
		return StorageFactoryPropertiesDocument.type.getDocumentElementName();
	}

	@Override
	public DestroyResponseDocument Destroy(DestroyDocument in) throws ResourceNotDestroyedFault, ResourceUnknownFault, ResourceUnavailableFault {
		throw ResourceNotDestroyedFault.createFault("Not destroyed."); 
	}

	@Override
	public SetTerminationTimeResponseDocument SetTerminationTime(
			SetTerminationTimeDocument in)
					throws UnableToSetTerminationTimeFault,
					TerminationTimeChangeRejectedFault, ResourceUnknownFault,
					ResourceUnavailableFault {
		throw TerminationTimeChangeRejectedFault.createFault("Not changed.");
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
