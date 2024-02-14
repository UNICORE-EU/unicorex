package eu.unicore.uas.trigger;

import eu.unicore.security.Client;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.io.IStorageAdapter;

/**
 * 
 * A TriggeredAction is executed by the triggering framework on a matching file
 *
 * @author schuller
 */
public interface TriggeredAction {

	public static final String FILE_NAME="UC_FILE_NAME";
	
	public static final String FILE_PATH="UC_FILE_PATH";
	
	public static final String CURRENT_DIR="UC_CURRENT_DIR";
	
	public static final String BASE_DIR="UC_BASE_DIR";
	
	/**
	 * run the action
	 *
	 * @param storage - the parent storage
	 * @param filePath - the file path
	 * @param client - the user
	 * @param xnjsConfig - the XNJS for executing things
	 */
	public String run(IStorageAdapter storage, String filePath, Client client, XNJS xnjsConfig) throws Exception;
	
}
