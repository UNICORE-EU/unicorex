package eu.unicore.client.core;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import eu.unicore.client.Endpoint;
import eu.unicore.security.AuthenticationException;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.uas.json.JSONUtil;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * client for the UNICORE/X core services endpoint
 *  
 * @author schuller
 */
public class CoreClient extends BaseServiceClient {

	public CoreClient(Endpoint endpoint, IClientConfiguration security, IAuthCallback auth) {
		super(endpoint, security, auth);
	}

	public JSONObject getClientInfo() throws Exception {
		return getProperties().getJSONObject("client");
	}

	public String getUID() throws AuthenticationException {
		try{
			return getClientInfo().getJSONObject("xlogin").getString("uid");
		}catch(Exception e){
			throw new AuthenticationException();
		}
	}

	public String[] getUIDs() throws AuthenticationException {
		try{
			JSONObject json = getClientInfo().getJSONObject("xlogin");
			return JSONUtil.toArray(json.getJSONArray("availableUIDs"));
		}catch(Exception e){
			throw new AuthenticationException();
		}
	}

	public String getGroup() throws AuthenticationException {
		try{
			return getClientInfo().getJSONObject("xlogin").getString("group");
		}catch(Exception e){
			throw new AuthenticationException();
		}
	}

	public String[] getGroups() throws AuthenticationException {
		try{
			JSONObject json = getClientInfo().getJSONObject("xlogin");
			return JSONUtil.toArray(json.getJSONArray("availableGroups"));
		}catch(Exception e){
			throw new AuthenticationException();
		}
	}

	/**
	 * get a SiteClient for submitting jobs
	 */
	public SiteClient getSiteClient() throws Exception {
		return getSiteFactoryClient().getOrCreateSite();
	}

	public SiteFactoryClient getSiteFactoryClient() throws Exception {
		String url = getLinkUrl("factories")+"/default_target_system_factory";
		return new SiteFactoryClient(endpoint.cloneTo(url), security, auth);
	}

	public EnumerationClient getJobsList() throws Exception {
		String url = getLinkUrl("jobs");
		return new EnumerationClient(endpoint.cloneTo(url), security, auth);
	}

	public EnumerationClient getStoragesList() throws Exception {
		String url = getLinkUrl("storages");
		return new EnumerationClient(endpoint.cloneTo(url), security, auth);
	}

	public EnumerationClient getTransfersList() throws Exception {
		String url = getLinkUrl("transfers");
		return new EnumerationClient(endpoint.cloneTo(url), security, auth);
	}

	public StorageFactoryClient getStorageFactory() throws Exception {
		String url = getLinkUrl("storagefactories");
		Endpoint ep = endpoint.cloneTo(url+"/default_storage_factory");
		return new StorageFactoryClient(ep, security, auth);
	}

	public List<StorageFactoryClient> getStorageFactories() throws Exception {
		List<StorageFactoryClient>res = new ArrayList<>();
		String url = getLinkUrl("storagefactories");
		EnumerationClient ec = new EnumerationClient(endpoint.cloneTo(url), security, auth);
		ec.forEach((smf)->{
			res.add(new StorageFactoryClient(endpoint.cloneTo(smf), security, auth));
		});
		return res;
	}
}
