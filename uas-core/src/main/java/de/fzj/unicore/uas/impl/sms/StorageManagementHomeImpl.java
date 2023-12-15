package de.fzj.unicore.uas.impl.sms;

import de.fzj.unicore.uas.util.LogUtil;
import eu.unicore.services.InitParameters;
import eu.unicore.services.Resource;
import eu.unicore.services.exceptions.ResourceNotCreatedException;
import eu.unicore.services.impl.DefaultHome;

/**
 * Storage service home. Depending on the passed-in init parameters, different
 * types of storage service instances can be created.
 * 
 * @author schuller
 */
public class StorageManagementHomeImpl extends DefaultHome {
	
	/**
	 * the types of storages</br>
	 * HOME: mapped to current user's home
	 * VARIABLE: actual path is looked up using the TSI, resolving any variables
	 * FIXEDPATH: mapped to a fixed path (e.g. "/opt/unicore/files")
	 * CUSTOM: other, needs a class to instantiate
	 */
	public static enum StorageTypes {
		HOME,
		FIXEDPATH,
		CUSTOM,
		VARIABLE
	}

	@Override
	protected Resource doCreateInstance(InitParameters initObjs) throws Exception {
		String clazz = initObjs.resourceClassName;
		return(Resource)(Class.forName(clazz).getConstructor().newInstance());
	}
	
	protected Resource doCreateInstance() throws Exception {
		throw new IllegalStateException();
	}
	
	@Override
	public  String createResource(InitParameters map) throws ResourceNotCreatedException {
		StorageInitParameters init = (StorageInitParameters)map;
		StorageDescription storageDesc = init.storageDescription;
		StorageTypes st=storageDesc.getStorageType();
		String clazz=null;
		if (st.equals(StorageTypes.HOME)){
			clazz=HomeStorageImpl.class.getName();
		}
		else if (st.equals(StorageTypes.FIXEDPATH)){
			clazz=FixedStorageImpl.class.getName();
		}
		else if (st.equals(StorageTypes.VARIABLE)){
			clazz=PathedStorageImpl.class.getName();
		}
		else if (st.equals(StorageTypes.CUSTOM)){
			clazz=storageDesc.getStorageClass().getName();
		}
		else{
			throw new ResourceNotCreatedException("Unknown storage type!?");
		}
		map.resourceClassName = clazz;
		return super.createResource(map);
	}
	
	@Override
	protected void postInitialise(Resource r){
		if(r instanceof SMSBaseImpl){
			try{
				((SMSBaseImpl)r).setupDirectoryScan();
			}catch(Exception ex){
				LogUtil.logException("Error setting up directory scan for storage "+r.getUniqueID(), ex, logger);
			}
		}
	}
	
}
