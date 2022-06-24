package de.fzj.unicore.uas.features;

import de.fzj.unicore.uas.impl.task.TaskHomeImpl;
import de.fzj.unicore.uas.rest.CoreServices;
import eu.unicore.services.Kernel;
import eu.unicore.services.rest.RestService;
import eu.unicore.services.utils.deployment.DeploymentDescriptorImpl;
import eu.unicore.services.utils.deployment.FeatureImpl;

/**
 * Generic services in UNICORE/X (REST core, task, ...)
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
		getStartupTasks().add(new Runnable() {
			public void run() {
				CoreServices.publish(kernel);
			}
		});
	}
		
	public static class RESTCoreSD extends DeploymentDescriptorImpl {

		public RESTCoreSD(Kernel kernel){
			this();
			setKernel(kernel);
		}
		
		public RESTCoreSD() {
			super();
			this.name = "core";
			this.type = RestService.TYPE;
			this.implementationClass = CoreServices.class;
		}

	}
	
}
