package eu.unicore.xnjs.tsi.remote;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.Map;

import eu.unicore.security.Client;
import eu.unicore.xnjs.tsi.TSIUnavailableException;

/**
 * Creates connections to a UNICORE TSI server
 * 
 * @author schuller
 */
public interface TSIConnectionFactory {

	/**
	 * return a connection that executes commands under the given user id.
	 * If a timeout is given (larger than zero), the factory waits for a connection to
	 * become available before attempting to create a new one.
	 * 
	 * @param user - user name (may never be null)
	 * @param group - group (may be null)
	 * @param preferredHost - the preferred TSI host (in case multiple hosts are available)
	 * @param timeoutMillis - timeout for waiting for a connection to become available.
	 * 
	 * @return a valid connection object or null in case of errors
	 * @throws TSIUnavailableException if TSI is down
	 * @throws IllegalArgumentException - if user is <code>null</code>
	 */
	public TSIConnection getTSIConnection(String user, String group, String preferredHost, int timeoutMillis)
			throws TSIUnavailableException;

	/**
	 * Return a connection that executes commands under the given user id. 
	 * If a timeout is given (larger than zero), the factory waits for a connection to
	 * become available before attempting to create a new one.
	 * 
	 * @param client - the {@link Client} for which to create the connection
	 * @param preferredHost - the preferred TSI host (in case multiple hosts are available)
	 * @param timeoutMillis - timeout for waiting for a connection to become available.
	 * @return a valid connection object or null in case of errors
	 */
	public TSIConnection getTSIConnection(Client client, String preferredHost, int timeoutMillis)
			throws TSIUnavailableException;

	/**
	 * notify that a connection was removed / died
	 */
	public void notifyConnectionDied();

	/**
	 * notify that the use of the connection is finished
	 *
	 * @param connection
	 */
	public void done(TSIConnection connection);
	
	/**
	 * get the version of the TSI server we are connected to
	 */
	public String getTSIVersion();

	/**
	 * TSI machine identifier
	 */
	public String getTSIMachine();

	/**
	 * TSI host names
	 */
	public Collection<String> getTSIHosts();

	/**
	 * TSI host categories (if defined)
	 */
	public Collection<String> getTSIHostCategories();

	/**
	 * Connection status overview message
	 */
	public String getConnectionStatus();

	/**
	 * get status messages for the individual TSIs we are connecting to
	 */
	public Map<String,String> getTSIConnectorStates();

	/**
	 * returns true if the TSIConnectionFactory is in an operational state
	 */
	public boolean isRunning();
	
	
	/**
	 * connect to a service listening on the given address
	 * via a TSI node (= port forwarding). The address can be
	 * of the form "host:port" for a normal TCP socket,
	 * or "file:socketfile" for a UNIX domain socket
	 * 
	 * @param address - service to connect to
	 * @param tsiHost - the TSI node to use (can be null, if that makes sense)
	 * @param user
	 * @param group
	 * @return connected SocketChannel
	 * @throws TSIUnavailableException
	 * @throws IOException
	 */
	public SocketChannel connectToService(String address, String tsiHost, String user, String group)
			throws TSIUnavailableException, IOException;

	/**
	 * shutdown
	 */
	public void stop();
	
}
