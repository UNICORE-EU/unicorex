package eu.unicore.uas.trigger;

import java.util.List;

import eu.unicore.security.Client;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.io.IStorageAdapter;

/**
 * 
 * An extension that allows to process multiple files in one chunk
 *
 * @author schuller
 */
public interface MultiFileTriggeredAction extends TriggeredAction {

	/**
	 * 
	 * @param storage - the parent storage
	 * @param files - the files
	 * @param client - the user
	 * @param xnjsConfig - the XNJS for executing things
	 */
	public void fire(IStorageAdapter storage, List<String> files, Client client, XNJS xnjsConfig) throws Exception;
	
}
