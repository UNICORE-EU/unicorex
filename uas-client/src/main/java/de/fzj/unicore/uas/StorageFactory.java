/*********************************************************************************
 * Copyright (c) 2009 Forschungszentrum Juelich GmbH 
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

import org.unigrids.x2006.x04.services.smf.AccessibleStorageEnumerationDocument;
import org.unigrids.x2006.x04.services.smf.AccessibleStorageReferenceDocument;
import org.unigrids.x2006.x04.services.smf.StorageDescriptionDocument;
import org.unigrids.x2006.x04.services.smf.StorageEnumerationDocument;
import org.unigrids.x2006.x04.services.smf.StorageReferenceDocument;

import eu.unicore.security.wsutil.RequiresSignature;
import eu.unicore.services.ws.BaseFault;
import eu.unicore.services.ws.ResourceLifetime;
import eu.unicore.services.ws.ResourceProperties;
import eu.unicore.services.ws.exceptions.ResourceUnavailableFault;
import eu.unicore.services.ws.exceptions.ResourceUnknownFault;

@WebService(targetNamespace = "http://unigrids.org/2006/04/services/smf",
		portName="StorageFactory")
@SOAPBinding(parameterStyle=ParameterStyle.BARE, use=Use.LITERAL, style=Style.DOCUMENT)
public interface StorageFactory extends ResourceProperties,ResourceLifetime{

	//Namespace
	public static final String SMF_NS="http://unigrids.org/2006/04/services/smf";
	
	//Porttype
	public static final QName SMF_PORT=new QName(SMF_NS,"StorageFactory");

	//action for "CreateSMS"
	public static final String ACTION_CREATESMS="http://unigrids.org/2006/04/services/smf/StorageFactory/CreateSMS";

	//resource property QNames
	public static final QName RPSMSReferences=StorageReferenceDocument.type.getDocumentElementName();
	public static final QName RPSMSEnumeration=StorageEnumerationDocument.type.getDocumentElementName();
	public static final QName RPAccessibleSMSReferences=AccessibleStorageReferenceDocument.type.getDocumentElementName();
	public static final QName RPAccessibleSMSEnumeration=AccessibleStorageEnumerationDocument.type.getDocumentElementName();
	public static final QName RPStorageDescription=StorageDescriptionDocument.type.getDocumentElementName();
	
	
	@RequiresSignature
	@WebMethod(action = ACTION_CREATESMS)
	@WebResult(targetNamespace=SMF_NS, name="CreateSMSResponse")
	public org.unigrids.x2006.x04.services.smf.CreateSMSResponseDocument CreateSMS(
			@WebParam(targetNamespace=SMF_NS, name="CreateSMS")
			org.unigrids.x2006.x04.services.smf.CreateSMSDocument in)
			throws ResourceUnavailableFault,ResourceUnknownFault,BaseFault;

}
