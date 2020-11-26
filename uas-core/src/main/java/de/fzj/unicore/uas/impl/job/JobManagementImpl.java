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

import javax.xml.namespace.QName;

import org.apache.logging.log4j.Logger;
import org.unigrids.x2006.x04.services.jms.AbortDocument;
import org.unigrids.x2006.x04.services.jms.AbortResponseDocument;
import org.unigrids.x2006.x04.services.jms.HoldDocument;
import org.unigrids.x2006.x04.services.jms.HoldResponseDocument;
import org.unigrids.x2006.x04.services.jms.JobPropertiesDocument;
import org.unigrids.x2006.x04.services.jms.RestartDocument;
import org.unigrids.x2006.x04.services.jms.RestartResponseDocument;
import org.unigrids.x2006.x04.services.jms.ResumeDocument;
import org.unigrids.x2006.x04.services.jms.ResumeResponseDocument;
import org.unigrids.x2006.x04.services.jms.StartDocument;
import org.unigrids.x2006.x04.services.jms.StartResponseDocument;
import org.unigrids.x2006.x04.services.jms.SubmissionTimeDocument;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.JobManagement;
import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.UASProperties;
import de.fzj.unicore.uas.client.BaseUASClient;
import de.fzj.unicore.uas.faults.JobNotStartedFault;
import de.fzj.unicore.uas.impl.PersistingPreferencesResource;
import de.fzj.unicore.uas.impl.TagsRenderer;
import de.fzj.unicore.uas.impl.bp.BPSupportImpl;
import de.fzj.unicore.uas.impl.sms.StorageDescription;
import de.fzj.unicore.uas.impl.sms.StorageInitParameters;
import de.fzj.unicore.uas.util.LogUtil;
import de.fzj.unicore.wsrflite.Home;
import de.fzj.unicore.wsrflite.InitParameters;
import de.fzj.unicore.wsrflite.InitParameters.TerminationMode;
import de.fzj.unicore.wsrflite.exceptions.ResourceUnknownException;
import de.fzj.unicore.wsrflite.messaging.ResourceDeletedMessage;
import de.fzj.unicore.wsrflite.security.util.AuthZAttributeStore;
import de.fzj.unicore.wsrflite.xmlbeans.BaseFault;
import de.fzj.unicore.wsrflite.xmlbeans.renderers.AddressRenderer;
import de.fzj.unicore.wsrflite.xmlbeans.renderers.ValueRenderer;
import de.fzj.unicore.xnjs.ems.Action;
import eu.unicore.security.Client;
import eu.unicore.services.ws.utils.WSServerUtilities;

/**
 * implements a Job resource, and allows job management through WSRF
 * 
 * @author schuller
 */
public class JobManagementImpl extends PersistingPreferencesResource
implements JobManagement, XnjsActionBacked {
	
	private static final Logger logger = LogUtil.getLogger(LogUtil.JOBS,JobManagementImpl.class);

	public static final String JMS_NS = "http://unigrids.org/2006/04/services/jms";
	public static final QName STDOUT = new QName(JMS_NS,"StdOut");
	public static final QName STDERR = new QName(JMS_NS,"StdErr");

	public JobManagementImpl(){
		super();

		addRenderer(new LogResourceProperty(this));

		addRenderer(new ValueRenderer(this, RPSubmissionTime) {
			@Override
			protected SubmissionTimeDocument getValue() throws Exception {
				SubmissionTimeDocument sd=SubmissionTimeDocument.Factory.newInstance();
				sd.setSubmissionTime(getModel().getSubmissionTime());
				return sd;
			}
		});
	
		addRenderer(new StatusInfoResourceProperty(this));
	
		AddressRenderer renderer=new AddressRenderer(this, RPTargetSystemReference, true){
			 @Override
			 protected String getServiceSpec(){
				 return UAS.TSS+"?res="+getModel().getParentUID();
			 }
		};
		addRenderer(renderer);
		
		addRenderer(new ExecutionJSDLResourceProperty(this));
		addRenderer(new StdErrProperty(this));
		addRenderer(new StdOutProperty(this));
		addRenderer(new QueueRenderer(this));
		addRenderer(new EstimatedEndtimeRenderer(this));
		
		AddressRenderer uspaceEPR=new AddressRenderer(this, RPWorkingDir, true){
			 @Override
			 protected String getServiceSpec(){
				 return UAS.SMS+"?res="+getModel().getUspaceId();
			 }
		};
		addRenderer(uspaceEPR);
		addRenderer(new TagsRenderer(this));
	}
	
	@Override
	protected void addWSResourceInterfaces(BPSupportImpl baseProfile) {
		super.addWSResourceInterfaces(baseProfile);
		baseProfile.addWSResourceInterface(JMS_PORT);
	}
	
	@Override
	public QName getPortType() {
		return JMS_PORT;
	}
	
	@Override
	public QName getResourcePropertyDocumentQName() {
		return JobPropertiesDocument.type.getDocumentElementName();
	}
	
	public StartResponseDocument Start(StartDocument in) throws JobNotStartedFault {
		try{
			start();
		}catch(Exception e){
			LogUtil.logException("Could not start job "+getUniqueID(),e,logger);
			throw JobNotStartedFault.createFault(e.getMessage());
		}
		StartResponseDocument res=StartResponseDocument.Factory.newInstance();
		res.addNewStartResponse();
		return res;
	}

	public AbortResponseDocument Abort(AbortDocument in) throws BaseFault {
		try{
			abort();
		}catch(Exception e){
			LogUtil.logException("Could not abort the job.",e,logger);
			throw BaseFault.createFault("Could not abort the job.",e);
		}
		AbortResponseDocument res=AbortResponseDocument.Factory.newInstance();
		res.addNewAbortResponse();
		return res;
	}

	public HoldResponseDocument Hold(HoldDocument in) throws BaseFault {
		
		try{
			getXNJSFacade().getManager().pause(getUniqueID(), 
				AuthZAttributeStore.getClient());
		}catch(Exception e){
			LogUtil.logException("Could not hold the job.",e,logger);
			throw BaseFault.createFault("Could not hold the job.",e);
		}
		HoldResponseDocument res=HoldResponseDocument.Factory.newInstance();
		res.addNewHoldResponse();
		return res;
	}

	public ResumeResponseDocument Resume(ResumeDocument in) throws BaseFault {
		try{
			getXNJSFacade().getManager().resume(getUniqueID(), 
				AuthZAttributeStore.getClient());
		}catch(Exception e){
			LogUtil.logException("Could not resume the job.",e,logger);
			throw BaseFault.createFault("Could not resume the job",e);
		}
		ResumeResponseDocument res=ResumeResponseDocument.Factory.newInstance();
		res.addNewResumeResponse();
		return res;
	}


	public RestartResponseDocument Restart(RestartDocument in) throws BaseFault {
		try{
			restart();
		}catch(Exception e){
			LogUtil.logException("Could not restart the job.",e,logger);
			throw BaseFault.createFault("Could not restart the job",e);
		}
		RestartResponseDocument res=RestartResponseDocument.Factory.newInstance();
		res.addNewRestartResponse();
		return res;
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

	protected AddressRenderer createTSSReferenceProperty(){
		AddressRenderer renderer=new AddressRenderer(this, RPTargetSystemReference, true){
			 @Override
			 protected String getServiceSpec(){
				 return UAS.TSS+"?res="+getModel().getParentUID();
			 }
		};
		return renderer;
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
