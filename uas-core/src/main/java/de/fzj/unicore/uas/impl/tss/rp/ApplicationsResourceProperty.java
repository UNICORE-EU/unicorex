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

import java.util.Collection;

import org.unigrids.services.atomic.types.MetadataDocument;
import org.unigrids.x2006.x04.services.tss.ApplicationResourceDocument;
import org.unigrids.x2006.x04.services.tss.ApplicationResourceType;

import de.fzj.unicore.uas.xnjs.XNJSFacade;
import de.fzj.unicore.uas.xnjs.XNJSResource;
import de.fzj.unicore.xnjs.idb.ApplicationInfo;
import de.fzj.unicore.xnjs.idb.ApplicationMetadata;
import de.fzj.unicore.xnjs.jsdl.JSDLRenderer;
import eu.unicore.security.Client;
import eu.unicore.services.Kernel;
import eu.unicore.services.security.util.AuthZAttributeStore;
import eu.unicore.services.ws.renderers.ValueRenderer;
import eu.unicore.services.ws.utils.WSServerUtilities;

/**
 * represents the applications on a TargetSystem resource<br>
 * 
 * @author schuller
 */
public class ApplicationsResourceProperty extends ValueRenderer {

	private XNJSResource res;
	
	public ApplicationsResourceProperty(XNJSResource parent) {
		super(parent, ApplicationResourceDocument.type.getDocumentElementName());
		this.res=parent;
	}

	/**
	 * updates application information from NJS
	 */
	@Override
	protected ApplicationResourceDocument[] getValue(){
		Kernel kernel=parent.getKernel();
		String xnjsReference = res.getXNJSReference();
		Client client = AuthZAttributeStore.getClient();
		Collection<ApplicationInfo> xnjsapps=XNJSFacade.get(xnjsReference, kernel).getIDB().getApplications(client);
		ApplicationResourceDocument[] appDocs=new ApplicationResourceDocument[xnjsapps.size()];
		int i=0;
		for(ApplicationInfo xnjsApp: xnjsapps){
			appDocs[i]=ApplicationResourceDocument.Factory.newInstance();
			appDocs[i].setApplicationResource(convertXNJSApp(xnjsApp));
			i++;
		}
		return appDocs;
	}

	public ApplicationResourceType convertXNJSApp(ApplicationInfo xnjsApp){
		ApplicationResourceType result=ApplicationResourceType.Factory.newInstance();
		result=ApplicationResourceType.Factory.newInstance();
		result.setApplicationName(xnjsApp.getName());
		result.setApplicationVersion(xnjsApp.getVersion());
		result.setDescription(xnjsApp.getDescription());
		ApplicationMetadata meta = xnjsApp.getMetadata();
		if(meta!=null){
			MetadataDocument mdd=MetadataDocument.Factory.newInstance();
			mdd.addNewMetadata();
			WSServerUtilities.append(new JSDLRenderer().render(meta), mdd);
			result.setMetadata(mdd.getMetadata());
		}
		return result;
	}

}
