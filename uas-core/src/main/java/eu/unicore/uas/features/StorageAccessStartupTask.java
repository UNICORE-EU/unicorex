package eu.unicore.uas.features;

import eu.unicore.services.Kernel;
import eu.unicore.uas.impl.sms.InitDefaultStorageFactory;
import eu.unicore.uas.impl.sms.InitSharedStorages;

/**
 * Startup code that initialises the storage factory and storages
 */
public class StorageAccessStartupTask implements Runnable{

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

}

