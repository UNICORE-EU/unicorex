package de.fzj.unicore.uas.cdmi;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.json.JSONObject;

import de.fzj.unicore.wsrflite.Kernel;
import eu.emi.security.authn.x509.helpers.BinaryCertChainValidator;
import eu.unicore.services.rest.client.BaseClient;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.util.httpclient.ClientProperties;

/**
 * Uses username and password to get an auth token from
 * OpenStack Keystone. This token will be added to outgoing
 * messages via an "X-Auth-Token" header 
 * 
 * @author schuller
 */
public class KeystoneAuth implements IAuthCallback {

	private final String endpoint;

	private final String username;

	private final String password;

	private final String key;
	
	private final Kernel kernel;

	private static final Map<String,TokenCacheEntry>cache = new ConcurrentHashMap<>();
	
	// token cache time in millis : 10 minutes
	private long tokenCacheTime = 600*1000 ;

	public KeystoneAuth(String endpoint, String username, String password, Kernel kernel){
		this.endpoint = endpoint;
		this.username = username;
		this.password = password;
		this.kernel = kernel;
		this.key = endpoint+":"+username+":"+password;
	}

	public synchronized String getToken() throws Exception {
		TokenCacheEntry cachedToken = cache.get(key);
		if (cachedToken==null||cachedToken.isExpired()){
			cachedToken = getNewToken();
			cache.put(key, cachedToken);
		}
		return cachedToken.token;
	}

	protected TokenCacheEntry getNewToken() throws Exception {
		ClientProperties cp = kernel.getClientConfiguration().clone();
		cp.setSslAuthn(false);
		cp.setSslEnabled(endpoint.startsWith("https"));
		/* disable server cert verification */		
		cp.setValidator(new BinaryCertChainValidator(true)); 
		BaseClient client = buildClient(endpoint, cp);
		JSONObject request = new JSONObject();
		JSONObject auth = new JSONObject();
		auth.put("tenantName",username);
		JSONObject creds = new JSONObject();
		creds.put("username",username);
		creds.put("password",password);
		auth.put("passwordCredentials",creds);
		request.put("auth", auth);
		
		HttpResponse res = client.post(request);
		if(client.getLastHttpStatus()!=200){
			throw new Exception("Error retrieving credentials: "+client.getLastStatus());
		}
		JSONObject json = client.asJSON(res);
		Object token = json.getJSONObject("access").getJSONObject("token").get("id");
		TokenCacheEntry tce = new TokenCacheEntry();
		tce.token = String.valueOf(token);
		tce.expiryTime = System.currentTimeMillis()+tokenCacheTime;
		return tce;
	}

	@Override
	public void addAuthenticationHeaders(HttpMessage httpMessage)
			throws Exception {
		String token = getToken();
		httpMessage.setHeader("x-auth-token", token);
	}

	protected BaseClient buildClient(String endpoint, ClientProperties cp){
		return new BaseClient(endpoint, cp, null);
	}
	
	private static class TokenCacheEntry{
		String token;
		long expiryTime;
		public boolean isExpired(){
			return System.currentTimeMillis()>expiryTime;
		}
	}

}
