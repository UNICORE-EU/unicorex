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

package de.fzj.unicore.uas.util;

import java.util.concurrent.locks.Lock;

import javax.xml.namespace.QName;

import org.apache.logging.log4j.Logger;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.persist.PersistenceException;
import de.fzj.unicore.persist.impl.LockSupport;
import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.UASProperties;
import de.fzj.unicore.uas.impl.BaseInitParameters;
import de.fzj.unicore.uas.impl.sms.InitDefaultStorageFactory;
import de.fzj.unicore.uas.impl.sms.InitSharedStorages;
import de.fzj.unicore.uas.impl.tss.TargetSystemFactoryHomeImpl;
import de.fzj.unicore.uas.impl.tss.TargetSystemFactoryImpl;
import de.fzj.unicore.uas.impl.tss.rp.TSFFrontend;
import de.fzj.unicore.uas.rest.CoreServices;
import de.fzj.unicore.uas.xnjs.XNJSFacade;
import de.fzj.unicore.wsrflite.Home;
import de.fzj.unicore.wsrflite.InitParameters.TerminationMode;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.exceptions.ResourceNotCreatedException;
import de.fzj.unicore.wsrflite.exceptions.ResourceUnknownException;
import de.fzj.unicore.wsrflite.registry.LocalRegistryClient;
import de.fzj.unicore.wsrflite.registry.ws.SGFrontend;
import de.fzj.unicore.wsrflite.xmlbeans.WSUtilities;
import de.fzj.unicore.wsrflite.xmlbeans.registry.RegistryHandler;
import eu.unicore.util.Log;

/**
 * Startup code that initialises the registry and creates the default 
 * TargetSystemFactory service.<br/>
 * 
 * To run, add the classname to the uas.config property "uas.onstartup.*"
 */
public class DefaultOnStartup implements Runnable{

	private static final Logger logger=LogUtil.getLogger(LogUtil.UNICORE,DefaultOnStartup.class);

	private final Kernel kernel;

	public DefaultOnStartup(Kernel kernel){
		this.kernel=kernel;
	}
	
	public void run(){
		try{
			createDefaultTSFIfNotExists();
		}catch(Exception re){
			throw new RuntimeException("Could not create default TSF instance.",re);
		}
		createSharedStorages();
		createDefaultStorageFactoryIfNotExists();
		CoreServices.publish(kernel);
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
		Lock tsfLock=ls.getOrCreateLock(DefaultOnStartup.class.getName());
		if(tsfLock.tryLock()){
			try{
				try{
					//this will throw ResourceUnknowException if resource does not exist
					TargetSystemFactoryImpl tsf=(TargetSystemFactoryImpl)tsfHome.get(defaultTsfName);
					//it exists, force re-publish
					publishWS(kernel, tsf.getServiceName(), tsf.getUniqueID(), TSFFrontend.TSF_PORT);
					return;
				}
				catch(ResourceUnknownException e){}
				
				doCreateTSF(tsfHome);
				publishWS(kernel, tsfHome.getServiceName(), defaultTsfName, TSFFrontend.TSF_PORT);
				
			}finally{
				tsfLock.unlock();
			}	
		}
	}

	private void doCreateTSF(Home tsfHome)throws ResourceNotCreatedException{
		String defaultTsfName=TargetSystemFactoryHomeImpl.DEFAULT_TSF;
		BaseInitParameters init = new BaseInitParameters(defaultTsfName, TerminationMode.NEVER);
		UASProperties props = kernel.getAttribute(UASProperties.class);
		Class<?>clazz = props.getClassValue(UASProperties.TSF_CLASS, TargetSystemFactoryImpl.class);
		init.resourceClassName = clazz.getName();
		tsfHome.createResource(init);
		logger.info("Added default TSF resource '"+defaultTsfName+"' of type <"+clazz.getName()+">.");
	}

	/**
	 * add a "default" storage factory instance if it does not yet exist	
	 */
	protected void createSharedStorages(){
		new InitSharedStorages(kernel).run();
	}
	
	/**
	 * add a "default" storage factory instance if it does not yet exist	
	 */
	protected void createDefaultStorageFactoryIfNotExists(){
		new InitDefaultStorageFactory(kernel).run();
	}
	
	public String toString(){
		return getClass().getName();
	}
	
	public static void publishWS(Kernel kernel, String serviceName, String uid, QName porttype){
		try{
			LocalRegistryClient lrc = kernel.getAttribute(RegistryHandler.class).getRegistryClient();
			String endpoint = kernel.getContainerProperties().getBaseUrl()+"/"+serviceName+"?res="+uid;
			EndpointReferenceType epr = WSUtilities.makeServiceEPR(endpoint, porttype);
			String dn = kernel.getSecurityManager().getServerIdentity();
			if(dn!=null){
				WSUtilities.addServerIdentity(epr, dn);
			}
			lrc.addEntry(endpoint, SGFrontend.parse(epr), null);
		}catch(Exception ex){
			Log.logException("Could not publish to local registry", ex, logger);
		}
		
	}

}

