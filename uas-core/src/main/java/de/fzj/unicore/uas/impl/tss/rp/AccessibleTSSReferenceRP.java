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
 

package de.fzj.unicore.uas.impl.tss.rp;

import java.util.List;

import org.unigrids.x2006.x04.services.tsf.AccessibleTargetSystemReferenceDocument;

import de.fzj.unicore.persist.PersistenceException;
import de.fzj.unicore.uas.TargetSystem;
import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.impl.tss.TargetSystemFactoryImpl;
import eu.unicore.security.Client;
import eu.unicore.services.Home;
import eu.unicore.services.security.util.AuthZAttributeStore;
import eu.unicore.services.ws.renderers.AddressListRenderer;

/**
 * Filtered set of references to Target System Services created by this Target System Factory
 * Only "accessible" instances are listed!
 *
 * @author schuller
 */
public class AccessibleTSSReferenceRP extends AddressListRenderer{
	
	public AccessibleTSSReferenceRP(TargetSystemFactoryImpl parent){
		super(parent,UAS.TSS,AccessibleTargetSystemReferenceDocument.type.getDocumentElementName(),TargetSystem.PORTTYPE,true);
	}

	@Override
	protected List<String>getUIDs() {
		TargetSystemFactoryImpl tsf=((TargetSystemFactoryImpl)parent);
		Client c=AuthZAttributeStore.getClient();
		List<String> tssIDs=tsf.getModel().getChildren(UAS.TSS);
		try{
			Home tss = tsf.getKernel().getHome(UAS.TSS);
			return tss.getAccessibleResources(tssIDs, c);
		}
		catch(PersistenceException pe){
			throw new RuntimeException(pe);
		}
		
	}
	
}
