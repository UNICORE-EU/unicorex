package eu.unicore.uas.trigger.impl;

import eu.unicore.security.Client;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.io.IStorageAdapter;

public interface TriggerContext {

	public IStorageAdapter getStorage();
	

	public Client getClient();


	public XNJS getXNJS();
}
