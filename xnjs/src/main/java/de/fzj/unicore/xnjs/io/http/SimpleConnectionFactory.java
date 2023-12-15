package de.fzj.unicore.xnjs.io.http;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;

import eu.unicore.security.Client;

/**
 * produces multithreaded httpclients - does not use any custom security 
 * for https, except the usual Java security
 * 
 * @author schuller
 */
public class SimpleConnectionFactory implements IConnectionFactory {
	
	public SimpleConnectionFactory() {
		super();
	}

	public HttpClient getConnection(String url, Client client) {
		HttpClientBuilder clientBuilder = HttpClientBuilder.create();
		clientBuilder.setConnectionManager(getConnectionManager());
		return clientBuilder.build();
	}
	
	//singleton connection manager
	private static PoolingHttpClientConnectionManager connectionManager;
	
	private static synchronized PoolingHttpClientConnectionManager getConnectionManager(){
		if(connectionManager==null)connectionManager=new PoolingHttpClientConnectionManager();
		return connectionManager;
	}
}
