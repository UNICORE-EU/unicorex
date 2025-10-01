package eu.unicore.uas.metadata;

import org.apache.logging.log4j.Logger;

import eu.unicore.services.Kernel;
import eu.unicore.uas.UASProperties;
import eu.unicore.uas.util.LogUtil;
import eu.unicore.util.Log;
import eu.unicore.xnjs.io.IStorageAdapter;

/**
 * factory and configuration class for the metadata support
 * 
 * @author schuller
 */
public class MetadataSupport {

	private static final Logger logger = LogUtil.getLogger(LogUtil.DATA, MetadataSupport.class);

	private MetadataSupport(){}

	/**
	 * get a new instance of the {@link MetadataManager}<br/>
	 * 
	 * The class to be used is read from configuration
	 */
	public static synchronized MetadataManager getManager(Kernel kernel, UASProperties config) {
		MetadataManager mm = null;
		try{
			var mgrClass = config.getClassValue(UASProperties.METADATA_MANAGER_CLASSNAME, MetadataManager.class);
			if(mgrClass!=null) {
				mm = kernel.load(mgrClass);
				if(!logged) {
					logged = true;
					logger.info("Loaded MetadataManager <{}>", mgrClass.getName());
				}
			}
		}catch(Exception ex){
			if(!logged) {
				logged = true;
				Log.logException("Cannot instantiate metadata manager.", ex, logger);
			}
		}
		return mm;
	}

	private static boolean logged = false;

	/**
	 * get a new instance of the {@link MetadataManager}<br/>
	 * 
	 * @param kernel - Kernel instance
	 * @param storage - the storage manager
	 * @param uniqueID - the unique ID of the storage
	 */
	public static synchronized MetadataManager getManager(Kernel kernel, IStorageAdapter storage, String uniqueID){
		MetadataManager mm = getManager(kernel, kernel.getAttribute(UASProperties.class));
		if(mm !=null && mm instanceof StorageMetadataManager){
			StorageMetadataManager smm=(StorageMetadataManager)mm;
			smm.setStorageAdapter(storage, uniqueID);
		}
		return mm;
	}
}
