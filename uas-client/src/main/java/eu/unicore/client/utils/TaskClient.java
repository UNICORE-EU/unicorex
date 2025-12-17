package eu.unicore.client.utils;

import java.util.Map;
import java.util.concurrent.TimeoutException;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.BaseServiceClient;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.uas.json.JSONUtil;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * access tasks
 * 
 * @author schuller
 */
public class TaskClient extends BaseServiceClient {

	public static enum Status {
	    UNDEFINED,
	    CREATED,
	    RUNNING,
	    SUCCESSFUL,
	    FAILED,
	    ABORTED
	}

	public TaskClient(Endpoint endpoint, IClientConfiguration security, IAuthCallback auth) {
		super(endpoint, security, auth);
	}

	public Status getStatus() throws Exception {
		return Status.valueOf(getProperties().getString("status"));
	}

	/**
	 * wait for the task to reach the given status (or a later one)
	 *
	 * @param status - the status to wait for. If <code>null</code>, wait for SUCCESSFUL
	 */
	public void poll(Status status) throws Exception {
		poll(status, -1);
	}

	/**
	 * wait for the job to reach the given status (or a later one)
	 *
	 * @param status - the status to wait for. If <code>null</code>, wait for SUCCESSFUL
	 * @param timeout - the timeout in seconds (only active if greater that 0)
	 * @throws TimeoutException  if timeout is exceeded
	 */
	public void poll(Status status, int timeout) throws Exception {
		int i=0;
		if(status==null)status = Status.SUCCESSFUL;
		while(getStatus().compareTo(status)<0) {
			Thread.sleep(1000);
			i++;
			if(timeout>0 && i>timeout) {
				throw new TimeoutException();
			}
		}
	}

	public String getStatusMessage() throws Exception {
		return getProperties().getString("statusMessage");
	}

	public boolean isFinished() throws Exception {
		Status s = getStatus();
		return Status.FAILED==s || Status.SUCCESSFUL == s;
	}

	public Integer getExitCode() throws Exception {
		String e = getProperties().optString("exitCode",null);
		return e!=null ? Integer.parseInt(e) : null;
	}

	public Float getProgress() throws Exception {
		String e = getProperties().optString("progress",null);
		return e!=null ? Float.parseFloat(e) : null;
	}

	public void abort() throws Exception {
		executeAction("abort", null);
	}

	public Map<String,String> getResult() throws Exception {
		return JSONUtil.asMap(getProperties().getJSONObject("result"));
	}

}