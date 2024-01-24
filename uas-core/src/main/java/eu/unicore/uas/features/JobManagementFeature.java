package eu.unicore.uas.features;

import eu.unicore.services.Kernel;
import eu.unicore.services.utils.deployment.FeatureImpl;
import eu.unicore.uas.UAS;
import eu.unicore.uas.fts.FileTransferHomeImpl;
import eu.unicore.uas.impl.job.JobManagementHomeImpl;
import eu.unicore.uas.impl.reservation.ReservationManagementHomeImpl;
import eu.unicore.uas.impl.sms.StorageFactoryHomeImpl;
import eu.unicore.uas.impl.sms.StorageManagementHomeImpl;
import eu.unicore.uas.impl.tss.TargetSystemFactoryHomeImpl;
import eu.unicore.uas.impl.tss.TargetSystemHomeImpl;

/**
 * job execution
 * 
 * @author schuller
 */
public class JobManagementFeature extends FeatureImpl {

	public JobManagementFeature() {
		this.name = "JobManagement";
	}

	public void setKernel(Kernel kernel) {
		super.setKernel(kernel);

		homeClasses.put(UAS.TSF, TargetSystemFactoryHomeImpl.class);
		homeClasses.put(UAS.TSS, TargetSystemHomeImpl.class);
		homeClasses.put(UAS.JMS, JobManagementHomeImpl.class);
		homeClasses.put(UAS.RESERVATIONS, ReservationManagementHomeImpl.class);

		homeClasses.put(UAS.SMS, StorageManagementHomeImpl.class);
		homeClasses.put(UAS.SMF, StorageFactoryHomeImpl.class);
		homeClasses.put(UAS.SERVER_FTS, FileTransferHomeImpl.class);
		homeClasses.put(UAS.CLIENT_FTS, FileTransferHomeImpl.class);

		getStartupTasks().add(new JobManagementStartupTask(kernel));
	}

}
