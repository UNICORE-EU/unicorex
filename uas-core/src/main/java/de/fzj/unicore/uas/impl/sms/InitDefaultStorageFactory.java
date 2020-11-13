package de.fzj.unicore.uas.impl.sms;

import java.util.concurrent.locks.Lock;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.persist.PersistenceException;
import de.fzj.unicore.persist.impl.LockSupport;
import de.fzj.unicore.uas.StorageFactory;
import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.UASProperties;
import de.fzj.unicore.uas.impl.BaseInitParameters;
import de.fzj.unicore.uas.impl.UASWSResourceImpl;
import de.fzj.unicore.uas.util.DefaultOnStartup;
import de.fzj.unicore.uas.util.LogUtil;
import de.fzj.unicore.wsrflite.Home;
import de.fzj.unicore.wsrflite.InitParameters.TerminationMode;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.exceptions.ResourceNotCreatedException;
import de.fzj.unicore.wsrflite.exceptions.ResourceUnknownException;

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
				try{
					//this will throw ResourceUnknowException if resource does not exist
					smfHome.get(defaultSmfName);
					UASWSResourceImpl smf=(UASWSResourceImpl)smfHome.get(defaultSmfName);
					//It exists, so force re-publish
					smf.publish();
					//exists, so we are done
					return;
				}
				catch(ResourceUnknownException e){}
				doCreateSMF(smfHome);
			}finally{
				smfLock.unlock();
			}
		}
	}

	private void doCreateSMF(Home smfHome)throws ResourceNotCreatedException{
		String defaultSmfName = StorageFactoryHomeImpl.DEFAULT_SMF_NAME;
		BaseInitParameters init = new BaseInitParameters(defaultSmfName, TerminationMode.NEVER);
		init.publishToRegistry = true;
		UASProperties props = kernel.getAttribute(UASProperties.class);
		Class<?>clazz = props.getClassValue(UASProperties.SMS_FACTORY_CLASS, StorageFactory.class);
		init.resourceClassName = clazz.getName();
		smfHome.createResource(init);
		logger.info("Added default StorageFactory resource '"+defaultSmfName+"' of type <"+clazz.getName()+">.");
	}

}
