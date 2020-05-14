package eu.unicore.client.data;

import org.apache.http.HttpResponse;
import org.json.JSONObject;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.BaseServiceClient;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.util.httpclient.IClientConfiguration;

public class FileClient extends BaseServiceClient {

	public FileClient(Endpoint endpoint, IClientConfiguration security, IAuthCallback auth) {
		super(endpoint, security, auth);
	}

	public void mkdir() throws Exception {
		HttpResponse res = bc.post(null);
		bc.checkError(res);
	}
	
	public void chmod(String unixPermissions) throws Exception {
		JSONObject o = new JSONObject();
		o.put("unixPermissions", unixPermissions);
		HttpResponse res = bc.put(o);
		bc.checkError(res);
	}

}
