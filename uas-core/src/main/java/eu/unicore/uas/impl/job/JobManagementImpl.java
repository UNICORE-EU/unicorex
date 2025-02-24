package eu.unicore.uas.impl.job;

import java.util.Arrays;
import java.util.Calendar;

import org.apache.logging.log4j.Logger;

import eu.unicore.security.Client;
import eu.unicore.services.Home;
import eu.unicore.services.InitParameters;
import eu.unicore.services.InitParameters.TerminationMode;
import eu.unicore.services.exceptions.ResourceUnknownException;
import eu.unicore.services.messaging.ResourceDeletedMessage;
import eu.unicore.services.security.util.AuthZAttributeStore;
import eu.unicore.services.utils.TimeProfile;
import eu.unicore.uas.UAS;
import eu.unicore.uas.UASProperties;
import eu.unicore.uas.impl.PersistingPreferencesResource;
import eu.unicore.uas.impl.sms.StorageDescription;
import eu.unicore.uas.impl.sms.StorageInitParameters;
import eu.unicore.uas.impl.sms.StorageManagementHomeImpl.StorageTypes;
import eu.unicore.uas.util.LogUtil;
import eu.unicore.xnjs.ems.Action;

/**
 * implements a Job resource
 * 
 * @author schuller
 */
public class JobManagementImpl extends PersistingPreferencesResource {

	private static final Logger logger = LogUtil.getLogger(LogUtil.JOBS,JobManagementImpl.class);

	public JobManagementImpl(){
		super();
	}

	public void restart() throws Exception {
		getXNJSFacade().getManager().restart(getUniqueID(), 
				AuthZAttributeStore.getClient());
	}

	public void start() throws Exception {
		getXNJSFacade().getManager().run(getUniqueID(), AuthZAttributeStore.getClient());
		logger.info("Started {}", getUniqueID());
	}

	public void abort() throws Exception {
		getXNJSFacade().getManager().abort(getUniqueID(), AuthZAttributeStore.getClient());
	}

	@Override
	public JobModel getModel(){
		return (JobModel)model;
	}

	@Override
	public void initialise(InitParameters init)throws Exception{
		if(model==null){
			model = new JobModel();
		}
		super.initialise(init);
		JobModel m = getModel();
		JobInitParameters initParams = (JobInitParameters) init;
		Action action=(Action)initParams.action;
		Client client = AuthZAttributeStore.getClient();
		logger.info("New job with id <{}> for <{}>", action.getUUID(), client.getDistinguishedName());
		m.setParentUID(initParams.parentUUID);
		m.setParentServiceName(UAS.TSS);
		if(initParams.autostart){
			action.getProcessingContext().put(Action.AUTO_SUBMIT,"true");
		}
		if(!initParams.no_xnjs_submit){
			getXNJSFacade().getManager().add(action, client);
		}
		m.setUspaceId(createUspace(action));
		m.setSubmissionTime(Calendar.getInstance());
		String[] tags = initParams.initialTags;
		if(tags!=null && tags.length>0){
			m.getTags().addAll(Arrays.asList(tags));
		}
	}

	/**
	 * clean up resources on the back-end, including job directory
	 */
	@Override
	public void destroy() {
		TimeProfile tp = AuthZAttributeStore.getTimeProfile();
		try{
			ResourceDeletedMessage m=new ResourceDeletedMessage("deleted:"+getUniqueID());
			m.setServiceName(getServiceName());
			m.setDeletedResource(getUniqueID());
			String tssId = getModel().getParentUID();
			getKernel().getMessaging().getChannel(tssId).publish(m);
		}
		catch(Exception e){}
		tp.time("sent_deleted_msg");
		try{
			getXNJSFacade().destroyAction(getUniqueID(), getClient());
		}
		catch(Exception e){}
		tp.time("triggered_action_destroy");
		try{
			kernel.getHome(UAS.SMS).destroyResource(getModel().getUspaceId());
		}catch(Exception e){}
		tp.time("sms_destroyed");
		super.destroy();
		tp.time("jms_destroyed");
	}
	
	/**
	 * creates an SMS instance for the job working directory
	 * 
	 * @return the unique id of the newly created SMS
	 */
	protected String createUspace(Action a) throws Exception {
		StorageInitParameters init = new StorageInitParameters(getUniqueID()+"-uspace", TerminationMode.NEVER);
		StorageDescription description = kernel.getAttribute(UASProperties.class).parseStorage(UASProperties.USPACE_SMS_PREFIX, 
				"UspaceOf-"+getUniqueID(), false);
		description.setName("UspaceOf-"+getUniqueID());
		description.setPathSpec(getUniqueID());
		description.setFilterListing(false);
		description.setCleanup(false);
		description.setDisableMetadata(true);
		description.setEnableTrigger(false);
		description.setDescription("Job workspace");
		description.setStorageType(StorageTypes.USPACE);
		init.storageDescription = description;
		init.xnjsReference = getXNJSReference();
		init.acl = getModel().getAcl();
		init.inheritSharing = true;
		Home smsHome=kernel.getHome(UAS.SMS);
		return smsHome.createResource(init);
	}

	/**
	 * update the "parent TSS" ID
	 * @param id
	 */
	public void setTSSID(String id){
		getModel().setParentUID(id);
	}
	
	private Action xnjsAction;
	
	/**
	 * Get the underlying XNJS action
	 */
	public synchronized Action getXNJSAction() throws Exception {
		if(xnjsAction == null){
			xnjsAction = getXNJSFacade().getAction(getUniqueID());
			if(xnjsAction==null){
				throw new ResourceUnknownException();
			}
		}
		return xnjsAction;
	}
}
