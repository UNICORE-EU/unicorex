package de.fzj.unicore.xnjs.io.impl;

import de.fzj.unicore.xnjs.io.DataStagingCredentials;
import eu.unicore.security.Client;

/**
 * holds OAuth bearer token
 * 
 * @author schuller
 */
public class OAuthToken implements DataStagingCredentials {

	private static final long serialVersionUID = 1l;
	
	private final String token;
	
	public static String OAUTH_TOKEN = "UC_OAUTH_BEARER_TOKEN";
	
	public OAuthToken(String token){
		this.token = token;
	}

	public String getToken() {
		return token;
	}

	@Override
	public String getHTTPAuthorizationHeader(Client client){
		String tok = token;
		if((token == null || token.isEmpty()) && client != null){
			tok = client.getExtraAttributes().get(OAUTH_TOKEN);
		}
		return "Bearer "+tok;
	}
}
