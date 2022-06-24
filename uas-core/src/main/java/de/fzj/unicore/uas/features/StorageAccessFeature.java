package de.fzj.unicore.uas.features;

import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.fts.FileTransferHomeImpl;
import de.fzj.unicore.uas.fts.uftp.UFTPStartupTask;
import de.fzj.unicore.uas.impl.sms.StorageFactoryHomeImpl;
import de.fzj.unicore.uas.impl.sms.StorageManagementHomeImpl;
import eu.unicore.services.Kernel;
import eu.unicore.services.utils.deployment.FeatureImpl;

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
		getStartupTasks().add(new StorageAccessStartupTask(kernel));
	}
	
	@Override
	public void initialise() throws Exception {
		new UFTPStartupTask(kernel).run();
	}

}
