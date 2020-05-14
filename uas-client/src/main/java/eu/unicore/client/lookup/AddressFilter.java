package eu.unicore.client.lookup;

import eu.unicore.client.Endpoint;

/**
 * used to accept/reject certain site/service addresses during lookup<br/>
 *
 * @author schuller
 */
public interface AddressFilter extends Filter {

	public boolean accept(Endpoint ep);
	
	public boolean accept(String uri);

}
