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

import de.fzj.unicore.uas.util.LogUtil;
import eu.unicore.services.InitParameters;
import eu.unicore.services.Resource;
import eu.unicore.services.exceptions.ResourceNotCreatedException;
import eu.unicore.services.impl.DefaultHome;

/**
 * Storage service home. Depending on the passed-in init parameters, different
 * types of storage service instances can be created.
 * 
 * @author schuller
 */
public class StorageManagementHomeImpl extends DefaultHome {
	
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
	protected Resource doCreateInstance(InitParameters initObjs) throws Exception {
		String clazz = initObjs.resourceClassName;
		return(Resource)(Class.forName(clazz).getConstructor().newInstance());
	}
	
	protected Resource doCreateInstance() throws Exception {
		throw new IllegalStateException();
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
				((SMSBaseImpl)r).setupDirectoryScan();
			}catch(Exception ex){
				LogUtil.logException("Error setting up directory scan for storage "+r.getUniqueID(), ex, logger);
			}
		}
	}
	
}