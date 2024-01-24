package eu.unicore.xnjs.io.impl;

import eu.unicore.security.Client;
import eu.unicore.xnjs.io.DataStagingCredentials;

/**
 * holds "Authorization: Token ..." value
 * 
 * @author schuller
 */
public class AuthToken implements DataStagingCredentials {

	private static final long serialVersionUID = 1l;
	
	private final String token;
	
	public AuthToken(String token){
		this.token = token;
	}

	public String getToken() {
		return token;
	}

	@Override
	public String getHTTPAuthorizationHeader(Client client){
		return "Token "+token;
	}
}
