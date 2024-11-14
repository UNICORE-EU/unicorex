package eu.unicore.client.lookup;

import java.util.Collection;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.SiteClient;
import eu.unicore.client.registry.IRegistryClient;
import eu.unicore.security.wsutil.client.authn.ClientConfigurationProvider;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.uas.json.Builder;

public interface Broker {

	/**
	 * Select a matching site
	 */
	public SiteClient findTSS(IRegistryClient registry, 
			ClientConfigurationProvider configurationProvider, IAuthCallback auth,
			Builder builder, SiteSelectionStrategy strategy) 
			throws Exception;
	
	/**
	 * List those sites that can run the given job
	 */
	public Collection<Endpoint> listCandidates(IRegistryClient registry,
			ClientConfigurationProvider configurationProvider, IAuthCallback auth,
			Builder builder) 
			throws Exception;
	
	
	/**
	 * allows to select the "best" broker
	 */
	public int getPriority();
	
	/**
	 * allows user to select the broker
	 */
	public String getName();
	
}
