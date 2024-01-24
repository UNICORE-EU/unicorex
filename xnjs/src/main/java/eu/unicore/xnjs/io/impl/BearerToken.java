package eu.unicore.xnjs.io.impl;

import eu.unicore.security.Client;
import eu.unicore.xnjs.io.DataStagingCredentials;

/**
 * holds "Authorization: Bearer ..." value
 * 
 * @author schuller
 */
public class BearerToken implements DataStagingCredentials {

	private static final long serialVersionUID = 1l;
	
	private final String token;
	
	public static String OAUTH_TOKEN = "UC_OAUTH_BEARER_TOKEN";
	
	public BearerToken(String token){
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
