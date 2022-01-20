package de.fzj.unicore.uas.features;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.fzj.unicore.uas.impl.task.TaskHomeImpl;
import de.fzj.unicore.uas.rest.CoreServices;
import eu.unicore.services.DeploymentDescriptor;
import eu.unicore.services.Home;
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

	@Override
	public Map<String, Class<? extends Home>> getHomeClasses(){
		Map<String, Class<? extends Home>> homeClasses = new HashMap<>();
		homeClasses.put("Task", TaskHomeImpl.class);
		return homeClasses;
	}
	
	@Override
	public List<DeploymentDescriptor> getServices(){
		List<DeploymentDescriptor> services = new ArrayList<>();
		services.add(new RESTCoreSD(kernel));		
		return services;
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
