package eu.unicore.client.core;

import java.util.Calendar;
import java.util.Map;

import org.json.JSONObject;

import eu.unicore.client.Endpoint;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.uas.json.JSONUtil;
import eu.unicore.uas.util.UnitParser;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * allows to create storage instances via the storage factory service
 * 
 * @author schuller
 */
public class StorageFactoryClient extends BaseServiceClient {

	public StorageFactoryClient(Endpoint endpoint, IClientConfiguration security, IAuthCallback auth) {
		super(endpoint, security, auth);
	}

	public StorageClient createStorage() throws Exception {
		return createStorage(null,null,null,null);
	}

	public StorageClient createStorage(String type, String name, Map<String,String> parameters, Calendar tt) throws Exception {
		JSONObject json = new JSONObject();
		if(type!=null)json.put("type", type);
		if(name!=null)json.put("name", name);
		if(tt!=null) {
			json.put("terminationTime", UnitParser.getISO8601().format(tt.getTime()));
		}
		json.put("parameters", JSONUtil.asJSON(parameters));
		String url = bc.create(json);
		return new StorageClient(new Endpoint(url), security, auth);
	}

}
