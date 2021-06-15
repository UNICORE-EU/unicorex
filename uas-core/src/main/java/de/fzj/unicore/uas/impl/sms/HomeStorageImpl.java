package de.fzj.unicore.uas.impl.sms;

import eu.unicore.services.InitParameters;

/**
 * Represents a HOME storage. The storage root is the current user's 
 * home directory on the target system.
 *
 * @author schuller
 */
public class HomeStorageImpl extends PathedStorageImpl {

	@Override
	public void initialise(InitParameters initobjs)throws Exception{
		super.initialise(initobjs);
	}
	
	@Override
	protected String getDefaultWorkdir() {
		return "$HOME";
	}
}
