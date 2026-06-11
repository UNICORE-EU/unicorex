package eu.unicore.client.lookup;

/**
 * used to accept/reject certain site/service addresses during lookup<br/>
 *
 * @author schuller
 */
public interface AddressFilter extends Filter {

	public boolean accept(String uri);

}
