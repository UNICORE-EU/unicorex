/*********************************************************************************
 * Copyright (c) 2014 Forschungszentrum Juelich GmbH 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * (1) Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the disclaimer at the end. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * 
 * (2) Neither the name of Forschungszentrum Juelich GmbH nor the names of its 
 * contributors may be used to endorse or promote products derived from this 
 * software without specific prior written permission.
 * 
 * DISCLAIMER
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************/


package de.fzj.unicore.uas.impl.tss;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import de.fzj.unicore.persist.PersistenceException;
import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.UASProperties;
import de.fzj.unicore.uas.impl.BaseResourceImpl;
import de.fzj.unicore.uas.impl.UmaskSupport;
import de.fzj.unicore.uas.impl.job.JobInitParameters;
import de.fzj.unicore.uas.impl.reservation.ReservationInitParameters;
import de.fzj.unicore.uas.impl.sms.SMSBaseImpl;
import de.fzj.unicore.uas.impl.sms.StorageDescription;
import de.fzj.unicore.uas.impl.sms.StorageInitParameters;
import de.fzj.unicore.uas.impl.tss.util.GenerateJMSInstances;
import de.fzj.unicore.uas.impl.tss.util.RecreateJMSReferenceList;
import de.fzj.unicore.uas.impl.tss.util.RecreateReservationReferenceList;
import de.fzj.unicore.uas.impl.tss.util.RecreateXNJSJobs;
import de.fzj.unicore.uas.impl.tss.util.TSSAsynchInitialisation;
import de.fzj.unicore.uas.util.LogUtil;
import de.fzj.unicore.uas.xnjs.XNJSFacade;
import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.json.JSONParser;
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
import eu.unicore.services.messaging.ResourceDeletedMessage;
import eu.unicore.services.utils.Utilities;
import eu.unicore.util.Log;

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
					ResourceDeletedMessage rdm=(ResourceDeletedMessage)message;
					String id=rdm.getDeletedResource();
					String service=rdm.getServiceName();
					if(UAS.JMS.equals(service)){
						getModel().getJobIDs().remove(id);
					}
					else if(UAS.RESERVATIONS.equals(service)){
						getModel().getReservationIDs().remove(id);
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
		String umask = new JSONParser().parseUmask(job);
		if(umask == null)umask = getUmask();
		action.setUmask(umask);
		if(autoStartWhenReady){
			action.getProcessingContext().put(Action.AUTO_SUBMIT, Boolean.TRUE);
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
		}catch(PersistenceException e){
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

		createAdditionalStorages();

		setStatusMessage("OK");
		
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
		String id=createStorageManagement(desc.getStorageTypeAsString(), init);
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
	protected String createStorageManagement(String type, StorageInitParameters init){
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

	//create a WS Resource for the reservation and return its UUID
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
