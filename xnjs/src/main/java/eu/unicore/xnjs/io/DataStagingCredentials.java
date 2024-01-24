package eu.unicore.xnjs.io;

import java.io.Serializable;

import eu.unicore.security.Client;

/**
 * marker interface for data staging credentials
 * 
 * @author schuller
 */
public interface DataStagingCredentials extends Serializable {

	/**
	 * get a value for use as "Authorization:" header, e.g. "Bearer token_value"
	 * when using an OAuth token
	 */
	public String getHTTPAuthorizationHeader(Client client);
	
}
