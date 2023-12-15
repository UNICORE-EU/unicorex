package de.fzj.unicore.uas.impl.sms;

import eu.unicore.services.InitParameters;
import eu.unicore.services.Resource;
import eu.unicore.services.impl.DefaultHome;

/**
 * Storage factory home
 * 
 * @author schuller
 */
public class StorageFactoryHomeImpl extends DefaultHome {
	
	public static final String DEFAULT_SMF_NAME="default_storage_factory";

	@Override
	protected Resource doCreateInstance(InitParameters initObjs)
			throws Exception {
		String clazz = initObjs.resourceClassName;
		return(Resource)(Class.forName(clazz).getConstructor().newInstance());
	}

	@Override
	protected Resource doCreateInstance() throws Exception {
		throw new IllegalStateException();
	}
}
