package eu.unicore.client.data;

import org.json.JSONObject;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.BaseServiceClient;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.util.httpclient.IClientConfiguration;

public abstract class FiletransferClient extends BaseServiceClient {

	public FiletransferClient(Endpoint endpoint, JSONObject initialProperties, IClientConfiguration security, IAuthCallback auth) {
		super(endpoint, security, auth);
	}
	
	public Long getTransferredBytes() throws Exception {
		return Long.parseLong(getProperties().getString("transferredBytes"));
	}

	public String getProtocol() throws Exception {
		return getProperties().getString("protocol");
	}
}
