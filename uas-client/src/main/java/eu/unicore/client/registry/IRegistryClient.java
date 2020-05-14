package eu.unicore.client.registry;

import java.util.List;

import eu.unicore.client.Endpoint;

public interface IRegistryClient {
	
	/**
	 * list services of the given type and matching the given filter
	 */
	public List<Endpoint> listEntries(ServiceListFilter acceptFilter) throws Exception;

	/**
	 * list the entries in this registry
	 */
	public abstract List<Endpoint> listEntries()throws Exception;
	
	/**
	 * list the entries in this registry matching the given service type / "interface name"
	 * @see Endpoint#getInterfaceName()
	 */
	public abstract List<Endpoint> listEntries(String type)throws Exception;
	
	/**
	 * check the connection status to the service
	 * 
	 * @return "OK" if connection is OK, 
	 *         an error message otherwise
	 */
	public String getConnectionStatus()throws Exception;	
	
	/**
	 * check the connection to the service
	 * @return true if service can be accessed
	 */
	public boolean checkConnection()throws Exception;	

	/**
	 * allows to specify custom filtering conditions on the service list
	 * returned by the registry client
	 */
	public static interface ServiceListFilter {
		public boolean accept(Endpoint endpoint);
	}
	
}
