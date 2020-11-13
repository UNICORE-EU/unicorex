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

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.logging.log4j.Logger;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.ResourcesDocument;
import org.oasisOpen.docs.wsrf.rl2.TerminationTimeDocument.TerminationTime;
import org.unigrids.x2006.x04.services.reservation.ResourceReservationRequestDocument;
import org.unigrids.x2006.x04.services.reservation.ResourceReservationResponseDocument;
import org.unigrids.x2006.x04.services.tss.DeleteJobsDocument;
import org.unigrids.x2006.x04.services.tss.DeleteJobsResponseDocument;
import org.unigrids.x2006.x04.services.tss.GetJobsStatusDocument;
import org.unigrids.x2006.x04.services.tss.GetJobsStatusResponseDocument;
import org.unigrids.x2006.x04.services.tss.JobStatusDocument.JobStatus;
import org.unigrids.x2006.x04.services.tss.NameDocument;
import org.unigrids.x2006.x04.services.tss.SubmitDocument;
import org.unigrids.x2006.x04.services.tss.SubmitResponseDocument;
import org.unigrids.x2006.x04.services.tss.SupportsReservationDocument;
import org.unigrids.x2006.x04.services.tss.TargetSystemPropertiesDocument;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.JobManagement;
import de.fzj.unicore.uas.ReservationManagement;
import de.fzj.unicore.uas.ResourceReservation;
import de.fzj.unicore.uas.TargetSystem;
import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.impl.UASBaseFrontEnd;
import de.fzj.unicore.uas.impl.UmaskRenderer;
import de.fzj.unicore.uas.impl.bp.BPSupportImpl;
import de.fzj.unicore.uas.impl.job.StatusInfoResourceProperty;
import de.fzj.unicore.uas.impl.tss.rp.ApplicationsResourceProperty;
import de.fzj.unicore.uas.impl.tss.rp.AvailableResourcesRP;
import de.fzj.unicore.uas.impl.tss.rp.CPUCountResourceProperty;
import de.fzj.unicore.uas.impl.tss.rp.CPUTimeResourceProperty;
import de.fzj.unicore.uas.impl.tss.rp.ComputeTimeBudgetRenderer;
import de.fzj.unicore.uas.impl.tss.rp.JobReferenceResourceProperty;
import de.fzj.unicore.uas.impl.tss.rp.MemoryPerNodeResourceProperty;
import de.fzj.unicore.uas.impl.tss.rp.NodesResourceProperty;
import de.fzj.unicore.uas.impl.tss.rp.NumberOfJobsProperty;
import de.fzj.unicore.uas.impl.tss.rp.OperatingSystemResourceProperty;
import de.fzj.unicore.uas.impl.tss.rp.ProcessorResourceProperty;
import de.fzj.unicore.uas.impl.tss.rp.ReservationReferenceResourceProperty;
import de.fzj.unicore.uas.impl.tss.rp.StorageReferenceResourceProperty;
import de.fzj.unicore.uas.impl.tss.rp.TextInfoResourceProperty;
import de.fzj.unicore.uas.impl.tss.rp.TotalCPUsResourceProperty;
import de.fzj.unicore.uas.impl.tss.rp.UpSinceResourceProperty;
import de.fzj.unicore.uas.util.LogUtil;
import de.fzj.unicore.uas.xnjs.XNJSFacade;
import de.fzj.unicore.wsrflite.ContainerProperties;
import de.fzj.unicore.wsrflite.xmlbeans.BaseFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.ResourceUnavailableFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.ResourceUnknownFault;
import de.fzj.unicore.wsrflite.xmlbeans.renderers.AddressRenderer;
import de.fzj.unicore.wsrflite.xmlbeans.renderers.ValueRenderer;
import de.fzj.unicore.xnjs.ems.Action;
import eu.unicore.services.ws.utils.WSServerUtilities;

/**
 * The WSRF frontend for the TargetSystem service.<br/>
 * 
 * @author schuller
 */
public class TSSFrontend extends UASBaseFrontEnd implements TargetSystem {

	private static final Logger logger = LogUtil.getLogger(LogUtil.JOBS, TSSFrontend.class);

	private final TargetSystemImpl resource;
	
	public TSSFrontend(TargetSystemImpl r){
		super(r);
		this.resource = r;
		addRenderer(new StorageReferenceResourceProperty(resource));
		addRenderer(new ReservationReferenceResourceProperty(resource));
		addRenderer(new JobReferenceResourceProperty(resource));

		addRenderer(new AddressRenderer(resource, RPJobReferenceEnumeration, true){
			@Override
			protected String getServiceSpec() {
				return UAS.ENUMERATION+"?res="+resource.getModel().jobEnumerationID;
			}
		});

		addRenderer(new ValueRenderer(resource, ResourceReservation.RP_SUPPORTS_RESERVATION) {
			@Override
			protected SupportsReservationDocument getValue() throws Exception {
				SupportsReservationDocument res=SupportsReservationDocument.Factory.newInstance();
				res.setSupportsReservation(resource.getModel().supportsReservation);
				return res;
			}
		}); 

		addRenderer(new OperatingSystemResourceProperty(resource));
		addRenderer(new CPUTimeResourceProperty(resource));
		addRenderer(new NodesResourceProperty(resource));
		addRenderer(new MemoryPerNodeResourceProperty(resource));
		addRenderer(new CPUCountResourceProperty(resource));
		addRenderer(new TotalCPUsResourceProperty(resource));
		addRenderer(new UpSinceResourceProperty(resource));
		addRenderer(new AvailableResourcesRP(resource));
		addRenderer(new UmaskRenderer(resource));

		addRenderer(new NumberOfJobsProperty(resource));
		addRenderer(new ValueRenderer(resource, RPName) {
			@Override
			protected NameDocument getValue() throws Exception {
				NameDocument nd=NameDocument.Factory.newInstance();
				String name = resource.getKernel().getContainerProperties().getValue(ContainerProperties.VSITE_NAME_PROPERTY);
				nd.setName(name);
				return nd;
			}
		}); 
		addRenderer(new ApplicationsResourceProperty(resource));
		addRenderer(new ProcessorResourceProperty(resource));
		addRenderer(new TextInfoResourceProperty(resource));
		addRenderer(new ComputeTimeBudgetRenderer(resource));
	}

	@Override
	protected void addWSResourceInterfaces(BPSupportImpl baseProfile) {
		super.addWSResourceInterfaces(baseProfile);
		baseProfile.addWSResourceInterface(TSS_PORT);
		baseProfile.addWSResourceInterface(ResourceReservation.PORTTYPE);
	}

	@Override
	public QName getResourcePropertyDocumentQName() {
		return TargetSystemPropertiesDocument.type.getDocumentElementName();
	}

	@Override
	public SubmitResponseDocument Submit(SubmitDocument in) throws BaseFault {
		if( !((TargetSystemHomeImpl)resource.getHome()).isJobSubmissionEnabled()){
			throw BaseFault.createFault(((TargetSystemHomeImpl)resource.getHome()).getHighMessage());
		}
		if(logger.isTraceEnabled()){
			logger.trace("submit: "+in.toString());
		}
		try {
			JobDefinitionType jsdl = in.getSubmit().getJobDefinition();
			JobDefinitionDocument doc = JobDefinitionDocument.Factory
					.newInstance();
			doc.setJobDefinition(jsdl);

			Calendar jobTT = null;
			TerminationTime itt=in.getSubmit().getTerminationTime();
			if(itt!=null && itt.getCalendarValue()!=null){
				jobTT=itt.getCalendarValue();
			}
			
			String id = resource.submit(doc,in.getSubmit().getAutoStartWhenReady(), jobTT);
			EndpointReferenceType epr=WSServerUtilities.makeEPR(UAS.JMS, id, JobManagement.JMS_PORT, kernel);
			SubmitResponseDocument response = SubmitResponseDocument.Factory.newInstance();
			response.addNewSubmitResponse().setJobReference(epr);
			return response;
		} catch (Exception e) {
			LogUtil.logException("Error submitting.",e,logger);
			throw BaseFault.createFault("Error submitting: "+e.getMessage(),e);
		}
	}

	@Override
	public QName getPortType() {
		return TSS_PORT;
	}

	@Override
	public ResourceReservationResponseDocument ReserveResources(ResourceReservationRequestDocument in) throws BaseFault {
		try{
			ResourcesDocument rd=ResourcesDocument.Factory.newInstance();
			rd.setResources(in.getResourceReservationRequest().getResources());
			Calendar startTime=in.getResourceReservationRequest().getStartTime();
			String id=resource.createReservationResource(rd,startTime);
			EndpointReferenceType epr=WSServerUtilities.makeEPR(UAS.RESERVATIONS, id, ReservationManagement.PORT, true, resource.getKernel());
			ResourceReservationResponseDocument res=ResourceReservationResponseDocument.Factory.newInstance();
			res.addNewResourceReservationResponse().setReservationReference(epr);
			return res;
		}catch(Exception e){
			LogUtil.logException("Reservation not created.",e,logger);
			throw BaseFault.createFault("Reservation not created. Reason: "+e.getMessage());
		}
	}

	@Override
	public GetJobsStatusResponseDocument GetJobsStatus(GetJobsStatusDocument in){
		XNJSFacade xnjs=XNJSFacade.get(resource.getModel().getXnjsReference(), kernel);
		GetJobsStatusResponseDocument res=GetJobsStatusResponseDocument.Factory.newInstance();
		res.addNewGetJobsStatusResponse();
		List<String>jobIDs = resource.getModel().getJobIDs();
		for(String j: in.getGetJobsStatus().getJobIDArray()){
			// we only want to show the status of jobs on this TSS 
			if(!jobIDs.contains(j))continue;
			Action a = xnjs.getAction(j);
			if(a!=null){
				Integer xnjsState = a.getStatus(); 
				boolean success = a.getResult().isSuccessful();
				JobStatus status=res.getGetJobsStatusResponse().addNewJobStatus();
				status.setJobID(j);
				status.setStatus(StatusInfoResourceProperty.convertStatus(xnjsState, success));
			}
		}
		return res;
	}

	@Override
	public DeleteJobsResponseDocument DeleteJobs(DeleteJobsDocument in)
			throws ResourceUnavailableFault, ResourceUnknownFault, BaseFault {
		resource.deleteJobs(Arrays.asList(in.getDeleteJobs().getJobIDArray()));
		DeleteJobsResponseDocument resp = DeleteJobsResponseDocument.Factory.newInstance();
		resp.addNewDeleteJobsResponse();
		return resp;
	}

}
