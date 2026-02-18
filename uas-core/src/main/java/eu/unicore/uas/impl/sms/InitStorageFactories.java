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
import eu.unicore.services.Resource;
import eu.unicore.services.exceptions.ResourceNotCreatedException;
import eu.unicore.services.exceptions.ResourceUnknownException;
import eu.unicore.services.registry.LocalRegistryClient;
import eu.unicore.services.rest.registry.RegistryHandler;
import eu.unicore.services.restclient.RegistryClient;
import eu.unicore.uas.UAS;
import eu.unicore.uas.UASProperties;
import eu.unicore.uas.impl.BaseInitParameters;
import eu.unicore.uas.util.LogUtil;
import eu.unicore.util.Log;

/**
 * Creates the configured instances of the StorageFactory service
 *
 * @author schuller
 */
public class InitStorageFactories implements Runnable {

	private static final Logger logger = LogUtil.getLogger(LogUtil.DATA, InitStorageFactories.class);

	private final Kernel kernel;

	public InitStorageFactories(Kernel kernel){
		this.kernel=kernel;
	}

	@Override
	public void run(){
		try{
			Home smfHome = kernel.getHome(UAS.SMF);
			for(String id: smfHome.getStore().getUniqueIDs()){
				try{
					if(!StorageFactoryHomeImpl.DEFAULT_SMF_NAME.equals(id)) {
						try(Resource smf = smfHome.getForUpdate(id)){
							smf.destroy();
						}
					}
				}catch(Exception e) {}
			}
			UASProperties props = kernel.getAttribute(UASProperties.class);
			Map<String, StorageDescription> factories = props.getStorageFactories();
			LockSupport ls = kernel.getPersistenceManager().getLockSupport();
			Lock smfLock = ls.getOrCreateLock(InitStorageFactories.class.getName());
			if(smfLock.tryLock()) try {
				for(String smfID: factories.keySet()) {
					BaseInitParameters init = new BaseInitParameters(smfID, TerminationMode.NEVER);
					Class<?>clazz = props.getClassValue(UASProperties.SMS_FACTORY_CLASS, StorageFactoryImpl.class);
					init.resourceClassName = clazz.getName();
					smfHome.createResource(init);
					logger.info("Added StorageFactory resource '{}' of type <{}>.", smfID, clazz.getName());
				}
			}finally{
				smfLock.unlock();
			}
			try{
				createDefaultStorageFactoryIfNotExists();
			} catch (Exception e) {
				throw new RuntimeException("Could not setup default storage factory.",e);
			}
		} catch (Exception e) {
			throw new RuntimeException("Could not setup storage factory service(s).",e);
		}
	}

	/**
	 * add a "default" storage factory if it does not yet exist
	 * @deprecated will be removed for UNICORE 11
	 */
	@Deprecated
	private void createDefaultStorageFactoryIfNotExists()throws ResourceNotCreatedException,PersistenceException{
		Home smfHome=kernel.getHome(UAS.SMF);
		LockSupport ls = kernel.getPersistenceManager().getLockSupport();
		Lock smfLock = ls.getOrCreateLock(InitStorageFactories.class.getName());
		String defaultSmfName = StorageFactoryHomeImpl.DEFAULT_SMF_NAME;
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
			publishEndpoint();
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

	private void publishEndpoint(){
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
