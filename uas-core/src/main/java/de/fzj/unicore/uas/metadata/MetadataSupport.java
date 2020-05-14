package de.fzj.unicore.uas.metadata;

import org.apache.log4j.Logger;

import de.fzj.unicore.uas.UASProperties;
import de.fzj.unicore.uas.util.LogUtil;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.xnjs.io.IStorageAdapter;
import eu.unicore.util.Log;

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
		
		Class<? extends MetadataManager> managerClass = null;
		try{
			managerClass=config.getClassValue(UASProperties.METADATA_MANAGER_CLASSNAME, MetadataManager.class);
		} catch(Exception ex){
			Log.logException("Shouldn't happen: metadata wrongly configured.", ex, logger);
		}
		
		if (managerClass!=null){
			try{
				MetadataManager mm=kernel.load(managerClass);
				return mm;
			}catch(Exception ex){
				Log.logException("Cannot instatiate metadata manager from class "+managerClass.getName(), ex, logger);
			}
		}
		return null;
	}
	
	/**
	 * get a new instance of the {@link MetadataManager}<br/>
	 * 
	 * @param kernel - Kernel instance
	 * @param storage - the storage manager
	 * @param uniqueID - the unique ID of the storage
	 */
	public static synchronized MetadataManager getManager(Kernel kernel, IStorageAdapter storage, String uniqueID){
		MetadataManager mm=MetadataSupport.getManager(kernel, kernel.getAttribute(UASProperties.class));
		if(mm instanceof StorageMetadataManager){
			StorageMetadataManager smm=(StorageMetadataManager)mm;
			smm.setStorageAdapter(storage, uniqueID);
		}
		return mm;
	}
}
