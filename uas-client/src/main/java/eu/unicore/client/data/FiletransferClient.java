package eu.unicore.client.data;

import org.json.JSONObject;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.BaseServiceClient;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.util.httpclient.IClientConfiguration;

public abstract class FiletransferClient extends BaseServiceClient {

	public FiletransferClient(Endpoint endpoint, JSONObject initialProperties, IClientConfiguration security, IAuthCallback auth) {
		super(endpoint, security, auth);
	}

	public abstract Long getTransferredBytes() throws Exception;

	public String getProtocol() throws Exception {
		return getProperties().getString("protocol");
	}
}