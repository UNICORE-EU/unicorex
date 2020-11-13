package eu.unicore.client.registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.oasisOpen.docs.wsrf.sg2.EntryType;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.json.JSONUtil;
import de.fzj.unicore.wsrflite.xmlbeans.WSUtilities;
import eu.unicore.client.Endpoint;
import eu.unicore.services.rest.client.BaseClient;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * connect to a Registry
 *  
 * @author schuller
 */
public class RegistryClient implements IRegistryClient {

	private static final Logger logger = Log.getLogger(Log.CLIENT, RegistryClient.class);

	private final Endpoint endpoint;

	private final IClientConfiguration security;

	private final IAuthCallback auth;

	protected final BaseClient bc;

	private final static ServiceListFilter acceptAll = new ServiceListFilter(){
		public boolean accept(Endpoint ep){return true;}
	};

	public RegistryClient(String url, IClientConfiguration security, IAuthCallback auth) {
		this.endpoint = new Endpoint(url);
		this.security = security;
		this.auth = auth;
		this.bc = createTransport(url, security, auth);
	}

	protected BaseClient createTransport(String url, IClientConfiguration security, IAuthCallback auth){
		return new BaseClient(endpoint.getUrl(), security, auth);
	}

	@Override
	public List<Endpoint> listEntries(ServiceListFilter acceptFilter) throws Exception {
		List<Endpoint>endpoints = new ArrayList<>();
		JSONArray js = bc.getJSON().getJSONArray("entries");
		for(int i=0; i<js.length(); i++){
			JSONObject o = js.getJSONObject(i);
			try{
				Endpoint ep = toEP(JSONUtil.asMap(o));
				if(acceptFilter!=null && !acceptFilter.accept(ep))continue;
				endpoints.add(ep);
			}catch(Exception ex){
				logger.debug("Could not convert entry "+o.toString(2));
			}
		}
		return endpoints;
	}

	@Override
	public List<Endpoint> listEntries() throws Exception {
		return listEntries(acceptAll);
	}

	@Override
	public List<Endpoint> listEntries(String type) throws Exception {
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

	public Endpoint getEndpoint() {
		return endpoint;
	}

	public IClientConfiguration getSecurity() {
		return security;
	}

	public IAuthCallback getAuth() {
		return auth;
	}

	public static Endpoint toEP(EntryType sgEntry){
		return toEP(sgEntry, false);
	}

	public static Endpoint toEP(Map<String,String>content){
		Endpoint ep = new Endpoint(content.get(ENDPOINT));
		ep.setInterfaceName(content.get(INTERFACE_NAME));
		ep.setServerPublicKey(content.get(SERVER_PUBKEY));
		ep.setServerIdentity(content.get(SERVER_IDENTITY));
		return ep;
	}

	public static Endpoint toEP(EntryType sgEntry, boolean forceWSRF){
		return toEP(parse(sgEntry.getMemberServiceEPR()));
	}

	public static Map<String,String> parse(EndpointReferenceType memberEPR){
		Map<String,String> res = new HashMap<>();
		String dn = WSUtilities.extractServerIDFromEPR(memberEPR);
		if(dn!=null){
			res.put(SERVER_IDENTITY,dn);
		}
		QName q = WSUtilities.extractInterfaceName(memberEPR);
		if(q!=null){
			res.put(INTERFACE_NAME,q.getLocalPart());
		}
		res.put(ENDPOINT, memberEPR.getAddress().getStringValue());
		String pem = WSUtilities.extractPublicKey(memberEPR);
		if(pem!=null){
			res.put(SERVER_PUBKEY,pem);
		}
		return res;
	}

	public static final String ENDPOINT = "href";
	public static final String INTERFACE_NAME = "type";
	public static final String SERVER_IDENTITY = "ServerIdentity";
	public static final String SERVER_PUBKEY = "ServerPublicKey";

	public static class ServiceTypeFilter implements ServiceListFilter {

		private final String type;

		public ServiceTypeFilter(String type){
			this.type = type;
		}

		public boolean accept(Endpoint ep){
			return ep!=null && type.equals(ep.getInterfaceName());
		}

	};

}
