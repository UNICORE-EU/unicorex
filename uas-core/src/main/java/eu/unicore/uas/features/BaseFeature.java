package eu.unicore.uas.features;

import eu.unicore.services.Kernel;
import eu.unicore.services.StartupTask;
import eu.unicore.services.rest.RestService;
import eu.unicore.services.security.pdp.DefaultPDP;
import eu.unicore.services.security.pdp.UnicoreXPDP;
import eu.unicore.services.utils.deployment.DeploymentDescriptorImpl;
import eu.unicore.services.utils.deployment.FeatureImpl;
import eu.unicore.uas.impl.task.TaskHomeImpl;
import eu.unicore.uas.rest.CoreServices;

/**
 * Feature providing generic services in UNICORE/X (REST core, task, ...)
 * 
 * @author schuller
 */
public class BaseFeature extends FeatureImpl {

	public BaseFeature() {
		this.name = "Base";
	}

	public void setKernel(Kernel kernel) {
		super.setKernel(kernel);
		homeClasses.put("Task", TaskHomeImpl.class);
		services.add(new RESTCoreSD(kernel));
		getStartupTasks().add(new Startup(kernel));
	}

	public static class Startup implements StartupTask {

		private final Kernel kernel;

		public Startup(Kernel kernel) {
			this.kernel = kernel;
		}

		@Override
		public void run() {
			CoreServices.publish(kernel);
		}
	}

	public static class RESTCoreSD extends DeploymentDescriptorImpl {
		public RESTCoreSD(Kernel kernel){
			super();
			this.name = "core";
			this.type = RestService.TYPE;
			this.implementationClass = CoreServices.class;
			setKernel(kernel);
		}
	}

	/**
	 * get the configured DefaultPDP for the kernel, or null if not available
	 * TODO should be in USE, remove this once it is
	 */
	public static DefaultPDP get(Kernel kernel) {
		UnicoreXPDP pdp = kernel.getSecurityManager().getPdp();
		if(pdp!=null && pdp instanceof DefaultPDP) {
			return (DefaultPDP)pdp;
		}
		else return null;
	}
}