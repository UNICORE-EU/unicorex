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
 

package de.fzj.unicore.uas.impl.job.ws;

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

import de.fzj.unicore.uas.JobManagement;
import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.faults.JobNotStartedFault;
import de.fzj.unicore.uas.impl.TagsRenderer;
import de.fzj.unicore.uas.impl.UASBaseFrontEnd;
import de.fzj.unicore.uas.impl.bp.BPSupportImpl;
import de.fzj.unicore.uas.impl.job.JobManagementImpl;
import de.fzj.unicore.uas.util.LogUtil;
import de.fzj.unicore.wsrflite.security.util.AuthZAttributeStore;
import de.fzj.unicore.wsrflite.xmlbeans.BaseFault;
import de.fzj.unicore.wsrflite.xmlbeans.renderers.AddressRenderer;
import de.fzj.unicore.wsrflite.xmlbeans.renderers.ValueRenderer;

/**
 * implements a Job resource, and allows job management through WSRF
 * 
 * @author schuller
 */
public class JobFrontend extends UASBaseFrontEnd implements JobManagement {
	
	private static final Logger logger = LogUtil.getLogger(LogUtil.JOBS,JobFrontend.class);

	public static final String JMS_NS = "http://unigrids.org/2006/04/services/jms";
	public static final QName STDOUT = new QName(JMS_NS,"StdOut");
	public static final QName STDERR = new QName(JMS_NS,"StdErr");

	private final JobManagementImpl resource;
	
	public JobFrontend(JobManagementImpl r){
		super(r);
		this.resource = r;
		addRenderer(new LogResourceProperty(r));
		addRenderer(new ValueRenderer(r, RPSubmissionTime) {
			@Override
			protected SubmissionTimeDocument getValue() throws Exception {
				SubmissionTimeDocument sd=SubmissionTimeDocument.Factory.newInstance();
				sd.setSubmissionTime(r.getModel().getSubmissionTime());
				return sd;
			}
		});
	
		addRenderer(new StatusInfoResourceProperty(r));
	
		AddressRenderer renderer = new AddressRenderer(r, RPTargetSystemReference, true){
			 @Override
			 protected String getServiceSpec(){
				 return UAS.TSS+"?res=" + r.getModel().getParentUID();
			 }
		};
		addRenderer(renderer);
		
		addRenderer(new ExecutionJSDLResourceProperty(r));
		addRenderer(new StdErrProperty(r));
		addRenderer(new StdOutProperty(r));
		addRenderer(new QueueRenderer(r));
		addRenderer(new EstimatedEndtimeRenderer(r));
		
		AddressRenderer uspaceEPR = new AddressRenderer(r, RPWorkingDir, true){
			 @Override
			 protected String getServiceSpec(){
				 return UAS.SMS+"?res=" + r.getModel().getUspaceId();
			 }
		};
		addRenderer(uspaceEPR);
		addRenderer(new TagsRenderer(r));
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
			resource.start();
		}catch(Exception e){
			LogUtil.logException("Could not start job " + resource.getUniqueID(),e,logger);
			throw JobNotStartedFault.createFault(e.getMessage());
		}
		StartResponseDocument res=StartResponseDocument.Factory.newInstance();
		res.addNewStartResponse();
		return res;
	}

	public AbortResponseDocument Abort(AbortDocument in) throws BaseFault {
		try{
			resource.abort();
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
			resource.getXNJSFacade().getManager().pause(resource.getUniqueID(), 
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
			resource.getXNJSFacade().getManager().resume(resource.getUniqueID(), 
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
			resource.restart();
		}catch(Exception e){
			LogUtil.logException("Could not restart the job.",e,logger);
			throw BaseFault.createFault("Could not restart the job",e);
		}
		RestartResponseDocument res=RestartResponseDocument.Factory.newInstance();
		res.addNewRestartResponse();
		return res;
	}
		
	
	protected AddressRenderer createTSSReferenceProperty(){
		AddressRenderer renderer = new AddressRenderer(resource, RPTargetSystemReference, true){
			 @Override
			 protected String getServiceSpec(){
				 return UAS.TSS+"?res=" + resource.getModel().getParentUID();
			 }
		};
		return renderer;
	}
	
	/**
	 * update the "parent TSS" ID
	 * @param id
	 */
	public void setTSSID(String id){
		resource.getModel().setParentUID(id);
	}

}
