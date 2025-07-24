package eu.unicore.uas.features;

import eu.unicore.services.Kernel;
import eu.unicore.services.StartupTask;
import eu.unicore.services.utils.deployment.FeatureImpl;
import eu.unicore.uas.UAS;
import eu.unicore.uas.fts.FileTransferHomeImpl;
import eu.unicore.uas.fts.uftp.UFTPStartupTask;
import eu.unicore.uas.impl.sms.InitDefaultStorageFactory;
import eu.unicore.uas.impl.sms.InitSharedStorages;
import eu.unicore.uas.impl.sms.StorageFactoryHomeImpl;
import eu.unicore.uas.impl.sms.StorageManagementHomeImpl;

/**
 * storage access
 * 
 * @author schuller
 */
public class StorageAccessFeature extends FeatureImpl {
	
	public StorageAccessFeature() {
		this.name = "StorageAccess";
	}

	@Override
	public void setKernel(Kernel kernel) {
		super.setKernel(kernel);
		homeClasses.put(UAS.SMS, StorageManagementHomeImpl.class);
		homeClasses.put(UAS.SMF, StorageFactoryHomeImpl.class);
		homeClasses.put(UAS.SERVER_FTS, FileTransferHomeImpl.class);
		homeClasses.put(UAS.CLIENT_FTS, FileTransferHomeImpl.class);
		getStartupTasks().add(new Startup(kernel));
	}
	
	@Override
	public void initialise() throws Exception {
		new UFTPStartupTask(kernel).run();
	}

	public class Startup implements StartupTask {

		private final Kernel kernel;

		public Startup(Kernel kernel){
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

	}


}
