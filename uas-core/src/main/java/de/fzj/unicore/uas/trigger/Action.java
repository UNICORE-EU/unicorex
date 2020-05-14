package de.fzj.unicore.uas.trigger;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.io.IStorageAdapter;
import eu.unicore.security.Client;

/**
 * 
 * An Action is executed by the triggering framework on a matching file
 *
 * @author schuller
 */
public interface Action {

	public static final String FILE_NAME="UC_FILE_NAME";
	
	public static final String FILE_PATH="UC_FILE_PATH";
	
	public static final String CURRENT_DIR="UC_CURRENT_DIR";
	
	public static final String BASE_DIR="UC_BASE_DIR";
	
	/**
	 * 
	 * @param storage - the parent storage
	 * @param filePath - the file path
	 * @param client - the user
	 * @param xnjsConfig - the XNJS for executing things
	 */
	public void fire(IStorageAdapter storage, String filePath, Client client, XNJS xnjsConfig) throws Exception;
	
}
