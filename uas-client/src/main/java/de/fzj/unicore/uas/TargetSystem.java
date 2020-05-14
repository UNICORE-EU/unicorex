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
 

package de.fzj.unicore.uas;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.ParameterStyle;
import javax.jws.soap.SOAPBinding.Style;
import javax.jws.soap.SOAPBinding.Use;
import javax.xml.namespace.QName;

import org.ggf.schemas.jsdl.x2005.x11.jsdl.IndividualCPUCountDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.IndividualCPUTimeDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.IndividualPhysicalMemoryDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.OperatingSystemDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.TotalCPUCountDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.TotalResourceCountDocument;
import org.unigrids.services.atomic.types.AvailableResourceDocument;
import org.unigrids.services.atomic.types.ProcessorDocument;
import org.unigrids.services.atomic.types.ServiceStatusDocument;
import org.unigrids.services.atomic.types.SiteResourceDocument;
import org.unigrids.services.atomic.types.StorageReferenceDocument;
import org.unigrids.services.atomic.types.TextInfoDocument;
import org.unigrids.services.atomic.types.UpSinceDocument;
import org.unigrids.x2006.x04.services.tss.ComputeTimeBudgetDocument;
import org.unigrids.x2006.x04.services.tss.JobReferenceDocument;
import org.unigrids.x2006.x04.services.tss.JobReferenceEnumerationDocument;
import org.unigrids.x2006.x04.services.tss.NameDocument;
import org.unigrids.x2006.x04.services.tss.ReservationReferenceDocument;
import org.unigrids.x2006.x04.services.tss.TotalNumberOfJobsDocument;

import de.fzj.unicore.wsrflite.xmlbeans.BaseFault;
import de.fzj.unicore.wsrflite.xmlbeans.ResourceLifetime;
import de.fzj.unicore.wsrflite.xmlbeans.ResourceProperties;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.ResourceUnavailableFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.ResourceUnknownFault;
import eu.unicore.jsdl.extensions.ExecutionEnvironmentDescriptionDocument;
import eu.unicore.security.wsutil.RequiresSignature;

@WebService(targetNamespace = "http://unigrids.org/2006/04/services/tss",
		portName="TargetSystem")
@SOAPBinding(parameterStyle=ParameterStyle.BARE, use=Use.LITERAL, style=Style.DOCUMENT)
public interface TargetSystem extends ResourceReservation, ResourceProperties, ResourceLifetime{


	//Namespace
	public static final String TSS_NS="http://unigrids.org/2006/04/services/tss";
	
	//Porttype
	public static final QName TSS_PORT=new QName(TSS_NS,"TargetSystem");
	
	//action for "Submit"
	public static final String ACTION_SUBMIT="http://unigrids.org/2006/04/services/tss/TargetSystem/SubmitRequest";
	
	//action for "DeleteJobs"
	public static final String ACTION_DELETEJOBS="http://unigrids.org/2006/04/services/tss/TargetSystem/DeleteJobsRequest";
	
	//action for "GetJobsStatus"
	public static final String ACTION_GETJOBSSTATUS="http://unigrids.org/2006/04/services/tss/TargetSystem/GetJobsStatusRequest";
		
	//target system resourceproperty QNames
	
	public static final QName RPNumberOfJobs = TotalNumberOfJobsDocument.type.getDocumentElementName();
	public static final QName RPName = NameDocument.type.getDocumentElementName();
	public static final QName RPApplication = new QName("http://unigrids.org/2006/04/services/tss","ApplicationResource");
	public static final QName RPProcessor = ProcessorDocument.type.getDocumentElementName();
	public static final QName RPStorageReference = StorageReferenceDocument.type.getDocumentElementName();
	public static final QName RPJobReference = JobReferenceDocument.type.getDocumentElementName();
	public static final QName RPReservationReference = ReservationReferenceDocument.type.getDocumentElementName();
	public static final QName RPCPUTime = IndividualCPUTimeDocument.type.getDocumentElementName();
	public static final QName RPCPUCount = IndividualCPUCountDocument.type.getDocumentElementName();
	public static final QName RPTotalCPUCount = TotalCPUCountDocument.type.getDocumentElementName();
	public static final QName RPNodeCount = TotalResourceCountDocument.type.getDocumentElementName();
	public static final QName RPMemoryPerNode = IndividualPhysicalMemoryDocument.type.getDocumentElementName();
	public static final QName RPTextInfo = TextInfoDocument.type.getDocumentElementName();
	public static final QName RPSiteResource = SiteResourceDocument.type.getDocumentElementName();
	public static final QName RPAvailableResources = AvailableResourceDocument.type.getDocumentElementName();
	public static final QName RPUpSince = UpSinceDocument.type.getDocumentElementName();
	public static final QName RPSiteSpecificResources = SiteResourceDocument.type.getDocumentElementName();
	public static final QName RPOperatingSystem = OperatingSystemDocument.type.getDocumentElementName();
	public static final QName RPExecutionEnvironments= ExecutionEnvironmentDescriptionDocument.type.getDocumentElementName();
	public static final QName RPServiceStatus = ServiceStatusDocument.type.getDocumentElementName();
	public static final QName RPComputeTimeBudget = ComputeTimeBudgetDocument.type.getDocumentElementName();
	
	/**
	 * @since 6.3.0
	 */
	public static final QName RPJobReferenceEnumeration= JobReferenceEnumerationDocument.type.getDocumentElementName();
	
	@RequiresSignature
	@WebMethod(action = ACTION_SUBMIT)
	@WebResult(targetNamespace=TSS_NS,name="SubmitResponse")
	public org.unigrids.x2006.x04.services.tss.SubmitResponseDocument Submit(
			@WebParam(targetNamespace=TSS_NS,name="Submit")
			org.unigrids.x2006.x04.services.tss.SubmitDocument in)
			throws ResourceUnavailableFault,ResourceUnknownFault,BaseFault;
	
	@RequiresSignature
	@WebMethod(action = ACTION_DELETEJOBS)
	@WebResult(targetNamespace=TSS_NS,name="DeleteJobsResponse")
	public org.unigrids.x2006.x04.services.tss.DeleteJobsResponseDocument DeleteJobs(
			@WebParam(targetNamespace=TSS_NS,name="DeleteJobs")
			org.unigrids.x2006.x04.services.tss.DeleteJobsDocument in)
			throws ResourceUnavailableFault,ResourceUnknownFault,BaseFault;
	
	@WebMethod(action = ACTION_GETJOBSSTATUS)
	@WebResult(targetNamespace=TSS_NS,name="GetJobsStatusResponse")
	public org.unigrids.x2006.x04.services.tss.GetJobsStatusResponseDocument GetJobsStatus(
			@WebParam(targetNamespace=TSS_NS,name="GetJobsStatus")
			org.unigrids.x2006.x04.services.tss.GetJobsStatusDocument in)
			throws ResourceUnavailableFault,ResourceUnknownFault,BaseFault;
		
}
