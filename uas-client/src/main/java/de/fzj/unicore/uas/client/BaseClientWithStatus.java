/*********************************************************************************
 * Copyright (c) 2012 Forschungszentrum Juelich GmbH 
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


package de.fzj.unicore.uas.client;

import org.unigrids.services.atomic.types.ServiceStatusDocument;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.wsrflite.xfire.ClientException;
import de.fzj.unicore.wsrflite.xmlbeans.BaseFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.ResourceUnavailableFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.ResourceUnknownFault;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * A base client supporting extended service status
 * 
 * @author schuller
 */
public class BaseClientWithStatus extends BaseUASClient {

	/**
	 * create a UAS client using the supplied security properties
	 * 
	 * @param url - URL to connect to
	 * @param epr - EPR of the target service
	 * @param sec - the security settings to use
	 */
	public BaseClientWithStatus(String url, EndpointReferenceType epr, IClientConfiguration sec) throws Exception{
		super(url, epr, sec);
	}

	/**
	 * create a UAS client using the supplied security properties
	 * @param epr - EPR of the target service
	 * @param sec - security settings to use
	 */
	public BaseClientWithStatus(EndpointReferenceType epr, IClientConfiguration sec) throws Exception{
		super(epr.getAddress().getStringValue(), epr, sec);
	}

	/**
	 * Get the service's detailed status, if the service supports it.
	 * This includes the enumerated state, a details message and an expected time of change
	 * 
	 * @return {@link ServiceStatusDocument} or <code>null</code> if the service does not have the service status feature
	 */
	public ServiceStatusDocument getServiceStatusDocument()
			throws BaseFault,ResourceUnavailableFault,ResourceUnknownFault,ClientException{
		return getSingleResourceProperty(ServiceStatusDocument.class);
	}
	
	/**
	 * return the service's status (READY, INITIALIZING, etc) or <code>null</code> if the 
	 * service does not have a status
	 * @throws Exception
	 */
	public String getServiceStatus()throws Exception{
		ServiceStatusDocument ssd=getServiceStatusDocument();
		if(ssd!=null){
			return String.valueOf(ssd.getServiceStatus().getState());
		}
		else{
			return null;
		}
	}
	
	/**
	 * return the detailed service's status message or <code>null</code> if the
	 * service does not have a detailed status
	 * 
	 * @throws Exception
	 */
	public String getServiceStatusMessage()throws Exception{
		ServiceStatusDocument ssd=getServiceStatusDocument();
		if(ssd!=null){
			return String.valueOf(ssd.getServiceStatus().getDescription());
		}
		else{
			return null;
		}
	}

}
