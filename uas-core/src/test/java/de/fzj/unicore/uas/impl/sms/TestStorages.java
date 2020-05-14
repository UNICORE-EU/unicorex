package de.fzj.unicore.uas.impl.sms;

import org.junit.Test;

import de.fzj.unicore.uas.Base;
import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.impl.sms.StorageManagementHomeImpl.StorageTypes;
import de.fzj.unicore.wsrflite.exceptions.ResourceNotCreatedException;

/**
 * tests related to storages and their creation
 */
public class TestStorages extends Base{

	@Test(expected=ResourceNotCreatedException.class)
	public void testEmptyPath()throws Exception{
		StorageManagementHomeImpl smsHome=(StorageManagementHomeImpl)kernel.getHome(UAS.SMS);
		StorageInitParameters init = new StorageInitParameters();
		//VARIABLE (and HOME) storages may not point to "/"
		//this test setup is a bit hackish, but localTS does not (yet) properly resolve
		StorageDescription sd = new StorageDescription("foo", "foo", StorageTypes.VARIABLE, null);
		sd.setPathSpec("/");
		sd.setDescription("bar");
		sd.setEnableTrigger(false);
		init.storageDescription = sd;
		smsHome.createResource(init);
	}
	
}
