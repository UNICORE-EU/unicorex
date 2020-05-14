package eu.unicore.client.lookup;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.BaseServiceClient;

/**
 * filter for searching services by site name
 */
public class SiteNameFilter implements AddressFilter {

	private final String name;
	
	public SiteNameFilter(String name){
		this.name=name;
	}
	@Override
	public boolean accept(Endpoint epr) {
		return true;
	}

	@Override
	public boolean accept(String uri) {
		return true;
	}

	@Override
	public boolean accept(BaseServiceClient client) throws Exception {
		return name==null || client.getEndpoint().getUrl().contains("/"+name+"/");
	}
	
}