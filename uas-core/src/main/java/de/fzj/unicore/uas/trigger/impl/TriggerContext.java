package de.fzj.unicore.uas.trigger.impl;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.io.IStorageAdapter;
import eu.unicore.security.Client;

public interface TriggerContext {

	public IStorageAdapter getStorage();
	

	public Client getClient();


	public XNJS getXNJS();
}
