package de.fzj.unicore.uas.features;

import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.fts.FileTransferHomeImpl;
import de.fzj.unicore.uas.impl.job.JobManagementHomeImpl;
import de.fzj.unicore.uas.impl.reservation.ReservationManagementHomeImpl;
import de.fzj.unicore.uas.impl.sms.StorageFactoryHomeImpl;
import de.fzj.unicore.uas.impl.sms.StorageManagementHomeImpl;
import de.fzj.unicore.uas.impl.tss.TargetSystemFactoryHomeImpl;
import de.fzj.unicore.uas.impl.tss.TargetSystemHomeImpl;
import eu.unicore.services.Kernel;
import eu.unicore.services.utils.deployment.FeatureImpl;

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
