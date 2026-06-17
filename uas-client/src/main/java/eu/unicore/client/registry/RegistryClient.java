package eu.unicore.client.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import eu.unicore.client.core.BaseServiceClient;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.uas.json.JSONUtil;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * connect to a Registry
 *  
 * @author schuller
 */
public class RegistryClient extends BaseServiceClient implements IRegistryClient {

	private static final Logger logger = Log.getLogger(Log.CLIENT, RegistryClient.class);

	private final static ServiceListFilter acceptAll = (epData) -> {
		return true;
	};

	public RegistryClient(String url, IClientConfiguration security, IAuthCallback auth) {
		super(url, security, auth);
	}

	@Override
	public List<String> listEntries(ServiceListFilter acceptFilter) throws Exception {
		List<String>endpoints = new ArrayList<>();
		JSONArray js = bc.getJSON().getJSONArray("entries");
		for(int i=0; i<js.length(); i++){
			JSONObject o = js.getJSONObject(i);
			try{
				Map<String,String>epData = JSONUtil.asMap(o);
				String ep = getEP(epData);
				if(acceptFilter!=null && !acceptFilter.accept(epData))continue;
				endpoints.add(ep);
			}catch(Exception ex){
				logger.debug("Could not convert entry "+o.toString(2));
			}
		}
		return endpoints;
	}

	@Override
	public List<String> listEntries() throws Exception {
		return listEntries(acceptAll);
	}

	@Override
	public List<String> listEntries(String type) throws Exception {
		return listEntries(new ServiceTypeFilter(type));
	}

	String connectionStatus = "OK";

	@Override
	public String getConnectionStatus() throws Exception {
		checkConnection();
		return connectionStatus;
	}

	@Override
	public boolean checkConnection() throws Exception {
		try {
			bc.getJSON();
		}catch(Exception ex) {
			connectionStatus = Log.createFaultMessage("FAILED", ex);
		}
		return true;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public IClientConfiguration getSecurity() {
		return security;
	}

	public IAuthCallback getAuth() {
		return auth;
	}

	public static String getEP(Map<String,String>content){
		return content.get(ENDPOINT);
	}

	public static final String ENDPOINT = "href";
	public static final String TYPE = "type";
	public static final String INTERFACE_NAME = "InterfaceName";
	public static final String SERVER_IDENTITY = "ServerIdentity";
	public static final String SERVER_PUBKEY = "ServerPublicKey";

	public static class ServiceTypeFilter implements ServiceListFilter {

		private final String type;

		public ServiceTypeFilter(String type){
			this.type = type;
		}

		@Override
		public boolean accept(Map<String,String>epData){
			return epData!=null &&
				type.equals(epData.get(INTERFACE_NAME)) || type.equals(epData.get(TYPE));
		}

	};

}
