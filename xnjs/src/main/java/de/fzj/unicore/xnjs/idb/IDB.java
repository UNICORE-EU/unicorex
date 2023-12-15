package de.fzj.unicore.xnjs.idb;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.resources.Resource;
import eu.unicore.security.Client;

/**
 * Allows to lookup information necessary for task incarnation
 * and for publishing applications and resources to the outside
 * 
 * @author schuller
 */
public interface IDB {

	/**
	 * return the {@link ApplicationInfo} for the specified app
	 * 
	 * @param name - application name
	 * @param version - application version (can be <code>null</code>)
	 * @param client - the current client
	 * @return the {@link ApplicationInfo} stored in the IDB
	 */
	public ApplicationInfo getApplication(String name, String version, Client client);

	/**
	 * list all apps available to the current client
	 * 
	 * @param client
	 * @return (non-live) collection of {@link ApplicationInfo}
	 */
	public Collection<ApplicationInfo> getApplications(Client client);

	/**
	 * return the configured partitions
	 */
	public List<Partition> getPartitions() throws ExecutionException;

	/**
	 * gets the named partition
	 */
	public Partition getPartition(String partition) throws ExecutionException;

	/**
	 * gets the default partition (named "default", or the first defined one)
	 */
	public default Partition getDefaultPartition() throws ExecutionException {
		return getPartition(null);
	}

	/**
	 * get the header that is used for building shell scripts for
	 * execution by the (classic) TSI
	 * 
	 * @return the template (or the default one if not defined)
	 */
	public String getScriptHeader();

	/**
	 * Get a TextInfo property</br>
	 * 
	 * @param name - the name of the property
	 * @return The value of the requested property, or null if property does not exist
	 */
	public String getTextInfo(String name);

	/**
	 * Get all TextInfo properties in a map keyed by property name</br>
	 *
	 * @return A map containing all properties
	 */
	public Map<String,String> getTextInfoProperties();

	/**
	 * get the definition of the named filesystem. This can contain variables, 
	 * so needs to be resolved in the current context.
	 * @see Incarnation#incarnatePath(String, String, de.fzj.unicore.xnjs.ems.ExecutionContext, Client)
	 * @return the file system definition from the IDB
	 */
	public String getFilespace(String key);

	/**
	 * Get the names of the configured file systems
	 */
	public String[] getFilesystemNames();

	/**
	 * get the time when the underlying information was last updated
	 * 
	 * @return the last update time
	 */
	public long getLastUpdateTime();
	
	/**
	 * get the available partition names (queue names) for the current client
	 */
	public  Resource getAllowedPartitions(Client c) throws ExecutionException;

}