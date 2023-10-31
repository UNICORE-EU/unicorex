package eu.unicore.client.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.net.URIBuilder;
import org.json.JSONException;
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
			Map<String,String>queryParams = new HashMap<>();
			queryParams.put("fields", JSONUtil.toCommaSeparated(fields));
			return getProperties(queryParams);
		}
		else {
			return getProperties();
		}
	}

	/**
	 * generic GET with query params
	 *
	 * @param queryParams
	 */
	public JSONObject getProperties(Map<String,String> queryParams) throws Exception {
		URIBuilder ub = new URIBuilder(bc.getURL());
		ub.removeQuery();
		for(Map.Entry<String,String>e: queryParams.entrySet()) {
			ub.addParameter(e.getKey(), e.getValue());
		}
		bc.pushURL(ub.build().toString());
		JSONObject props = bc.getJSON();
		bc.popURL();
		return props;
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
		String url;
		try {
			url = getLinkUrl("action:"+name);
		} catch(JSONException e) {
			throw new IllegalArgumentException("No such action: "+name, e);
		}
		bc.pushURL(url);
		try {
			ClassicHttpResponse res = bc.post(params);
			bc.checkError(res);
			if(HttpStatus.SC_NO_CONTENT==res.getCode()) {
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
		return bc.asJSON(bc.put(properties));
	}
}