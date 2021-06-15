/*********************************************************************************
 * Copyright (c) 2010 Forschungszentrum Juelich GmbH 
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


package de.fzj.unicore.uas.impl.sms.ws;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.FileSystemDocument;
import org.unigrids.services.atomic.types.PropertyType;
import org.unigrids.x2006.x04.services.smf.StorageBackendParametersDocument.StorageBackendParameters;
import org.unigrids.x2006.x04.services.smf.StorageDescriptionDocument;
import org.unigrids.x2006.x04.services.smf.StorageDescriptionType;

import de.fzj.unicore.uas.UASProperties;
import de.fzj.unicore.uas.impl.sms.StorageDescription;
import de.fzj.unicore.uas.impl.sms.StorageFactoryImpl;
import de.fzj.unicore.uas.impl.sms.StorageInfoProvider;
import de.fzj.unicore.uas.util.LogUtil;
import eu.unicore.security.Client;
import eu.unicore.services.Kernel;
import eu.unicore.services.security.util.AuthZAttributeStore;
import eu.unicore.services.ws.renderers.ValueRenderer;

/**
 * publishes descriptions of the available storage factory types to clients
 *
 * @author schuller
 */
public class StorageDescriptionRP extends ValueRenderer{

	private static final Logger logger = LogUtil.getLogger(LogUtil.DATA, StorageDescriptionRP.class);

	StorageFactoryImpl smf;

	public StorageDescriptionRP(StorageFactoryImpl parent){
		super(parent, StorageDescriptionDocument.type.getDocumentElementName());
		this.smf = parent;
	}

	@Override
	protected StorageDescriptionDocument[] getValue(){
		UASProperties props = parent.getKernel().getAttribute(UASProperties.class);
		Map<String, StorageDescription> factoriesDesc = props.getStorageFactories();
		StorageDescriptionDocument[] xml=new StorageDescriptionDocument[factoriesDesc.size()];
		int i=0;
		for(String type: factoriesDesc.keySet()){
			xml[i]=getStorageDescription(factoriesDesc.get(type));
			i++;
		}
		return xml;
	}

	protected StorageDescriptionDocument getStorageDescription(StorageDescription storageDesc){
		StorageDescriptionDocument sdd=StorageDescriptionDocument.Factory.newInstance();
		StorageDescriptionType sType=sdd.addNewStorageDescription();
		sType.setStorageBackendType(storageDesc.getName());
		try{
			StorageInfoProvider infoProvider = getInfoProvider(storageDesc);
			FileSystemDocument fs=getFileSystemDescription(infoProvider,storageDesc);
			if(fs!=null){
				sType.setFileSystem(fs.getFileSystem());
			}
			Map<String,String>paramDesc = infoProvider.getUserParameterInfo(storageDesc);
			if(paramDesc!=null && !paramDesc.isEmpty()){
				StorageBackendParameters sbp = sType.addNewStorageBackendParameters();
				for(Map.Entry<String, String>e: paramDesc.entrySet()){
					PropertyType p = sbp.addNewProperty();
					p.setName(e.getKey());
					p.setValue(e.getValue());
				}
			}
		}
		catch(Exception ex){
			LogUtil.logException("Can't get storage information for <"+storageDesc.getName()+">", 
					ex, logger);
		}
		return sdd;
	}

	protected FileSystemDocument getFileSystemDescription(StorageInfoProvider infoProvider, StorageDescription storageDesc)
			throws Exception {
		Client client=AuthZAttributeStore.getClient();
		FileSystemDocument fs=infoProvider.getInformation(storageDesc, client, null);
		return fs;
	}

	private StorageInfoProvider getInfoProvider(StorageDescription storageDesc) 
			throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
		Kernel kernel=smf.getKernel();
		Class<? extends StorageInfoProvider> infoP=storageDesc.getInfoProviderClass();
		return (StorageInfoProvider)kernel.load(infoP);
	}
}
