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

import org.unigrids.services.atomic.types.StatusInfoDocument;
import org.unigrids.services.atomic.types.StorageReferenceDocument;
import org.unigrids.x2006.x04.services.jms.EstimatedEndTimeDocument;
import org.unigrids.x2006.x04.services.jms.ExecutionJSDLDocument;
import org.unigrids.x2006.x04.services.jms.LogDocument;
import org.unigrids.x2006.x04.services.jms.OriginalJSDLDocument;
import org.unigrids.x2006.x04.services.jms.QueueDocument;
import org.unigrids.x2006.x04.services.jms.SubmissionTimeDocument;
import org.unigrids.x2006.x04.services.jms.TargetSystemReferenceDocument;
import org.unigrids.x2006.x04.services.jms.WorkingDirectoryReferenceDocument;

import de.fzj.unicore.uas.faults.JobNotStartedFault;
import de.fzj.unicore.wsrflite.xmlbeans.BaseFault;
import de.fzj.unicore.wsrflite.xmlbeans.ResourceLifetime;
import de.fzj.unicore.wsrflite.xmlbeans.ResourceProperties;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.ResourceUnavailableFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.ResourceUnknownFault;

/**
 * job management interface
 */
@WebService(targetNamespace = "http://unigrids.org/2006/04/services/jms",
		portName="JobManagement")
@SOAPBinding(parameterStyle=ParameterStyle.BARE, use=Use.LITERAL, style=Style.DOCUMENT)
public interface JobManagement extends ResourceLifetime,ResourceProperties {

	//Namespace
	public static final String JMS_NS="http://unigrids.org/2006/04/services/jms";
	
	//Porttype
	public static final QName JMS_PORT=new QName(JMS_NS,"JobManagement");
	
	//resource property qnames
	public static final QName RPSubmissionTime=SubmissionTimeDocument.type.getDocumentElementName();
	public static final QName RPStatusInfo=StatusInfoDocument.type.getDocumentElementName();
	public static final QName RPTargetSystemReference=TargetSystemReferenceDocument.type.getDocumentElementName();
	public static final QName RPOriginalJSDL=OriginalJSDLDocument.type.getDocumentElementName();
	public static final QName RPExecutionJSDL=ExecutionJSDLDocument.type.getDocumentElementName();
	public static final QName RPLog=LogDocument.type.getDocumentElementName();
	public static final QName RPWorkingDir=WorkingDirectoryReferenceDocument.type.getDocumentElementName();
	public static final QName RPStorageReference=StorageReferenceDocument.type.getDocumentElementName();
	public static final QName RPQueue=QueueDocument.type.getDocumentElementName();
	public static final QName RPEstimatedEndTime=EstimatedEndTimeDocument.type.getDocumentElementName();
	
	@WebMethod(action = "http://unigrids.org/2006/04/services/jms/JobManagement/StartRequest")
	@WebResult(targetNamespace=JMS_NS, name="StartResponse")
	public org.unigrids.x2006.x04.services.jms.StartResponseDocument Start(
			@WebParam(targetNamespace=JMS_NS, name="Start")
			org.unigrids.x2006.x04.services.jms.StartDocument in)
			throws ResourceUnavailableFault,ResourceUnknownFault,JobNotStartedFault;

	@WebMethod(action = "http://unigrids.org/2006/04/services/jms/JobManagement/AbortRequest")
	@WebResult(targetNamespace=JMS_NS, name="AbortResponse")
	public org.unigrids.x2006.x04.services.jms.AbortResponseDocument Abort(
			@WebParam(targetNamespace=JMS_NS, name="Abort")
			org.unigrids.x2006.x04.services.jms.AbortDocument in)
			throws ResourceUnavailableFault,ResourceUnknownFault,BaseFault;

	@WebMethod(action = "http://unigrids.org/2006/04/services/jms/JobManagement/HoldRequest")
	@WebResult(targetNamespace=JMS_NS, name="HoldResponse")
	public org.unigrids.x2006.x04.services.jms.HoldResponseDocument Hold(
			@WebParam(targetNamespace=JMS_NS, name="Hold")
			org.unigrids.x2006.x04.services.jms.HoldDocument in)
			throws ResourceUnavailableFault,ResourceUnknownFault,BaseFault;

	@WebMethod(action = "http://unigrids.org/2006/04/services/jms/JobManagement/ResumeRequest")
	@WebResult(targetNamespace=JMS_NS, name="ResumeResponse")
	public org.unigrids.x2006.x04.services.jms.ResumeResponseDocument Resume(
			@WebParam(targetNamespace=JMS_NS, name="Resume")
			org.unigrids.x2006.x04.services.jms.ResumeDocument in)
			throws ResourceUnavailableFault,ResourceUnknownFault,BaseFault;


	@WebMethod(action = "http://unigrids.org/2006/04/services/jms/JobManagement/RestartRequest")
	@WebResult(targetNamespace=JMS_NS, name="RestartResponse")
	public org.unigrids.x2006.x04.services.jms.RestartResponseDocument Restart(
			@WebParam(targetNamespace=JMS_NS, name="Restart")
			org.unigrids.x2006.x04.services.jms.RestartDocument in)
			throws ResourceUnavailableFault,ResourceUnknownFault,BaseFault;

}