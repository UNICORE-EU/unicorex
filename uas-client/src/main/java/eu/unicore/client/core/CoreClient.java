package eu.unicore.client.core;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import eu.unicore.client.utils.TaskClient;
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

	public CoreClient(String endpoint, IClientConfiguration security, IAuthCallback auth) {
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
		return new SiteFactoryClient(url, security, auth);
	}

	public EnumerationClient getJobsList() throws Exception {
		return new EnumerationClient(getLinkUrl("jobs"), security, auth);
	}

	public EnumerationClient getStoragesList() throws Exception {
		return new EnumerationClient(getLinkUrl("storages"), security, auth);
	}

	public EnumerationClient getTransfersList() throws Exception {
		return new EnumerationClient(getLinkUrl("transfers"), security, auth);
	}

	public StorageFactoryClient getStorageFactory(String type) throws Exception {
		if(type==null)type = "DEFAULT";
		return new StorageFactoryClient(getLinkUrl("storagefactories")+"/"+type, security, auth);
	}

	public List<StorageFactoryClient> getStorageFactories() throws Exception {
		List<StorageFactoryClient>res = new ArrayList<>();
		try(EnumerationClient ec = new EnumerationClient(getLinkUrl("storagefactories"), security, auth))
		{
			ec.forEach((smf)->{
				res.add(new StorageFactoryClient(smf, security, auth));
			});
			return res;
		}
	}

	public TaskClient launchFederatedSearch(JSONObject spec) throws Exception {
		String url = getLinkUrl("storages")+"/search";
		bc.pushURL(url);
		String taskURL = bc.create(spec);
		bc.popURL();
		return new TaskClient(taskURL, security, auth);
	}

}
