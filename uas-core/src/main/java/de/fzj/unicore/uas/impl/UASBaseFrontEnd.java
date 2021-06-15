/*********************************************************************************
 * Copyright (c) 2014 Forschungszentrum Juelich GmbH 
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

import javax.xml.namespace.QName;

import org.ggf.baseprofile.WSResourceInterfacesDocument;

import de.fzj.unicore.uas.impl.bp.BPSupportImpl;
import de.fzj.unicore.uas.impl.bp.BPWSResource;
import eu.unicore.services.WSRFConstants;
import eu.unicore.services.impl.ResourceImpl;
import eu.unicore.services.ws.impl.WSRFFrontend;
import eu.unicore.services.ws.renderers.ValueRenderer;

/**
 * @author schuller
 */
public abstract class UASBaseFrontEnd extends WSRFFrontend implements BPWSResource {
	
	protected BPSupportImpl baseProfile;
	
	public UASBaseFrontEnd(final ResourceImpl resource){
		super(resource);
		
		// setup base profile support
		baseProfile=new BPSupportImpl();
		addWSResourceInterfaces(baseProfile);
		ValueRenderer r0=new ValueRenderer(resource, BPSupportImpl.RPResourcePropertyNames) {
			@Override
			protected Object getValue() throws Exception {
				return BPSupportImpl.getRPNamesProperty(renderers.keySet());
			}
		};
		addRenderer(r0);
		
		ValueRenderer r1=new ValueRenderer(resource, BPSupportImpl.RPWsResourceInterfaces) {
			@Override
			protected Object getValue() throws Exception {
				return getWSResourceInterfaces();
			}
		};
		addRenderer(r1);
		if(getPortType()!=null){
			ValueRenderer r2=new ValueRenderer(resource, BPSupportImpl.RPFinalWSResourceInterface) {
				@Override
				protected Object getValue() throws Exception {
					return baseProfile.getFinalResourceInterfaceRP(getPortType());
				}
			};
			ValueRenderer r3=new ValueRenderer(resource, BPSupportImpl.RPResourceEndpointReference) {
				@Override
				protected Object getValue() throws Exception {
					return BPSupportImpl.getResourceEndpointReferenceRP(getEPR());
				}
			};
			addRenderer(r2);
			addRenderer(r3);
		}
		addRenderer(new VersionResourceProperty(resource));
		addRenderer(new ServiceStateResourceProperty(resource));
	}
	
	protected void addWSResourceInterfaces(BPSupportImpl baseProfile){
		baseProfile.addWSResourceInterface(WSRFConstants.WSRL_DESTROY_PORT);
		baseProfile.addWSResourceInterface(WSRFConstants.WSRL_SET_TERMTIME_PORT);
		baseProfile.addWSResourceInterface(WSRFConstants.WSRP_GET_RP_DOCUMENT_PORT);
		baseProfile.addWSResourceInterface(WSRFConstants.WSRP_GET_RP_PORT);
		baseProfile.addWSResourceInterface(WSRFConstants.WSRP_QUERY_RP_PORT);
	}
	
	@Override
	public abstract QName getResourcePropertyDocumentQName();

	@Override
	public abstract QName getPortType();
	
	/**
	 * returns an XML document listing the interfaces (PortTypes) realised
	 * by this class (see OGSA BaseProfile 1.0)
	 *  
	 * @return WSResourceInterfacesDocument
	 */
	public WSResourceInterfacesDocument getWSResourceInterfaces() {
		return baseProfile.getWSResourceInterfaces();
	}

}