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
 

package de.fzj.unicore.uas.impl;

import java.security.MessageDigest;

import org.unigrids.services.atomic.types.ServiceStateType;
import org.unigrids.services.atomic.types.ServiceStatusDocument;

import de.fzj.unicore.wsrflite.ExtendedResourceStatus;
import de.fzj.unicore.wsrflite.ExtendedResourceStatus.ResourceStatus;
import de.fzj.unicore.wsrflite.Resource;
import de.fzj.unicore.wsrflite.xmlbeans.AbstractXmlRenderer;

/**
 * Allows to publish status information about a service instance 
 * that implements the {@link ExtendedResourceStatus} interface
 *
 * @author schuller
 */
public class ServiceStateResourceProperty extends AbstractXmlRenderer {

	private final Resource parent;
	
	/**
	 * Create a new service status property
	 * @param parent - the parent resource
	 */
	public ServiceStateResourceProperty(Resource parent) {
		super(ServiceStatusDocument.type.getDocumentElementName());
		this.parent=parent;
	}

	@Override
	public ServiceStatusDocument[] render() {
		ServiceStatusDocument doc=ServiceStatusDocument.Factory.newInstance();
		doc.addNewServiceStatus();
		if(parent instanceof ExtendedResourceStatus){
			ExtendedResourceStatus r=(ExtendedResourceStatus)parent;
			doc.getServiceStatus().setDescription(r.getStatusMessage());
			doc.getServiceStatus().setState(convert(r.getResourceStatus()));
		}
		else{
			doc.getServiceStatus().setState(ServiceStateType.UNDEFINED);
		}
		return new ServiceStatusDocument[]{doc};
	}

	private ServiceStateType.Enum convert(ResourceStatus resourceStatus){
		switch(resourceStatus){
		case UNDEFINED: 
			return ServiceStateType.UNDEFINED;
		case READY: 
			return ServiceStateType.READY;
		case INITIALIZING: 
			return ServiceStateType.INITIALIZING;
		case DISABLED: 
			return ServiceStateType.DISABLED;
		case ERROR: 
			return ServiceStateType.ERROR;
		case SHUTTING_DOWN: 
			return ServiceStateType.SHUTTING_DOWN;				
		default: return ServiceStateType.UNDEFINED;
		}
	}

	@Override
	public void updateDigest(MessageDigest md) throws Exception {
		md.update(render()[0].toString().getBytes());
	}
	
}
