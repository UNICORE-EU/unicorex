package eu.unicore.uas.impl.sms;

import java.util.HashMap;
import java.util.Map;

import eu.unicore.services.Kernel;
import eu.unicore.uas.SMSProperties;

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
		if(storageDescription!=null) {
			if(storageDescription.isAllowUserdefinedPath()){
				params.put("path","Root path of the new storage");
			}
			boolean tr = storageDescription.isEnableTrigger();
			if(storageDescription.isAllowTrigger()){
				params.put(SMSProperties.ENABLE_TRIGGER, "Enable the data triggering feature. (default: "+tr+")");
			}
			boolean meta = storageDescription.isDisableMetadata();
			params.put(SMSProperties.DISABLE_METADATA, "Disable metadata (default: "+meta+")");
		}
		return params;
	}
}
