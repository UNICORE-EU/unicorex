package de.fzj.unicore.uas.features;

import java.util.ArrayList;
import java.util.List;

import de.fzj.unicore.uas.Task;
import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.impl.task.TaskHomeImpl;
import de.fzj.unicore.uas.rest.CoreServices;
import eu.unicore.services.DeploymentDescriptor;
import eu.unicore.services.Kernel;
import eu.unicore.services.rest.RestService;
import eu.unicore.services.utils.deployment.DeploymentDescriptorImpl;
import eu.unicore.services.utils.deployment.FeatureImpl;
import eu.unicore.services.ws.cxf.CXFService;

/**
 * Generic services in UNICORE/X (REST core, task, ...)
 * 
 * @author schuller
 */
public class BaseFeature extends FeatureImpl {
	
	public BaseFeature() {
		this.name = "Base";
	}

	public List<DeploymentDescriptor> getServices(){
		List<DeploymentDescriptor> services = new ArrayList<>();
		
		services.add(new TaskSD(kernel));		
		
		services.add(new RESTCoreSD(kernel));		
		
		return services;
	}
	
	
	public static class TaskSD extends DeploymentDescriptorImpl {

		public TaskSD(Kernel kernel){
			this();
			setKernel(kernel);
		}
		
		public TaskSD() {
			super();
			this.name = UAS.TASK;
			this.type = CXFService.TYPE;
			this.implementationClass = TaskHomeImpl.class;
			this.interfaceClass = Task.class;
		}

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
