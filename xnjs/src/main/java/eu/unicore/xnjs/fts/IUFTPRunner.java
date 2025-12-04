package eu.unicore.xnjs.fts;

import eu.unicore.security.Client;

public interface IUFTPRunner {

	/** set a (human-friendly) ID of this operation **/
	public void setID(String ID);

	/** the ID of the parent action **/
	public void setParentActionID(String actionID);

	public void setClient(Client client);

	/** the client host / tsi node to use **/
	public void setClientHost(String clientHost);

	/**
	 * perform download
	 *
	 * @param from
	 * @param to
	 * @param workdir
	 * @param host
	 * @param port
	 * @param secret
	 * @throws Exception
	 */
	public void get(String from, String to, String workdir, String host, int port, String secret) throws Exception;

	/**
	 * perform upload
	 *
	 * @param from
	 * @param to
	 * @param workdir
	 * @param host
	 * @param port
	 * @param secret
	 * @throws Exception
	 */
	public void put(String from, String to, String workdir, String host, int port, String secret) throws Exception;

	/** the internal action ID (mostly useful to get error logs and such **/
	public String getSubactionID();

}