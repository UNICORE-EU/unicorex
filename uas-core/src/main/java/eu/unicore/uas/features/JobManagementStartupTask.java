package eu.unicore.uas.features;

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
import eu.unicore.services.rest.registry.RegistryHandler;
import eu.unicore.services.restclient.RegistryClient;
import eu.unicore.uas.UAS;
import eu.unicore.uas.UASProperties;
import eu.unicore.uas.impl.BaseInitParameters;
import eu.unicore.uas.impl.tss.TargetSystemFactoryHomeImpl;
import eu.unicore.uas.impl.tss.TargetSystemFactoryImpl;
import eu.unicore.uas.xnjs.XNJSFacade;
import eu.unicore.util.Log;

/**
 * Startup code that creates the default TargetSystemFactory service
  */
public class JobManagementStartupTask implements Runnable{

	private static final Logger logger = Log.getLogger(Log.UNICORE,JobManagementStartupTask.class);

	private final Kernel kernel;

	public JobManagementStartupTask(Kernel kernel){
		this.kernel=kernel;
	}
	
	public void run(){
		try{
			createDefaultTSFIfNotExists();
		}catch(Exception re){
			throw new RuntimeException("Could not create default TSF instance.",re);
		}
	}

	/**
	 * add a "default" target system factory if it does not yet exist	
	 */
	protected void createDefaultTSFIfNotExists()throws ResourceNotCreatedException,PersistenceException{
		Home tsfHome=kernel.getHome(UAS.TSF);
		if(tsfHome==null){
			logger.info("No TSF service configured for this site!");
			return;
		}
		logger.info("Initialising backend.");
		XNJSFacade.get(null,kernel);
		String defaultTsfName=TargetSystemFactoryHomeImpl.DEFAULT_TSF;
		
		LockSupport ls=kernel.getPersistenceManager().getLockSupport();
		Lock tsfLock=ls.getOrCreateLock(JobManagementStartupTask.class.getName());
		if(tsfLock.tryLock()){
			try{
				//this will throw ResourceUnknowException if resource does not exist
				tsfHome.get(defaultTsfName);
			}
			catch(ResourceUnknownException e){
				doCreateTSF(tsfHome);
			}
			finally{
				tsfLock.unlock();
			}
			publishWS(defaultTsfName);
		}
	}

	private void doCreateTSF(Home tsfHome)throws ResourceNotCreatedException{
		String defaultTsfName=TargetSystemFactoryHomeImpl.DEFAULT_TSF;
		BaseInitParameters init = new BaseInitParameters(defaultTsfName, TerminationMode.NEVER);
		UASProperties props = kernel.getAttribute(UASProperties.class);
		Class<?>clazz = props.getClassValue(UASProperties.TSF_CLASS, TargetSystemFactoryImpl.class);
		init.resourceClassName = clazz.getName();
		tsfHome.createResource(init);
		logger.info("Added default TSF resource '{}' of type <{}>.", defaultTsfName, clazz.getName());
	}

	public String toString(){
		return getClass().getName();
	}

	private void publishWS(String uid){
		try{
			LocalRegistryClient lrc = kernel.getAttribute(RegistryHandler.class).getRegistryClient();
			Map<String,String> res = new HashMap<>();
			String endpoint = kernel.getContainerProperties().getContainerURL()+"/rest/core/factories/"+uid;
			res.put(RegistryClient.ENDPOINT, endpoint);
			res.put(RegistryClient.INTERFACE_NAME, "TargetSystemFactory");
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

