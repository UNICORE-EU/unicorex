package de.fzj.unicore.uas.impl.tss.util;

import java.util.Collection;

import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.impl.tss.TargetSystemImpl;
import eu.unicore.services.ExtendedResourceStatus.ResourceStatus;
import eu.unicore.services.Kernel;
import eu.unicore.services.events.AsynchActionWithCallback;
import eu.unicore.util.Log;

/**
 * Used for asynchronous initialisation of new TSS instances.
 * When this task finishes, it will update the TSS's resource status.
 * 
 * @author schuller
 */
public class TSSAsynchInitialisation extends AsynchActionWithCallback<TargetSystemImpl> {

	public TSSAsynchInitialisation(Kernel kernel, final String resourceID, final Collection<Runnable> tasks) {
		super(new Runnable(){
			public void run(){
				for(Runnable r: tasks)r.run();
			}
		}, kernel.getHome(UAS.TSS), resourceID);
	}

	@Override
	public void taskFinished(TargetSystemImpl resource) {
		resource.setResourceStatus(ResourceStatus.READY);
		log.info("Finished init/update of TSS {}", resource.getUniqueID());
	}

	@Override
	public void taskFailed(TargetSystemImpl resource, RuntimeException ex) {
		String msg = Log.createFaultMessage("Failed init/update of TSS "+resource.getUniqueID(), ex);
		resource.setResourceStatus(ResourceStatus.ERROR);
		resource.setStatusMessage(msg);
		log.error("Failed init/update of TSS {}", resource.getUniqueID(),ex);
	}
}
