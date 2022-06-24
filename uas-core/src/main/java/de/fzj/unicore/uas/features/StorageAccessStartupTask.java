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

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.uas.impl.sms.InitDefaultStorageFactory;
import de.fzj.unicore.uas.impl.sms.InitSharedStorages;
import eu.unicore.services.Kernel;
import eu.unicore.services.registry.LocalRegistryClient;
import eu.unicore.services.registry.RegistryHandler;
import eu.unicore.services.rest.client.RegistryClient;
import eu.unicore.util.Log;

/**
 * Startup code that initialises the storage factory and storages
 */
public class StorageAccessStartupTask implements Runnable{

	private static final Logger logger = Log.getLogger(Log.UNICORE,StorageAccessStartupTask.class);

	private final Kernel kernel;

	public StorageAccessStartupTask(Kernel kernel){
		this.kernel=kernel;
	}
	
	public void run(){
		createSharedStorages();
		createDefaultStorageFactoryIfNotExists();
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
	
	public static void publishWS(Kernel kernel, String serviceName, String uid, String interfaceName){
		try{
			LocalRegistryClient lrc = kernel.getAttribute(RegistryHandler.class).getRegistryClient();
			Map<String,String> res = new HashMap<>();
			String endpoint = kernel.getContainerProperties().getBaseUrl()+"/"+serviceName+"?res="+uid;
			res.put(RegistryClient.ENDPOINT, endpoint);
			res.put(RegistryClient.INTERFACE_NAME, interfaceName);
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

