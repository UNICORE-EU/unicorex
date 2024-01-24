package eu.unicore.uas.impl.sms;

import java.util.Map;
import java.util.concurrent.locks.Lock;

import org.apache.logging.log4j.Logger;

import eu.unicore.persist.PersistenceException;
import eu.unicore.persist.impl.LockSupport;
import eu.unicore.security.OperationType;
import eu.unicore.services.Home;
import eu.unicore.services.InitParameters.TerminationMode;
import eu.unicore.services.Kernel;
import eu.unicore.services.exceptions.ResourceNotCreatedException;
import eu.unicore.services.security.ACLEntry;
import eu.unicore.services.security.ACLEntry.MatchType;
import eu.unicore.uas.UAS;
import eu.unicore.uas.UASProperties;
import eu.unicore.uas.features.StorageAccessStartupTask;
import eu.unicore.uas.util.LogUtil;


/**
 * Creates the configured shared storage instances of the StorageManagement service
 * 
 * It is run from the {@link StorageAccessStartupTask}
 * 
 * @author schuller
 */
public class InitSharedStorages implements Runnable{

	private static final Logger logger=LogUtil.getLogger(LogUtil.CONFIG,InitSharedStorages.class);

	private final Kernel kernel;

	public InitSharedStorages(Kernel kernel){
		this.kernel=kernel;
	}

	public void run(){
		try{
			createSharedStorages();
		} catch (Exception e) {
			throw new RuntimeException("Could not create shared storages.",e);
		}
	}

	@SuppressWarnings("deprecation")
	protected void createSharedStorages()throws ResourceNotCreatedException,PersistenceException{
		Home smsHome=kernel.getHome(UAS.SMS);
		if(smsHome==null){
			logger.info("No StorageManagement service configured for this site!");
			return;
		}
		//get "global" lock
		LockSupport ls=kernel.getPersistenceManager().getLockSupport();
		Lock lock=ls.getOrCreateLock(InitSharedStorages.class.getName());
		if(lock.tryLock()){
			try{
				// get configured storage descriptions
				UASProperties props = kernel.getAttribute(UASProperties.class);
				Map<String,StorageDescription> storageConfigurations = 
						props.parseStorages(UASProperties.SMS_SHARE_PREFIX_old, UASProperties.SMS_ENABLED_SHARES_old, false);
				if(storageConfigurations.size()>0) {
					logger.warn("DEPRECATION: Configuration uses properties <coreServices.sms.share.*> and <coreServices.sms.enabledShares>, "
							+ "please replace by <coreServices.sms.storage.*> and <coreServices.sms.enabledStorages>");
				}
				storageConfigurations.putAll(props.parseStorages(UASProperties.SMS_SHARE_PREFIX, 
						UASProperties.SMS_ENABLED_SHARES, false));
				
				for(Map.Entry<String,StorageDescription> entry: storageConfigurations.entrySet()){
					StorageDescription desc = entry.getValue();
					desc.setCleanup(false);
					doCreateSMS(smsHome, desc);
				}
			}finally{
				lock.unlock();
			}
		}
	}

	private void doCreateSMS(Home home, StorageDescription desc)throws ResourceNotCreatedException{
		String id = desc.getName();
		// sanity check the ID
		if(!id.matches("[-\\w]+"))throw new ResourceNotCreatedException("ID contains illegal characters! Only [a-z A-Z 0-9 - _] are allowed ");
		
		StorageInitParameters map = new StorageInitParameters(id, TerminationMode.NEVER);
		map.storageDescription = desc;

		// do not resolve and/or create directory scan 
		// since we do not have a client context here
		map.skipResolve = true;
		
		if(desc.getSharedTriggerUser() == null){
			// triggering is per-user, so must disable it here
			desc.setEnableTrigger(false);
		}
		// allow user access via ACL
		map.acl.add(new ACLEntry(OperationType.write, "user", MatchType.ROLE));
		home.createResource(map);
		logger.info("Added shared Storage resource '{}' {}", id, desc);
	}

}
