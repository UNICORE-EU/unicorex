package de.fzj.unicore.uas.lookup;

import org.w3.x2005.x08.addressing.EndpointReferenceType;

/**
 * used to accept/reject certain site/service addresses during lookup<br/>
 *
 * @author schuller
 */
public interface AddressFilter<T> {

	public boolean accept(EndpointReferenceType epr);
	
	public boolean accept(String uri);

	/**
	 * in some cases the filtering should be done not on address alone, 
	 * but on some properties of the service.
	 * 
	 * @param client - the client object
	 */
	public boolean accept(T client) throws Exception;
}
