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
 

package de.fzj.unicore.uas.impl.sms;

import java.util.Collection;

import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.util.LogUtil;
import de.fzj.unicore.wsrflite.InitParameters;
import de.fzj.unicore.wsrflite.Resource;
import de.fzj.unicore.wsrflite.exceptions.ResourceNotCreatedException;
import eu.unicore.services.ws.impl.WSResourceHomeImpl;
import eu.unicore.services.ws.impl.WSResourceImpl;

/**
 * Storage service home. Depending on the passed-in init parameters, different
 * types of storage service instances can be created.
 * 
 * @author schuller
 */
public class StorageManagementHomeImpl extends WSResourceHomeImpl {
	
	/**
	 * the types of storages</br>
	 * HOME: mapped to current user's home
	 * VARIABLE: actual path is looked up using the TSI, resolving any variables
	 * FIXEDPATH: mapped to a fixed path (e.g. "/opt/unicore/files")
	 * CUSTOM: other, needs a class to instantiate
	 */
	public static enum StorageTypes {
		HOME,
		FIXEDPATH,
		CUSTOM,
		VARIABLE
	}

	@Override
	protected WSResourceImpl doCreateInstance(InitParameters initObjs) throws Exception {
		String clazz = initObjs.resourceClassName;
		return(WSResourceImpl)(Class.forName(clazz).getConstructor().newInstance());
	}
	
	@Override
	public  String createResource(InitParameters map) throws ResourceNotCreatedException {
		StorageInitParameters init = (StorageInitParameters)map;
		StorageDescription storageDesc = init.storageDescription;
		StorageTypes st=storageDesc.getStorageType();
		String clazz=null;
		if (st.equals(StorageTypes.HOME)){
			clazz=HomeStorageImpl.class.getName();
		}
		else if (st.equals(StorageTypes.FIXEDPATH)){
			clazz=FixedStorageImpl.class.getName();
		}
		else if (st.equals(StorageTypes.VARIABLE)){
			clazz=PathedStorageImpl.class.getName();
		}
		else if (st.equals(StorageTypes.CUSTOM)){
			clazz=storageDesc.getStorageClass().getName();
		}
		else{
			throw new ResourceNotCreatedException("Unknown storage type!?");
		}
		map.resourceClassName = clazz;
		return super.createResource(map);
	}
	
	@Override
	protected void postInitialise(Resource r){
		if(r instanceof SMSBaseImpl){
			try{
				((SMSBaseImpl)r).setupMetadataService();
			}catch(Exception ex){
				LogUtil.logException("Error setting up metadata service instance for storage "+r.getUniqueID(), ex, logger);
			}
			try{
				((SMSBaseImpl)r).setupDirectoryScan();
			}catch(Exception ex){
				LogUtil.logException("Error setting up directory scan for storage "+r.getUniqueID(), ex, logger);
			}
		}
	}

	/**
	 * check and (if necessary) repair the internal state of the instances after server start
	 * TODO remove this again for UNICORE 8
	 */
	public void run(){
		super.run();
		try{
			Collection<String> uniqueIDs=serviceInstances.getUniqueIDs();
			for(String id: uniqueIDs){
				Resource r = serviceInstances.read(id);
				if(r!=null && r instanceof SMSBaseImpl){
					SMSBaseImpl s = (SMSBaseImpl)r;
					try{
						String enumID = s.getModel().getFileTransferEnumerationID();
						if(enumID!=null && !getKernel().getHome(UAS.ENUMERATION).getStore().getUniqueIDs().contains(enumID)){
							s.createFTListEnumeration();
						}
					}catch(Exception ex){
						LogUtil.logException("Error checking file transfer enumeration for storage <"+id+">", ex, logger);
					}
					try{
						String metaID = s.getModel().getMetadataServiceID();
						if(metaID!=null && !getKernel().getHome(UAS.META).getStore().getUniqueIDs().contains(metaID)){
							s.setupMetadataService();
						}
					}catch(Exception ex){
						LogUtil.logException("Error checking metadata service for storage <"+id+">", ex, logger);
					}
				}
			}
		}catch(Exception ex){
			logger.warn(ex);
		}
	}
	
}