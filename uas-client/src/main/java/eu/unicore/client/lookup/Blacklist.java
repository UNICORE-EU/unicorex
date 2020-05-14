package eu.unicore.client.lookup;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.BaseServiceClient;

/**
 * useful filter for excluding sites: if a service URL matches
 * one of the blacklist patterns, it won't be accepted
 *
 * @author schuller
 */
public class Blacklist implements AddressFilter {

	private final String[] patterns;

	public Blacklist(String[]patterns){
		this.patterns=patterns;
	}
		
	@Override
	public boolean accept(Endpoint epr) {
		return accept(epr.getUrl());
	}

	@Override
	public boolean accept(String uri) {
		if(patterns==null||patterns.length==0)return true;
		for(String p: patterns){
			if(p.contains("*")){
				if(uri.matches(p))return false;
			}
			if(uri.contains(p))return false;
		}
		return true;
	}

	@Override
	public boolean accept(BaseServiceClient client) {
		return true;
	}

}