package eu.unicore.uas.impl.tss.util;

import java.util.Collection;

import org.apache.logging.log4j.Logger;

import eu.unicore.services.ExtendedResourceStatus;
import eu.unicore.services.ExtendedResourceStatus.ResourceStatus;
import eu.unicore.services.Kernel;
import eu.unicore.services.Resource;
import eu.unicore.uas.UAS;
import eu.unicore.uas.util.LogUtil;
import eu.unicore.util.Log;

/**
 * Used for asynchronous initialisation of new TSS instances.
 * When this task finishes, it will update the TSS's resource status.
 *
 * @author schuller
 */
public class TSSAsyncInitialisation implements Runnable{

	private final Logger logger = LogUtil.getLogger(LogUtil.SERVICES, TSSAsyncInitialisation.class);

	private final String resourceID;
	private final Kernel kernel;
	private final Collection<Runnable> tasks;

	public TSSAsyncInitialisation(Kernel kernel, final String resourceID, final Collection<Runnable> tasks) {
		this.resourceID = resourceID;
		this.kernel = kernel;
		this.tasks = tasks;
	}

	@Override
	public void run() {
		RuntimeException rte = null;
		try{ 
			for(Runnable r: tasks)r.run();
		}catch(RuntimeException e) {
			rte = e;
		}
		try(Resource r = kernel.getHome(UAS.TSS).getForUpdate(resourceID)){
			ExtendedResourceStatus resource = (ExtendedResourceStatus)r;
			if(rte==null) {
				resource.setResourceStatus(ResourceStatus.READY);
				logger.debug("Finished init/update of TSS {}", resourceID);
			}
			else {
				String msg = Log.createFaultMessage("Failed init/update of TSS "+resourceID, rte);
				resource.setResourceStatus(ResourceStatus.ERROR);
				resource.setResourceStatusMessage(msg);
				logger.error(msg);
			}
		}
	}
}