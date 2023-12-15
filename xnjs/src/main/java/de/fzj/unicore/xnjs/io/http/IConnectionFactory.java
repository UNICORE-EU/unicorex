package de.fzj.unicore.xnjs.io.http;

import org.apache.hc.client5.http.classic.HttpClient;

import eu.unicore.security.Client;

/**
 * Used to create a {@link HttpClient} for a given URL.
 * 
 * @author schuller
 */
public interface IConnectionFactory {
	
	/**
	 * Create a new HttpClient instance
	 * 
	 * @param url -  the URL to connect to
	 * @param client - the {@link Client} that may contain additional security information
	 * @return a {@link HttpClient} to talk to the given URL
	 */
	public HttpClient getConnection(String url, Client client);

}
