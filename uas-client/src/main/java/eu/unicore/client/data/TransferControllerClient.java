package eu.unicore.client.data;

import java.util.concurrent.TimeoutException;

import org.json.JSONObject;

import eu.unicore.client.Endpoint;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * Monitor and control server-server transfers
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

	public Long getTransferredBytes() throws Exception{
		return getProperties().optLong("transferredBytes", -1);
	}

	public boolean hasFailed() throws Exception {
		return Status.FAILED.equals(getStatus());
	}

	public boolean isComplete() throws Exception {
		Status s = getStatus();
		return Status.DONE.equals(s) || Status.FAILED.equals(s) || Status.ABORTED.equals(s);
	}

	/**
	 * wait for the transfer to reach the given status (or a later one)
	 *
	 * @param status - the status to wait for
	 */
	public void poll(Status status) throws Exception {
		poll(status, -1);
	}

	/**
	 * wait for the transfer to reach the given status (or a later one)
	 *
	 * @param status - the status to wait for
	 * @param timeout - the timeout in seconds (only active if greater that 0)
	 * @throws TimeoutException  if timeout is exceeded
	 */
	public void poll(Status status, int timeout) throws Exception {
		int i=0;
		while(getStatus().compareTo(status)<0) {
			Thread.sleep(1000);
			i++;
			if(timeout>0 && i>timeout) {
				throw new TimeoutException();
			}
		}
	}

}
