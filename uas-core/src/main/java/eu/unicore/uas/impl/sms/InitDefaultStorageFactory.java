package eu.unicore.uas.impl.sms;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import org.apache.logging.log4j.Logger;

import eu.unicore.persist.PersistenceException;
import eu.unicore.persist.impl.LockSupport;
import eu.unicore.services.Home;
import eu.unicore.services.InitParameters.TerminationMode;
import eu.unicore.services.Kernel;
import eu.unicore.services.exceptions.ResourceNotCreatedException;
import eu.unicore.services.exceptions.ResourceUnknownException;
import eu.unicore.services.registry.LocalRegistryClient;
import eu.unicore.services.registry.RegistryHandler;
import eu.unicore.services.rest.client.RegistryClient;
import eu.unicore.uas.UAS;
import eu.unicore.uas.UASProperties;
import eu.unicore.uas.impl.BaseInitParameters;
import eu.unicore.uas.util.LogUtil;
import eu.unicore.util.Log;

/**
 * Creates the "default" instance of the StorageFactory service
 * 
 * It is run from either the {@link DefaultOnStartup} class, or by adding the class name
 * to the container.onstartup.* property
 * 
 * @author schuller
 */
public class InitDefaultStorageFactory implements Runnable{

	private static final Logger logger = LogUtil.getLogger(LogUtil.DATA, InitDefaultStorageFactory.class);

	private final Kernel kernel;
	
	public InitDefaultStorageFactory(Kernel kernel){
		this.kernel=kernel;
	}
	
	public void run(){
		try{
			createDefaultStorageFactoryIfNotExists();
		} catch (Exception e) {
			throw new RuntimeException("Could not setup default storage factory.",e);
		}
	}

	/**
	 * add a "default" storage factory if it does not yet exist
	 */
	protected void createDefaultStorageFactoryIfNotExists()throws ResourceNotCreatedException,PersistenceException{
		Home smfHome=kernel.getHome(UAS.SMF);
		if(smfHome==null){
			logger.info("No StorageFactory service configured for this site!");
			return;
		}
		//get "global" lock
		LockSupport ls=kernel.getPersistenceManager().getLockSupport();
		Lock smfLock=ls.getOrCreateLock(InitDefaultStorageFactory.class.getName());
		String defaultSmfName=StorageFactoryHomeImpl.DEFAULT_SMF_NAME;
		if(smfLock.tryLock()){
			try{
				//this will throw ResourceUnknowException if resource does not exist
				smfHome.get(defaultSmfName);
			}
			catch(ResourceUnknownException e){
				doCreateSMF(smfHome);
			}finally{
				smfLock.unlock();
			}
			publishWS();
		}
	}

	private void doCreateSMF(Home smfHome)throws ResourceNotCreatedException{
		String defaultSmfName = StorageFactoryHomeImpl.DEFAULT_SMF_NAME;
		BaseInitParameters init = new BaseInitParameters(defaultSmfName, TerminationMode.NEVER);
		UASProperties props = kernel.getAttribute(UASProperties.class);
		Class<?>clazz = props.getClassValue(UASProperties.SMS_FACTORY_CLASS, StorageFactoryImpl.class);
		init.resourceClassName = clazz.getName();
		smfHome.createResource(init);
		logger.info("Added default StorageFactory resource '{}' of type <{}>.", defaultSmfName, clazz.getName());
	}

	private void publishWS(){
		try{
			LocalRegistryClient lrc = kernel.getAttribute(RegistryHandler.class).getRegistryClient();
			Map<String,String> res = new HashMap<>();
			String endpoint = kernel.getContainerProperties().getContainerURL()+"/rest/core/storagefactories/"
                              + StorageFactoryHomeImpl.DEFAULT_SMF_NAME;
			res.put(RegistryClient.ENDPOINT, endpoint);
			res.put(RegistryClient.INTERFACE_NAME, UAS.SMF);
			String dn = kernel.getSecurityManager().getServerIdentity();
			if(dn!=null) {
				res.put(RegistryClient.SERVER_IDENTITY,dn);
			}
			lrc.addEntry(endpoint, res, null);
		}catch(Exception ex){
			Log.logException("Could not publish to local registry", ex, logger);
		}
		
	}

}
