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

import org.unigrids.x2006.x04.services.reservation.ResourceReservationRequestDocument;
import org.unigrids.x2006.x04.services.reservation.ResourceReservationResponseDocument;
import org.unigrids.x2006.x04.services.tss.SupportsReservationDocument;

import de.fzj.unicore.wsrflite.xmlbeans.BaseFault;
import de.fzj.unicore.wsrflite.xmlbeans.ResourceLifetime;
import de.fzj.unicore.wsrflite.xmlbeans.ResourceProperties;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.ResourceUnavailableFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.ResourceUnknownFault;
import eu.unicore.security.wsutil.RequiresSignature;

/**
 * Resource reservation service
 */
@WebService(targetNamespace = "http://unigrids.org/2006/04/services/reservation",
		portName="ResourceReservation")
@SOAPBinding(parameterStyle=ParameterStyle.BARE, use=Use.LITERAL, style=Style.DOCUMENT)
public interface ResourceReservation extends ResourceProperties,ResourceLifetime{

	//Namespace
	public static final String NAMESPACE="http://unigrids.org/2006/04/services/reservation";
	
	//Porttype
	public static final QName PORTTYPE=new QName(NAMESPACE,"ResourceReservation");

	//action for "ReserveResources"
	public static final String ACTION_RESERVE="http://unigrids.org/2006/04/services/reservation/ResourceReservation/ReserveResources";

	/**
	 * resource property for advertising resource reservation support.
	 */
	public static final QName RP_SUPPORTS_RESERVATION=SupportsReservationDocument.type.getDocumentElementName();
	
	@RequiresSignature
	@WebMethod(action = ACTION_RESERVE)
	@WebResult(targetNamespace=NAMESPACE, name="ResourceReservationResponse")
	public ResourceReservationResponseDocument ReserveResources(
			@WebParam(targetNamespace=NAMESPACE, name="ResourceReservationRequest")
			ResourceReservationRequestDocument in)
			throws ResourceUnavailableFault,ResourceUnknownFault,BaseFault;

}
