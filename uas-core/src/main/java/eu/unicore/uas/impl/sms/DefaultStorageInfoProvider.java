package eu.unicore.uas.impl.sms;

import java.util.HashMap;
import java.util.Map;

import eu.unicore.services.Kernel;

/**
 * provides information about the storage backed by a filesystem
 */
public class DefaultStorageInfoProvider implements StorageInfoProvider {

	protected final Kernel kernel;
	
	public DefaultStorageInfoProvider(Kernel kernel){
		this.kernel = kernel;
	}
		
	@Override
	public Map<String,String> getUserParameterInfo(StorageDescription storageDescription){
		Map<String,String> params = new HashMap<>();
		if(storageDescription!=null && storageDescription.isAllowUserdefinedPath()){
			params.put("path","Root path of the new storage");
		}
		return params;
	}
}
