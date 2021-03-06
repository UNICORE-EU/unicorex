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


package de.fzj.unicore.uas.impl.sms.ws;

import java.util.List;

import org.unigrids.x2006.x04.services.smf.AccessibleStorageReferenceDocument;

import de.fzj.unicore.persist.PersistenceException;
import de.fzj.unicore.uas.StorageManagement;
import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.impl.sms.StorageFactoryImpl;
import eu.unicore.security.Client;
import eu.unicore.services.Home;
import eu.unicore.services.security.util.AuthZAttributeStore;
import eu.unicore.services.ws.XmlRenderer.Internal;
import eu.unicore.services.ws.renderers.AddressListRenderer;

/**
 * Generates filtered set of references to Storage Management Services 
 * created by the StorageFactory. Only "accessible" instances are listed!
 *
 * @author schuller
 */
public class AccessibleSMSReferenceRP extends AddressListRenderer implements Internal {

	public AccessibleSMSReferenceRP(StorageFactoryImpl parent){
		super(parent, UAS.SMS, AccessibleStorageReferenceDocument.type.getDocumentElementName(),StorageManagement.SMS_PORT,false);
	}

	@Override
	public List<String>getUIDs(){ 
		StorageFactoryImpl smf=((StorageFactoryImpl)parent);
		Client c=AuthZAttributeStore.getClient(); 
		try{
			Home sms = smf.getKernel().getHome(UAS.SMS);
			return sms.getAccessibleResources(smf.getModel().getSmsIDs(), c);
		} catch(PersistenceException pe){
			throw new RuntimeException(pe);
		}
	}

}
