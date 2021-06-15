/*********************************************************************************
 * Copyright (c) 2006-2007 Forschungszentrum Juelich GmbH 
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

import org.ggf.schemas.jsdl.x2005.x11.jsdl.RangeValueType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.ResourcesType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.TotalCPUCountDocument;

import de.fzj.unicore.uas.xnjs.XNJSResource;
import eu.unicore.security.Client;
import eu.unicore.services.security.util.AuthZAttributeStore;
import eu.unicore.services.ws.renderers.ValueRenderer;

/**
 * represents the total number of CPUs on a 
 * TargetSystem or TargetSystemFactory resource<br/>
 */
public class TotalCPUsResourceProperty extends ValueRenderer {
	
	private XNJSResource parent;
	
	public TotalCPUsResourceProperty(XNJSResource res) {
		super(res, TotalCPUCountDocument.type.getDocumentElementName());
		this.parent=res;
	}

	@Override
	protected TotalCPUCountDocument getValue(){
		Client client = AuthZAttributeStore.getClient();
		ResourcesType res = parent.getXNJSFacade().getResources(client);
		RangeValueType cpuCount = res.getTotalCPUCount();
		TotalCPUCountDocument cput=null;
		if(cpuCount!=null){
			cput=TotalCPUCountDocument.Factory.newInstance();
			cput.setTotalCPUCount(cpuCount);
		}
		return cput;
	}
}
