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

package de.fzj.unicore.uas.impl;

import org.ggf.baseprofile.WSResourceInterfacesDocument;

import de.fzj.unicore.uas.UASProperties;
import de.fzj.unicore.uas.impl.bp.BPSupportImpl;
import de.fzj.unicore.uas.impl.bp.BPWSResource;
import de.fzj.unicore.uas.xnjs.XNJSFacade;
import de.fzj.unicore.uas.xnjs.XNJSResource;
import eu.unicore.services.InitParameters;
import eu.unicore.services.Kernel;
import eu.unicore.services.Resource;
import eu.unicore.services.WSRFConstants;
import eu.unicore.services.registry.LocalRegistryClient;
import eu.unicore.services.registry.RegistryHandler;
import eu.unicore.services.registry.ws.SGFrontend;
import eu.unicore.services.ws.impl.WSResourceImpl;
import eu.unicore.services.ws.renderers.ValueRenderer;
import eu.unicore.util.Log;

/**
 * a UNICORE WS Resource<br/>
 * It adds the following features to the basic {@link WSResourceImpl}:
 * <ul>
 *  <li>Support for the OGSA basic profile</li>
 *  <li>Sets up some data commonly needed by UAS services as uasProperties </li>
 * </ul>
 * @author schuller
 */
public abstract class UASWSResourceImpl extends WSResourceImpl implements BPWSResource, XNJSResource {

	protected BPSupportImpl baseProfile;

	public UASProperties uasProperties;

	public UASWSResourceImpl(){
		super();

		baseProfile=new BPSupportImpl();
		addWSResourceInterfaces(baseProfile);
		ValueRenderer r0=new ValueRenderer(this, BPSupportImpl.RPResourcePropertyNames) {
			@Override
			protected Object getValue() throws Exception {
				return BPSupportImpl.getRPNamesProperty(frontendDelegate.getRenderers().keySet());
			}
		};
		addRenderer(r0);

		ValueRenderer r1=new ValueRenderer(this, BPSupportImpl.RPWsResourceInterfaces) {
			@Override
			protected Object getValue() throws Exception {
				return getWSResourceInterfaces();
			}
		};
		addRenderer(r1);
		if(getPortType()!=null){
			ValueRenderer r2=new ValueRenderer(this, BPSupportImpl.RPFinalWSResourceInterface) {
				@Override
				protected Object getValue() throws Exception {
					return baseProfile.getFinalResourceInterfaceRP(getPortType());
				}
			};
			ValueRenderer r3=new ValueRenderer(this, BPSupportImpl.RPResourceEndpointReference) {
				@Override
				protected Object getValue() throws Exception {
					return BPSupportImpl.getResourceEndpointReferenceRP(getEPR());
				}
			};
			addRenderer(r2);
			addRenderer(r3);
		}
		addRenderer(new VersionResourceProperty(this));
		addRenderer(new ServiceStateResourceProperty(this));
	}

	protected void addWSResourceInterfaces(BPSupportImpl baseProfile){
		baseProfile.addWSResourceInterface(WSRFConstants.WSRL_DESTROY_PORT);
		baseProfile.addWSResourceInterface(WSRFConstants.WSRL_SET_TERMTIME_PORT);
		baseProfile.addWSResourceInterface(WSRFConstants.WSRP_GET_RP_DOCUMENT_PORT);
		baseProfile.addWSResourceInterface(WSRFConstants.WSRP_GET_RP_PORT);
		baseProfile.addWSResourceInterface(WSRFConstants.WSRP_QUERY_RP_PORT);
	}

	@Override
	public UASBaseModel getModel(){
		return (UASBaseModel)super.getModel(); 
	}

	public void setKernel(Kernel kernel){
		super.setKernel(kernel);
		uasProperties = kernel.getAttribute(UASProperties.class);
	}

	/**
	 * This method is <code>final</code> to avoid programmer errors in
	 * subclasses. To add custom behaviour, use the hook method
	 * {@link #customPostActivate()}
	 * @see Resource#activate()
	 */
	@Override
	public final void activate() {
		super.activate();
		customPostActivate();
	}

	/**
	 * add special post-activation behaviour by overriding this method 
	 */
	protected void customPostActivate(){}

	/**
	 * sets XNJS reference, setups WSRF base profile RPs and the server's version RP
	 */
	@Override
	public void initialise(InitParameters initParams)throws Exception{
		UASBaseModel m = getModel();
		if(m==null){
			m = new UASBaseModel();
			setModel(m);
		}
		super.initialise(initParams);
		uasProperties = kernel.getAttribute(UASProperties.class);
		uasProperties = kernel.getAttribute(UASProperties.class);
		if(initParams instanceof BaseInitParameters){
			m.setXnjsReference(((BaseInitParameters)initParams).xnjsReference);
		}
		if(initParams.publishToRegistry)publish();
	}

	/**
	 * returns an XML document listing the interfaces (PortTypes) realised
	 * by this class (see OGSA BaseProfile 1.0)
	 *  
	 * @return WSResourceInterfacesDocument
	 */
	public WSResourceInterfacesDocument getWSResourceInterfaces() {
		return baseProfile.getWSResourceInterfaces();
	}

	private String xnjsReference;

	public synchronized String getXNJSReference(){
		if(xnjsReference==null){
			xnjsReference = getModel().getXnjsReference();
		}
		return xnjsReference;
	}

	private XNJSFacade xnjs;

	public synchronized XNJSFacade getXNJSFacade(){
		if(xnjs==null){
			xnjs = XNJSFacade.get(getXNJSReference(),kernel);
		}
		return xnjs;
	}


	public void refreshSystemInfo(){
		if(getModel().getLastSystemInfoRefreshInstant()+30000 
				< System.currentTimeMillis()){
			return;
		}
		getModel().setLastSystemInfoRefreshInstant(System.currentTimeMillis());
		try{
			doRefreshSystemInfo();
		}catch(Exception ex){
			Log.logException("Error getting info from TSI", ex, logger);
		}
	}

	/**
	 * perform any updates of system-level info, invoked from
	 * refreshSystemInfo() if necessary
	 */
	protected void doRefreshSystemInfo() throws Exception {}

	public void publish() {
		try{
			LocalRegistryClient lrc = kernel.getAttribute(RegistryHandler.class).getRegistryClient();
			String endpoint = getEPR().getAddress().getStringValue();
			lrc.addEntry(endpoint, SGFrontend.parse(getEPR()), null);
		}catch(Exception ex){
			Log.logException("Could not publish to local registry", ex, logger);
		}
	}

}