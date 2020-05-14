package de.fzj.unicore.uas.cdmi;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpMessage;

import eu.unicore.services.rest.client.IAuthCallback;

/**
 * Uses username and password to add a basic auth header to outgoing calls 
 * 
 * @author schuller
 */
public class BasicAuth implements IAuthCallback {

	private final String username;
	
	private final String password;
	
	public BasicAuth(String username, String password){
		this.username = username;
		this.password = password;
	}
	
	@Override
	public void addAuthenticationHeaders(HttpMessage httpMessage) throws Exception {
		httpMessage.setHeader("Authorization", "Basic " +
				new String(Base64.encodeBase64((username+":"+password).getBytes())));
	}

}
