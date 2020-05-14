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

//auto-generated from wsdl file src/main/schema/TargetSystemFactory.wsdl
//on Fri May 05 14:42:17 CEST 2006

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
import org.unigrids.services.atomic.types.SiteResourceDocument;
import org.unigrids.services.atomic.types.TextInfoDocument;
import org.unigrids.services.atomic.types.UpSinceDocument;
import org.unigrids.x2006.x04.services.tsf.AccessibleTargetSystemReferenceDocument;
import org.unigrids.x2006.x04.services.tsf.NameDocument;
import org.unigrids.x2006.x04.services.tsf.PerformanceDataDocument;
import org.unigrids.x2006.x04.services.tsf.TargetSystemReferenceDocument;
import org.unigrids.x2006.x04.services.tss.ApplicationResourceDocument;

import de.fzj.unicore.wsrflite.xmlbeans.BaseFault;
import de.fzj.unicore.wsrflite.xmlbeans.ResourceLifetime;
import de.fzj.unicore.wsrflite.xmlbeans.ResourceProperties;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.ResourceUnavailableFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.ResourceUnknownFault;
import eu.unicore.jsdl.extensions.ExecutionEnvironmentDescriptionDocument;
import eu.unicore.security.wsutil.RequiresSignature;

@WebService(targetNamespace = "http://unigrids.org/2006/04/services/tsf",
		portName="TargetSystemFactory")
@SOAPBinding(parameterStyle=ParameterStyle.BARE, use=Use.LITERAL, style=Style.DOCUMENT)
public interface TargetSystemFactory extends ResourceProperties,ResourceLifetime{

	//Namespace
	public static final String TSF_NS="http://unigrids.org/2006/04/services/tsf";
	
	//Porttype
	public static final QName TSF_PORT=new QName(TSF_NS,"TargetSystemFactory");

	//action for "CreateTSR"
	public static final String ACTION_CREATETSR="http://unigrids.org/2006/04/services/tsf/TargetSystemFactory/CreateTSR";

	//resource property QNames
	
	public static final QName RPApplicationResource=ApplicationResourceDocument.type.getDocumentElementName();
	public static final QName RPTSSReferences=TargetSystemReferenceDocument.type.getDocumentElementName();
	public static final QName RPAccessibleTSSReferences=AccessibleTargetSystemReferenceDocument.type.getDocumentElementName();
	public static final QName RPCPUTime = IndividualCPUTimeDocument.type.getDocumentElementName();
	public static final QName RPCPUCount = IndividualCPUCountDocument.type.getDocumentElementName();
	public static final QName RPTotalCPUCount = TotalCPUCountDocument.type.getDocumentElementName();
	public static final QName RPNodeCount = TotalResourceCountDocument.type.getDocumentElementName();
	public static final QName RPMemoryPerNode = IndividualPhysicalMemoryDocument.type.getDocumentElementName();
	public static final QName RPTextInfo = TextInfoDocument.type.getDocumentElementName();
	public static final QName RPProcessor = ProcessorDocument.type.getDocumentElementName();
	public static final QName RPUpSince = UpSinceDocument.type.getDocumentElementName();
	public static final QName RPSiteSpecificResources = SiteResourceDocument.type.getDocumentElementName();
	public static final QName RPAvailableResources = AvailableResourceDocument.type.getDocumentElementName();
	public static final QName RPName = NameDocument.type.getDocumentElementName();
	public static final QName RPOperatingSystem = OperatingSystemDocument.type.getDocumentElementName();
	public static final QName RPExecutionEnvironments= ExecutionEnvironmentDescriptionDocument.type.getDocumentElementName();
	public static final QName RPPerformanceData=PerformanceDataDocument.type.getDocumentElementName(); 

	@RequiresSignature
	@WebMethod(action = ACTION_CREATETSR)
	@WebResult(targetNamespace=TSF_NS,name="CreateTSRResponse")
	public org.unigrids.x2006.x04.services.tsf.CreateTSRResponseDocument CreateTSR(
			@WebParam(targetNamespace=TSF_NS,name="CreateTSR")
			org.unigrids.x2006.x04.services.tsf.CreateTSRDocument in)
			throws ResourceUnavailableFault,ResourceUnknownFault,BaseFault;

}
