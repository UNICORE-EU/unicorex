/*********************************************************************************
 * Copyright (c) 2006 Forschungszentrum Juelich GmbH 
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
 

package de.fzj.unicore.uas.impl.job;

import java.util.Arrays;
import java.util.Calendar;

import org.apache.logging.log4j.Logger;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.UASProperties;
import de.fzj.unicore.uas.client.BaseUASClient;
import de.fzj.unicore.uas.impl.PersistingPreferencesResource;
import de.fzj.unicore.uas.impl.sms.StorageDescription;
import de.fzj.unicore.uas.impl.sms.StorageInitParameters;
import de.fzj.unicore.uas.util.LogUtil;
import de.fzj.unicore.xnjs.ems.Action;
import eu.unicore.security.Client;
import eu.unicore.services.Home;
import eu.unicore.services.InitParameters;
import eu.unicore.services.InitParameters.TerminationMode;
import eu.unicore.services.exceptions.ResourceUnknownException;
import eu.unicore.services.messaging.ResourceDeletedMessage;
import eu.unicore.services.security.util.AuthZAttributeStore;
import eu.unicore.services.ws.utils.WSServerUtilities;

/**
 * implements a Job resource, and allows job management through WSRF
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
		logger.info("Started "+getUniqueID());
	}
	

	public void abort() throws Exception {
		getXNJSFacade().getManager().abort(getUniqueID(),	AuthZAttributeStore.getClient());
	}
	
	@Override
	public JobModel getModel(){
		return (JobModel)model;
	}
	
	/**
	 * initialise the new WS Resource<br>
	 * this will submit the job to the XNJS and setup the Job's resource properties
	 */
	@Override
	public void initialise(InitParameters init)throws Exception{
		if(model==null){
			model = new JobModel();
		}
		super.initialise(init);
		JobModel m = getModel();
		JobInitParameters initParams = (JobInitParameters) init;
		Action action=(Action)initParams.action;
		
		m.setParentUID(initParams.parentUUID);
		m.setParentServiceName(UAS.TSS);
		
		if(initParams.autostart){
			action.getProcessingContext().put(Action.AUTO_SUBMIT,"true");
		}
		
		if(!initParams.no_xnjs_submit){
			Client client = AuthZAttributeStore.getClient();
			getXNJSFacade().getManager().add(action, client);
			logger.info("Submitted job with id "+action.getUUID()+ " for client "+client);
		}
		
		m.setUspaceId(createUspace(action));
		m.setSubmissionTime(Calendar.getInstance());
		
		String[] tags = initParams.initialTags;
		if(tags!=null && tags.length>0){
			m.getTags().addAll(Arrays.asList(tags));
		}
	}

	
	/**
	 * resource-specific destruction: send message about our demise
	 */
	@Override
	public void destroy() {
		try{
			ResourceDeletedMessage m=new ResourceDeletedMessage("deleted:"+getUniqueID());
			m.setServiceName(getServiceName());
			m.setDeletedResource(getUniqueID());
			String tssId = getModel().getParentUID();
			getKernel().getMessaging().getChannel(tssId).publish(m);
		}
		catch(Exception e){
			LogUtil.logException("Could not send internal message.",e,logger);
		}
		//clean up on backend
		try{
			getXNJSFacade().destroyAction(getUniqueID(), getClient());
		}
		catch(Exception e){
			LogUtil.logException("Could not destroy job on XNJS.",e,logger);
		}
		try{
			//destroy the uspace resource
			EndpointReferenceType uspaceEPR=WSServerUtilities.makeEPR(
				UAS.SMS, getModel().getUspaceId(), kernel);
			BaseUASClient c=new BaseUASClient(uspaceEPR, kernel.getClientConfiguration());
			c.destroy();
		}catch(Exception e){
			LogUtil.logException("Could not destroy storages.",e,logger);
		}
		super.destroy();
	}
	
	/**
	 * creates an SMS instance for the job working directory
	 * 
	 * @return the unique id of the newly created SMS
	 */
	protected String createUspace(Action a){
		StorageInitParameters init = new StorageInitParameters(getUniqueID()+"-uspace", TerminationMode.NEVER);
		StorageDescription description = kernel.getAttribute(UASProperties.class).parseStorage(UASProperties.USPACE_SMS_PREFIX, 
				"UspaceOf-"+getUniqueID(), false);
		description.setName("UspaceOf-"+getUniqueID());
		description.setPathSpec(a.getExecutionContext().getWorkingDirectory());
		description.setFilterListing(false);
		description.setCleanup(false);
		description.setDisableMetadata(true);
		description.setEnableTrigger(false);
		description.setDescription("Job's workspace");
		init.storageDescription = description;
		init.xnjsReference = getXNJSReference();
		init.acl = getModel().getAcl();
		init.inheritSharing = true;
		String id = null;
		try {
			Home smsHome=kernel.getHome(UAS.SMS);
			id = smsHome.createResource(init);
		} catch (Exception e) {
			LogUtil.logException("Could not create storage for job",e,logger);
		}
		return id;
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
	 * Get the underlying XNJS action. This is cached between calls to this method
	 * in order to speed up access, e.g. for rendering the resource properties
	 */
	public synchronized Action getXNJSAction(){
		if(xnjsAction == null){
			xnjsAction = getXNJSFacade().getAction(getUniqueID());
			if(xnjsAction==null){
				throw new ResourceUnknownException();
			}
		}
		return xnjsAction;
	}
}
