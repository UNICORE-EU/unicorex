/*********************************************************************************
 * Copyright (c) 2010 Forschungszentrum Juelich GmbH 
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

import eu.unicore.services.ws.BaseFault;
import eu.unicore.services.ws.ResourceLifetime;
import eu.unicore.services.ws.ResourceProperties;
import eu.unicore.services.ws.exceptions.ResourceUnavailableFault;
import eu.unicore.services.ws.exceptions.ResourceUnknownFault;
import eu.unicore.unicore6.task.CancelRequestDocument;
import eu.unicore.unicore6.task.CancelResponseDocument;
import eu.unicore.unicore6.task.ResultDocument;
import eu.unicore.unicore6.task.SubmissionServiceReferenceDocument;
import eu.unicore.unicore6.task.SubmissionTimeDocument;

/**
 * Task service for monitoring asynchronous tasks
 */
@WebService(targetNamespace = "http://www.unicore.eu/unicore6/task",portName="Task")
@SOAPBinding(parameterStyle=ParameterStyle.BARE, use=Use.LITERAL, style=Style.DOCUMENT)
public interface Task extends ResourceProperties,ResourceLifetime{

	//Namespace
	public static final String NAMESPACE="http://www.unicore.eu/unicore6/task";
	
	//Porttype
	public static final QName PORTTYPE=new QName(NAMESPACE,"Task");

	/**
	 * resource property giving the submision time
	 */
	public static final QName RP_SUBMISSION_TIME=SubmissionTimeDocument.type.getDocumentElementName();
	
	/**
	 * resource property giving the EPR of the "parent" service
	 */
	public static final QName RP_SUBMISSION_SERVICE_REFERENCE=SubmissionServiceReferenceDocument.type.getDocumentElementName();
	
	/**
	 * resource property giving the result of the task
	 */
	public static final QName RP_RESULT=ResultDocument.type.getDocumentElementName();
	
	/**
	 * resource property giving the result of the task
	 */
	public static final QName RP_STATUS=StatusInfoDocument.type.getDocumentElementName();
	
	/**
	 * cancel the task
	 * @param in
	 * @throws BaseFault
	 */
	@WebMethod(action = "http://www.unicore.eu/unicore6/task/Cancel")
	@WebResult(targetNamespace=NAMESPACE,name="CancelResponse")
	public CancelResponseDocument Cancel(
			@WebParam(targetNamespace=NAMESPACE,name="CancelRequest")
			CancelRequestDocument in)
			throws ResourceUnavailableFault,ResourceUnknownFault,BaseFault;

}
