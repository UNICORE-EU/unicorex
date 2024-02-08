package eu.unicore.uas.notification;
import eu.unicore.services.Kernel;
import eu.unicore.services.rest.RestService;
import eu.unicore.services.utils.deployment.DeploymentDescriptorImpl;
import eu.unicore.services.utils.deployment.FeatureImpl;

/**
 * mock notification receiver service
 * 
 * @author schuller
 */
public class NotificationFeature extends FeatureImpl {

	public NotificationFeature() {
		this.name = "NotificationReceiver";
	}

	public void setKernel(Kernel kernel) {
		super.setKernel(kernel);
		services.add(new NotificationSD(kernel));	
		
	}

	public static class NotificationSD extends DeploymentDescriptorImpl {

		public NotificationSD(Kernel kernel){
			this();
			setKernel(kernel);
		}
		
		public NotificationSD() {
			super();
			this.name = "notification";
			this.type = RestService.TYPE;
			this.implementationClass = NotificationServices.class;
		}

	}
}
