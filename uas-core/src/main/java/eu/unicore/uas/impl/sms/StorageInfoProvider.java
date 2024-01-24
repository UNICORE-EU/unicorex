package eu.unicore.uas.impl.sms;

import java.util.Map;

/**
 * provides detailed information about a storage backend for use by the {@link StorageFactory}
 * 
 * @author schuller
 */
public interface StorageInfoProvider {

	/**
	 * get name and description for each parameter settable by the user when 
	 * invoking the StorageFactory service
	 * 
	 * @param storageDescription
	 * @return a map (which can be <code>null</code>) where key=parameter name and value=parameter description  
	 */
	public Map<String,String> getUserParameterInfo(StorageDescription storageDescription);
	
}
