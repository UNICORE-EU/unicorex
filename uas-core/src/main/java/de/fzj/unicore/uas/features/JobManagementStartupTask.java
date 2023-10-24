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

package de.fzj.unicore.uas.features;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.UASProperties;
import de.fzj.unicore.uas.impl.BaseInitParameters;
import de.fzj.unicore.uas.impl.tss.TargetSystemFactoryHomeImpl;
import de.fzj.unicore.uas.impl.tss.TargetSystemFactoryImpl;
import de.fzj.unicore.uas.xnjs.XNJSFacade;
import eu.unicore.persist.PersistenceException;
import eu.unicore.persist.impl.LockSupport;
import eu.unicore.services.Home;
import eu.unicore.services.InitParameters.TerminationMode;
import eu.unicore.services.Kernel;
import eu.unicore.services.exceptions.ResourceNotCreatedException;
import eu.unicore.services.exceptions.ResourceUnknownException;
import eu.unicore.services.registry.LocalRegistryClient;
import eu.unicore.services.registry.RegistryHandler;
import eu.unicore.services.rest.client.RegistryClient;
import eu.unicore.util.Log;

/**
 * Startup code that creates the default TargetSystemFactory service
  */
public class JobManagementStartupTask implements Runnable{

	private static final Logger logger = Log.getLogger(Log.UNICORE,JobManagementStartupTask.class);

	private final Kernel kernel;

	public JobManagementStartupTask(Kernel kernel){
		this.kernel=kernel;
	}
	
	public void run(){
		try{
			createDefaultTSFIfNotExists();
		}catch(Exception re){
			throw new RuntimeException("Could not create default TSF instance.",re);
		}
	}

	/**
	 * add a "default" target system factory if it does not yet exist	
	 */
	protected void createDefaultTSFIfNotExists()throws ResourceNotCreatedException,PersistenceException{
		Home tsfHome=kernel.getHome(UAS.TSF);
		if(tsfHome==null){
			logger.info("No TSF service configured for this site!");
			return;
		}
		logger.info("Initialising backend.");
		XNJSFacade.get(null,kernel);
		String defaultTsfName=TargetSystemFactoryHomeImpl.DEFAULT_TSF;
		
		LockSupport ls=kernel.getPersistenceManager().getLockSupport();
		Lock tsfLock=ls.getOrCreateLock(JobManagementStartupTask.class.getName());
		if(tsfLock.tryLock()){
			try{
				//this will throw ResourceUnknowException if resource does not exist
				tsfHome.get(defaultTsfName);
			}
			catch(ResourceUnknownException e){
				doCreateTSF(tsfHome);
			}
			finally{
				tsfLock.unlock();
			}
			publishWS(defaultTsfName);
		}
	}

	private void doCreateTSF(Home tsfHome)throws ResourceNotCreatedException{
		String defaultTsfName=TargetSystemFactoryHomeImpl.DEFAULT_TSF;
		BaseInitParameters init = new BaseInitParameters(defaultTsfName, TerminationMode.NEVER);
		UASProperties props = kernel.getAttribute(UASProperties.class);
		Class<?>clazz = props.getClassValue(UASProperties.TSF_CLASS, TargetSystemFactoryImpl.class);
		init.resourceClassName = clazz.getName();
		tsfHome.createResource(init);
		logger.info("Added default TSF resource '{}' of type <{}>.", defaultTsfName, clazz.getName());
	}

	public String toString(){
		return getClass().getName();
	}

	private void publishWS(String uid){
		try{
			LocalRegistryClient lrc = kernel.getAttribute(RegistryHandler.class).getRegistryClient();
			Map<String,String> res = new HashMap<>();
			String endpoint = kernel.getContainerProperties().getContainerURL()+"/rest/core/factories/"+uid;
			res.put(RegistryClient.ENDPOINT, endpoint);
			res.put(RegistryClient.INTERFACE_NAME, "TargetSystemFactory");
			String dn = kernel.getSecurityManager().getServerIdentity();
			if(dn!=null) {
				res.put(RegistryClient.SERVER_IDENTITY,dn);
			}
			lrc.addEntry(endpoint, res, null);
		}catch(Exception ex){
			Log.logException("Could not publish to local registry", ex, logger);
		}		
	}

}

