package de.fzj.unicore.uas.lookup;

import org.w3.x2005.x08.addressing.EndpointReferenceType;

/**
 * useful filter for excluding sites: if a service URL matches
 * one of the blacklist patterns, it won't be accepted
 *
 * @author schuller
 */
public class Blacklist<T> implements AddressFilter<T>{

	private final String[] patterns;

	public Blacklist(String[]patterns){
		this.patterns=patterns;
	}
		
	@Override
	public boolean accept(EndpointReferenceType epr) {
		return accept(epr.getAddress().getStringValue());
	}

	@Override
	public boolean accept(String uri) {
		if(patterns==null||patterns.length==0)return true;
		for(String p: patterns){
			if(uri.contains(p))return false;
		}
		return true;
	}

	@Override
	public boolean accept(T client) throws Exception {
		return true;
	}

}