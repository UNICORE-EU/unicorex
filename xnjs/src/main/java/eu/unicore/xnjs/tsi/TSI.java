package eu.unicore.xnjs.tsi;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import eu.unicore.security.Client;
import eu.unicore.xnjs.ems.ExecutionContext;
import eu.unicore.xnjs.ems.ExecutionException;
import eu.unicore.xnjs.io.IStorageAdapter;

/**
 * The execution management system's interface to the target system<br/>
 * <p>
 * This interface allows to access and modify files, and call executables on the
 * target system.
 * </p>
 * <p>
 * File system access: the TSI interface is a superset of the {@link IStorageAdapter}, allowing
 * to perform storage operations on the file system
 * </p>
 * <p>
 * Implementation note: TSIs are NOT intended to be threadsafe, each thread should use its 
 * own TSI instance
 * </p>
 * 
 * @author schuller
 */
public interface TSI extends IStorageAdapter{

	/**
	 * is the target system's file system local to the TSI?
	 * 
	 * @return true if filesystem is local
	 */
	public boolean isLocal();

	/**
	 * get the HOME directory of the current client
	 * 
	 * @return the HOME directory
	 * @throws ExecutionException
	 */
	public String getHomePath() throws ExecutionException;
	
	/**
	 * get an environment variable for the current client
	 * 
	 * @param name -  the name of the environment variable to retrieve
	 * @return the value of the variable
	 * @throws ExecutionException
	 */
	public String getEnvironment(String name) throws ExecutionException;
	
	/**
	 * Resolve a name in the current environment, e.g.
	 * /tmp/$ENV/foo.txt
	 * 
	 * @param name -  the path name to resolve
	 * @return the resolved value
	 * @throws ExecutionException
	 */
	public String resolve(String name) throws ExecutionException;
	
	/**
	 * execute a command asynchronously
	 * 
	 * @param what the shell script to execute
	 * @param ec the ExecutionContext
	 * @throws TSIBusyException
	 * @throws ExecutionException
	 */
	public void exec(String what, ExecutionContext ec) throws TSIBusyException,ExecutionException;
	
	/**
	 * execute a command synchronously
	 * 
	 * @param what the shell script to execute
	 * @param ec the ExecutionContext
	 * @throws TSIBusyException
	 * @throws ExecutionException
	 */
	public void execAndWait(String what, ExecutionContext ec) throws TSIBusyException,ExecutionException;
	
	/**
	 * set the client for which this TSI is performing work
	 */
	public void setClient(Client client);
	
	/**
	 * get the groups the current client is in
	 * @return list of groups
	 */
	public String[]getGroups() throws TSIBusyException, ExecutionException;

	/**
	 * have the TSI make a connection to the service listening on the given host and port
	 * 
	 * @param host - hostname, or null if on the local TSI node
	 * @param serverPort
	 * @return a connected SocketChannel
	 * @throws TSIBusyException
	 * @throws IOException
	 */
	public SocketChannel openConnection(String host, int serverPort) throws Exception;
	
}
