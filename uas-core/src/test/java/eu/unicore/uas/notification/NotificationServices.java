package eu.unicore.uas.notification;

import java.util.HashSet;
import java.util.Set;

import eu.unicore.services.Kernel;
import eu.unicore.services.rest.USERestApplication;
import jakarta.ws.rs.core.Application;

/**
 * REST app for the notification receiver
 * 
 * @author schuller
 */
public class NotificationServices extends Application implements USERestApplication {

	@Override
	public void initialize(Kernel kernel) throws Exception {}

	@Override
	public Set<Class<?>> getClasses() {
		Set<Class<?>>classes=new HashSet<>();
		classes.add(Notifications.class);
		return classes;
	}

	public static boolean isEnabled(Kernel kernel){
		return kernel.getService("notification")!=null;
	}
	
}
