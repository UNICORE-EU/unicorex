package eu.unicore.client.core;

import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.utils.URIBuilder;
import org.json.JSONObject;

import de.fzj.unicore.uas.json.JSONUtil;
import eu.unicore.client.Endpoint;
import eu.unicore.services.rest.client.BaseClient;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.services.rest.client.UserPreferences;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * base class for RESTful UNICORE services clients
 * 
 * @author schuller
 */
public class BaseServiceClient {

	protected final Endpoint endpoint;

	protected final IClientConfiguration security;

	protected final IAuthCallback auth;

	protected final BaseClient bc;

	private long updateInterval=500;

	private long lastAccessed;

	public BaseServiceClient(Endpoint endpoint, IClientConfiguration security, IAuthCallback auth) {
		this.endpoint = endpoint;
		this.security = security;
		this.auth = auth;
		this.bc = createTransport(endpoint.getUrl(), security, auth);
	}

	protected BaseClient createTransport(String url, IClientConfiguration security, IAuthCallback auth){
		return new BaseClient(url, security, auth);
	}
	
	public IClientConfiguration getSecurityConfiguration() {
		return security;
	}
	
	public IAuthCallback getAuth() {
		return auth;
	}

	public UserPreferences getUserPreferences(){
		return bc.getUserPreferences();
	}
	
	public String getLinkUrl(String relation) throws Exception {
		return getProperties().getJSONObject("_links").getJSONObject(relation).getString("href");
	}
	
	JSONObject cachedProperties = null;
	
	public JSONObject getProperties() throws Exception {
		if(cachedProperties==null || System.currentTimeMillis()>lastAccessed+updateInterval){
			cachedProperties = bc.getJSON();
			lastAccessed = System.currentTimeMillis();
		}	
		return cachedProperties;
	}
	
	/**
	 * get only the named properties
	 */
	public JSONObject getProperties(String... fields) throws Exception {
		if(fields!=null && fields.length>0) {
			URIBuilder ub = new URIBuilder(bc.getURL());
			ub.removeQuery();
			ub.addParameter("fields",JSONUtil.toCommaSeparated(fields));
			bc.pushURL(ub.build().toString());
			JSONObject props = bc.getJSON();
			bc.popURL();
			return props;
		}
		else {
			return getProperties();
		}
	}
	
	public List<String> getTags() throws Exception {
		return JSONUtil.toList(getProperties().getJSONArray("tags"));
	}
	
	public long getUpdateInterval() {
		return updateInterval;
	}

	/**
	 * Sets the update interval, i.e. the minimum time between 
	 * subsequent remote calls to retrieve properties. 
	 * To disable the cache, set to a negative value
	 */
	public void setUpdateInterval(long updateInterval) {
		this.updateInterval = updateInterval;
	}
	
	public Endpoint getEndpoint() {
		return endpoint;
	}
	
	public JSONObject executeAction(String name, JSONObject params) throws Exception {
		String url = getLinkUrl("action:"+name);
		bc.pushURL(url);
		try {
			HttpResponse res = bc.post(params);
			bc.checkError(res);
			if(HttpStatus.SC_NO_CONTENT==res.getStatusLine().getStatusCode()) {
				return new JSONObject();
			}
			else {
				return bc.asJSON(res);
			}
		}finally {
			bc.popURL();
		}
	}
	
	/**
	 * deletes the resources
	 */
	public void delete() throws Exception {
		bc.delete();
	}
	
	/**
	 * set/update properties
	 * @param set - JSON doc containing key/value properties to set/update
	 * @return JSON doc containing success/failure info
	 */
	public JSONObject setProperties(JSONObject properties) throws Exception {
		HttpResponse res = bc.put(properties);
		bc.checkError(res);
		return bc.asJSON(res);
	}
}