package de.fzj.unicore.uas.trigger;

import java.util.List;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.io.IStorageAdapter;
import eu.unicore.security.Client;

/**
 * 
 * An extension that allows to process multiple files in one chunk
 *
 * @author schuller
 */
public interface MultiFileAction extends Action {

	/**
	 * 
	 * @param storage - the parent storage
	 * @param files - the files
	 * @param client - the user
	 * @param xnjsConfig - the XNJS for executing things
	 */
	public void fire(IStorageAdapter storage, List<String> files, Client client, XNJS xnjsConfig) throws Exception;
	
}
