package eu.unicore.client.data;

import org.json.JSONObject;

import eu.unicore.client.Endpoint;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * server-server transfers
 * 
 * @author schuller
 */
public class TransferControllerClient extends FiletransferClient {
	
	// same as server-side TransferInfo.Status
	public static enum Status {
		 CREATED,
		 RUNNING,
		 PAUSED,
		 DONE,
		 FAILED,
		 ABORTED
	}
	
	public TransferControllerClient(Endpoint endpoint, IClientConfiguration security, IAuthCallback auth) {
		super(endpoint, new JSONObject(), security, auth);
	}
	
	public Status getStatus() throws Exception {
		return Status.valueOf(getProperties().getString("status"));
	}
	
	public String getStatusMessage() throws Exception {
		return getProperties().getString("statusMessage");
	}
	
	public Long getSize() throws Exception {
		return getProperties().optLong("size", -1);
	}

	public boolean hasFailed() throws Exception {
		return Status.FAILED.equals(getStatus());
	}
	
	public boolean isComplete() throws Exception {
		Status s = getStatus();
		return Status.DONE.equals(s) || Status.FAILED.equals(s) || Status.ABORTED.equals(s);
	}
}
