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

import org.apache.log4j.Logger;

import de.fzj.unicore.uas.UASProperties;
import de.fzj.unicore.uas.xnjs.XNJSFacade;
import de.fzj.unicore.uas.xnjs.XNJSResource;
import de.fzj.unicore.wsrflite.InitParameters;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.impl.ResourceImpl;
import eu.unicore.util.Log;

/**
 * @author schuller
 */
public abstract class BaseResourceImpl extends ResourceImpl implements XNJSResource {
	
	protected static final Logger logger=Log.getLogger(Log.SERVICES,BaseResourceImpl.class);
	
	protected UASProperties uasProperties;
	
	public BaseResourceImpl(){
		super();
	}
	
	@Override
	public UASBaseModel getModel(){
		return (UASBaseModel)super.getModel(); 
	}
	
	public void setKernel(Kernel kernel){
		super.setKernel(kernel);
		uasProperties = kernel.getAttribute(UASProperties.class);
	}

	@Override
	public final void activate() {}

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
		if(initParams instanceof BaseInitParameters){
			m.setXnjsReference(((BaseInitParameters)initParams).xnjsReference);
		}
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

}