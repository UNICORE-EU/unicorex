package eu.unicore.uas.impl.tss;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import eu.unicore.security.Client;
import eu.unicore.services.Home;
import eu.unicore.services.InitParameters;
import eu.unicore.services.InitParameters.TerminationMode;
import eu.unicore.services.exceptions.ResourceNotCreatedException;
import eu.unicore.services.exceptions.ResourceUnknownException;
import eu.unicore.services.exceptions.TerminationTimeChangeRejectedException;
import eu.unicore.services.exceptions.UnableToSetTerminationTimeException;
import eu.unicore.services.messaging.Message;
import eu.unicore.services.messaging.PullPoint;
import eu.unicore.services.messaging.ResourceAddedMessage;
import eu.unicore.services.messaging.ResourceDeletedMessage;
import eu.unicore.services.utils.Utilities;
import eu.unicore.uas.UAS;
import eu.unicore.uas.UASProperties;
import eu.unicore.uas.impl.BaseResourceImpl;
import eu.unicore.uas.impl.UmaskSupport;
import eu.unicore.uas.impl.job.JobInitParameters;
import eu.unicore.uas.impl.reservation.ReservationInitParameters;
import eu.unicore.uas.impl.sms.SMSBaseImpl;
import eu.unicore.uas.impl.sms.StorageDescription;
import eu.unicore.uas.impl.sms.StorageInitParameters;
import eu.unicore.uas.impl.sms.StorageManagementHomeImpl.StorageTypes;
import eu.unicore.uas.impl.tss.util.GenerateJMSInstances;
import eu.unicore.uas.impl.tss.util.RecreateJMSReferenceList;
import eu.unicore.uas.impl.tss.util.RecreateReservationReferenceList;
import eu.unicore.uas.impl.tss.util.RecreateXNJSJobs;
import eu.unicore.uas.impl.tss.util.TSSAsynchInitialisation;
import eu.unicore.uas.util.LogUtil;
import eu.unicore.uas.xnjs.XNJSFacade;
import eu.unicore.util.Log;
import eu.unicore.xnjs.ems.Action;
import eu.unicore.xnjs.json.JSONParser;
import eu.unicore.xnjs.tsi.remote.TSIMessages;

/**
 * TargetSystem business logic code<br/>
 * 
 * It uses an XNJS instance as back end, see {@link XNJSFacade}
 * 
 * @author schuller
 */
public class TargetSystemImpl extends BaseResourceImpl implements UmaskSupport {

	private static final Logger logger = LogUtil.getLogger(LogUtil.JOBS, TargetSystemImpl.class);

	public TargetSystemImpl(){
		super();
	}

	@Override
	public TSSModel getModel(){
		return (TSSModel)model;
	}

	@Override
	public void processMessages(PullPoint p){
		//check for deleted jobs/reservations and remove them
		try{
			while(p.hasNext()){
				Message message=p.next();
				if(message instanceof ResourceDeletedMessage){
					ResourceDeletedMessage rdm = (ResourceDeletedMessage)message;
					String id = rdm.getDeletedResource();
					String service = rdm.getServiceName();
					if(UAS.JMS.equals(service)){
						getModel().getJobIDs().remove(id);
					}
					else if(UAS.RESERVATIONS.equals(service)){
						getModel().getReservationIDs().remove(id);
					}
				}
				else if (message instanceof ResourceAddedMessage) {
					ResourceAddedMessage ram = (ResourceAddedMessage)message;
					String id = ram.getAddedResource();
					String service = ram.getServiceName();
					if(UAS.JMS.equals(service)){
						getModel().getJobIDs().add(id);
					}
					else if(UAS.RESERVATIONS.equals(service)){
						getModel().getReservationIDs().add(id);
					}
				}
			}
		}catch(Exception e){
			LogUtil.logException(e.getMessage(),e,logger);
		}
	}
	
	public String submit(JSONObject job, boolean autoStartWhenReady, Calendar tt,
			String... tags) throws Exception {
		if(tt!=null){
			checkAndExtendLT(tt);
		}
		String xnjsReference = getModel().getXnjsReference();
		Action action = XNJSFacade.get(xnjsReference, kernel).makeAction(job);
		String umask = JSONParser.parseUmask(job);
		if(umask == null)umask = getUmask();
		action.setUmask(umask);
		if(autoStartWhenReady){
			action.getProcessingContext().put(Action.AUTO_SUBMIT, Boolean.TRUE);
		}
		String allocationID = job.optString(TSIMessages.ALLOCATION_ID, null);
		if(allocationID!=null) {
			action.getProcessingContext().put(TSIMessages.ALLOCATION_ID, allocationID);
			job.remove(TSIMessages.ALLOCATION_ID);
		}
		return createJobResource(action, tt, tags);
	}

	/*
	 * if user submits a job with a tt longer than the TSS lifetime
	 * automatically extend the TSS lifetime
	 */
	protected void checkAndExtendLT(Calendar newTT)throws UnableToSetTerminationTimeException, TerminationTimeChangeRejectedException{
		try{
			Calendar myTT = home.getTerminationTime(getUniqueID());
			if(myTT!=null && myTT.compareTo(newTT)<0){
				logger.debug("Job termination time exceeds TSS termination time, extending TSS lifetime...");
				Calendar tt=(Calendar)newTT.clone();
				tt.add(Calendar.DATE, 1);
				home.setTerminationTime(getUniqueID(),tt);
			}
		}catch(Exception e){
			Log.logException("Persistence error.", e, logger);
			throw new UnableToSetTerminationTimeException("Persistence error.");
		}
	}


	@Override
	public void initialise(InitParameters initobjs) throws Exception{
		TSSModel model = getModel();
		if(model == null){
			model = new TSSModel();
			setModel(model);
		}
		initobjs.resourceState = ResourceStatus.INITIALIZING;
		super.initialise(initobjs);

		String xnjsReference = model.getXnjsReference();
		model.setParentUID(initobjs.parentUUID);
		model.setParentServiceName(UAS.TSF);
		model.supportsReservation=XNJSFacade.get(xnjsReference, kernel).supportsReservation();
		model.umask = XNJSFacade.get(xnjsReference, kernel).getDefaultUmask();
		createAdditionalStorages();
		setResourceStatusMessage("OK");
		Map<String,String> desc = initobjs.extraParameters;
		List<Runnable>initTasks = getInitTasks(desc);
		kernel.getContainerProperties().getThreadingServices().getScheduledExecutorService().schedule(
				new TSSAsynchInitialisation(kernel, getUniqueID(),initTasks), 
				200, TimeUnit.MILLISECONDS);
	}

	/**
	 * Called when initialising a new TSS instance. The default implementation will attempt
	 * to re-connect old jobs and reservations
	 * @param desc - targetsystem description
	 */
	protected List<Runnable> getInitTasks(Map<String,String> desc) throws Exception{
		ArrayList<Runnable>initTasks = new ArrayList<Runnable>();
		Client c=getClient();
		String xnjsReference = getModel().getXnjsReference();
		//re-connect any old JMS instances
		initTasks.add(new RecreateJMSReferenceList(kernel, getUniqueID(),c));
		//re-create lost XNJS actions
		initTasks.add(new RecreateXNJSJobs(kernel,c,xnjsReference));
		//re-generate JMS instances from XNJS actions
		initTasks.add(new GenerateJMSInstances(kernel, getUniqueID(), c, xnjsReference));
		//re-generate Reservation list
		initTasks.add(new RecreateReservationReferenceList(kernel, getUniqueID(),c));
		return initTasks;
	}

	protected String createStorageID(String smsName){
		try{
			if(!uasProperties.getBooleanValue(UASProperties.TSS_FORCE_UNIQUE_STORAGE_IDS)){
				String xlogin=getClient().getXlogin().getUserName();
				return xlogin+"-"+smsName;	
			}
		}catch(Exception ex){}
		return Utilities.newUniqueID();
	}

	//create additional storages defined in the config file...
	protected void createAdditionalStorages(){
		Collection<StorageDescription> storages = uasProperties.getAddonStorages();
		for(StorageDescription a: storages){
			createStorageResource(a);
			logger.debug("Added " + a);
		}
	} 

	protected void createStorageResource(StorageDescription desc){
		String uuid=createStorageID(desc.getName());
		StorageInitParameters init = new StorageInitParameters(uuid,TerminationMode.NEVER);
		init.storageDescription = desc;
		init.acl.addAll(getModel().getAcl());
		init.xnjsReference = getModel().getXnjsReference();
		String id=createStorageManagement(desc.getStorageType(), init);
		if (id != null){
			getModel().getStorageIDs().add(id);
		}
	}

	/**
	 * create new Job and return the ID
	 * 
	 * @param action
	 * @param tt - termination time - <code>null</code> to use default lifetime
	 * @param tags - optional tags
	 * @return job ID
	 */
	public String createJobResource(Action action, Calendar tt, String...tags)throws Exception{
		JobInitParameters init = new JobInitParameters(action, tt);
		init.parentUUID = getUniqueID();
		init.xnjsReference = getXNJSReference();
		init.acl.addAll(getModel().getAcl());
		init.initialTags = tags;
		Home jmsHome = kernel.getHome(UAS.JMS); 
		return jmsHome.createResource(init);
	}

	public void registerJob(String jobID) {
		getModel().getJobIDs().add(jobID);
	}

	/**
	 * create new storage management service and return its epr
	 * @param type 
	 * @param init
	 * @return UUID or null if SMS could not be created
	 */
	protected String createStorageManagement(StorageTypes type, StorageInitParameters init){
		try {
			Home home=kernel.getHome(UAS.SMS);
			if(home==null){
				logger.warn("Storage management service is not deployed.");
				return null;
			}

			String id=init.uniqueID;
			boolean mustCreate=true;
			if(id!=null){
				try{
					SMSBaseImpl sms=(SMSBaseImpl)home.get(id);
					//exists, now check if it is accessible for the current user
					String smsOwner=sms.getOwner();
					if(smsOwner!=null && !kernel.getSecurityManager().isAccessible(getClient(), 
							sms.getServiceName(), id, 
							smsOwner, sms.getModel().getAcl())){
						mustCreate=true;
						logger.info("Existing storage <"+id+"> is not accessible to current user (certificate change?), re-creating it.");
					}
					else{
						mustCreate=false;
					}
				}catch(ResourceUnknownException ignored){
					mustCreate=true;
				}
			}
			if(mustCreate){
				id=home.createResource(init);
			}
			return id;
		} catch (ResourceNotCreatedException rnc){
			String msg=Log.createFaultMessage("Storage of type <"+type+"> was NOT created.", rnc);
			logger.info(msg);
		} catch (Exception e) {
			LogUtil.logException("Could not create storage management service.",e,logger);
		}
		return null;
	}

	/**
	 * resource-specific destruction
	 */
	@Override
	public void destroy() {
		TSSModel model = getModel();
		try{
			ResourceDeletedMessage m=new ResourceDeletedMessage("deleted:"+getUniqueID());
			m.setDeletedResource(getUniqueID());
			m.setServiceName(getServiceName());
			kernel.getMessaging().getChannel(model.getParentUID()).publish(m);
		}
		catch(Exception e){}
		if(uasProperties.getBooleanValue(UASProperties.TSS_FORCE_UNIQUE_STORAGE_IDS)){
			try{
				//destroy our SMS instances
				Home smsHome = kernel.getHome(UAS.SMS);
				for(String smsID: model.getStorageIDs()){
					smsHome.destroyResource(smsID);
				}
			}catch(Exception e){}
		}
		try{
			String xnjsRef = model.getXnjsReference(); 
			//shutdown the xnjs instance only if not the default instance
			if(xnjsRef!=null){
				XNJSFacade.get(xnjsRef,kernel).shutdown();
			}
		}catch(Exception e){}
		super.destroy();
		String ownerName=(getOwner()!=null?getOwner().toString():"<no client>");
		logger.debug("Removed TargetSystem resource <{}> owned by <{}>", getUniqueID(), ownerName);
	}

	// create a resource for the reservation and return its UUID
	public String createReservationResource(Map<String,String> resources, Calendar startTime)throws Exception{
		ReservationInitParameters init = new ReservationInitParameters();
		init.xnjsReference = getModel().getXnjsReference();
		init.tssReference = getUniqueID();
		init.resources = resources;
		init.starttime = startTime;
		String id = kernel.getHome(UAS.RESERVATIONS).createResource(init);
		getModel().getReservationIDs().add(id);
		return id;
	}

	@Override
	public String getUmask() {
		return getModel().getUmask();
	}

	@Override
	public void setUmask(String umask){
		getModel().setUmask(umask);
	}

}
