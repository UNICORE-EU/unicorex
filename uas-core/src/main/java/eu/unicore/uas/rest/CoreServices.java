package eu.unicore.uas.rest;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.services.Kernel;
import eu.unicore.services.registry.LocalRegistryClient;
import eu.unicore.services.registry.RegistryImpl;
import eu.unicore.services.rest.USERestApplication;
import eu.unicore.services.rest.registry.RegistryHandler;
import eu.unicore.services.security.pdp.DefaultPDP;
import eu.unicore.uas.UAS;
import eu.unicore.util.Log;
import jakarta.ws.rs.core.Application;

/**
 * REST app dealing with core services
 * 
 * @author schuller
 */
public class CoreServices extends Application implements USERestApplication {

	@Override
	public void initialize(Kernel kernel) throws Exception {
		DefaultPDP pdp = DefaultPDP.get(kernel);
		if(pdp!=null) {
			pdp.setServiceRules("core",
					DefaultPDP.PERMIT_READ,
					DefaultPDP.PERMIT_POST_FOR_USER);
			pdp.setServiceRules(UAS.SMF, DefaultPDP.PERMIT_READ, DefaultPDP.PERMIT_POST_FOR_USER);
			pdp.setServiceRules(UAS.TSF, DefaultPDP.PERMIT_READ, DefaultPDP.PERMIT_POST_FOR_USER);
		}
	}

	@Override
	public Set<Class<?>> getClasses() {
		Set<Class<?>>classes  = new HashSet<>();
		classes.add(Base.class);
		classes.add(Sites.class);
		classes.add(SiteFactories.class);
		classes.add(Storages.class);
		classes.add(StorageFactories.class);
		classes.add(Jobs.class);
		classes.add(Reservations.class);
		classes.add(Transfers.class);
		classes.add(ClientTransfers.class);
		classes.add(Tasks.class);
		return classes;
	}

	public static boolean isEnabled(Kernel kernel){
		return kernel.getService("core")!=null;
	}

	public static void publish(Kernel kernel){
		if(isEnabled(kernel)) try{
			LocalRegistryClient lrc = kernel.getAttribute(RegistryHandler.class).getRegistryClient();
			String endpoint = kernel.getContainerProperties().getContainerURL()+"/rest/core";
			Map<String,String>content = new HashMap<>();
			content.put(RegistryImpl.INTERFACE_NAME, "CoreServices");
			content.put(RegistryImpl.INTERFACE_NAMESPACE, "https://www.unicore.eu/rest");
			X509Credential cred = kernel.getContainerSecurityConfiguration().getCredential();
			if(cred!=null){
				StringWriter out = new StringWriter();
				try(JcaPEMWriter writer = new JcaPEMWriter(out)){
					writer.writeObject(cred.getCertificate());
				}catch(Exception ex){
					Log.logException("Cannot write public key", ex, Log.getLogger("unicore.security", CoreServices.class));
				}
				content.put(RegistryImpl.SERVER_PUBKEY, out.toString());
				content.put(RegistryImpl.SERVER_IDENTITY, cred.getSubjectName());
				
			}
			lrc.addEntry(endpoint, content, null);
		}catch(Exception ex){
			Log.logException("Could not publish to registry", ex);
		}
	}

}
