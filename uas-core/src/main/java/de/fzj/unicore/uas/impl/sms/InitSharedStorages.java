package de.fzj.unicore.uas.impl.sms;

import java.util.Map;
import java.util.concurrent.locks.Lock;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.persist.PersistenceException;
import de.fzj.unicore.persist.impl.LockSupport;
import de.fzj.unicore.uas.StorageManagement;
import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.UASProperties;
import de.fzj.unicore.uas.util.DefaultOnStartup;
import de.fzj.unicore.uas.util.LogUtil;
import de.fzj.unicore.wsrflite.Home;
import de.fzj.unicore.wsrflite.InitParameters.TerminationMode;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.exceptions.ResourceNotCreatedException;
import de.fzj.unicore.wsrflite.security.ACLEntry;
import de.fzj.unicore.wsrflite.security.ACLEntry.MatchType;
import eu.unicore.security.OperationType;


/**
 * Creates the configured shared storage instances of the StorageManagement service
 * 
 * It is run from either the {@link DefaultOnStartup} class, or by adding the class name
 * to the container.onstartup.* property
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
		map.publishToRegistry = false;
		
		// do not resolve and/or create directory scan 
		// since we do not have a client context here
		map.skipResolve = true;
		
		if(desc.getSharedTriggerUser() == null){
			// triggering is per-user, so must disable it here
			desc.setEnableTrigger(false);
		}
		// allow user access via ACL
		map.acl.add(new ACLEntry(OperationType.modify, "user", MatchType.ROLE));
		home.createResource(map);
		DefaultOnStartup.publishWS(kernel, home.getServiceName(), id, StorageManagement.SMS_PORT);
		logger.info("Added shared Storage resource '"+id+"' "+desc);
	}

}
