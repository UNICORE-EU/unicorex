package eu.unicore.uas.notification;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.services.Kernel;
import eu.unicore.services.registry.LocalRegistryClient;
import eu.unicore.services.registry.RegistryHandler;
import eu.unicore.services.registry.RegistryImpl;
import eu.unicore.services.rest.USERestApplication;
import eu.unicore.util.Log;
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
