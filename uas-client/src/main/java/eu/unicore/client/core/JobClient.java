package eu.unicore.client.core;

import java.util.List;
import java.util.concurrent.TimeoutException;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.unicore.client.Endpoint;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.uas.json.JSONUtil;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * manage a UNICORE job via its REST API endpoint
 * 
 * @author schuller
 */
public class JobClient extends BaseServiceClient {

	public static enum Status {
	    UNDEFINED,
	    READY,
	    STAGINGIN,
		QUEUED,
	    RUNNING,
	    STAGINGOUT,
	    SUCCESSFUL,
	    FAILED
	}

	public JobClient(Endpoint endpoint, IClientConfiguration security, IAuthCallback auth) {
		super(endpoint, security, auth);
	}

	public Status getStatus() throws Exception {
		return Status.valueOf(getProperties().getString("status"));
	}

	public String getStatusMessage() throws Exception {
		return getProperties().getString("statusMessage");
	}

	/**
	 * wait for the job to reach the given status (or a later one)
	 *
	 * @param status - the status to wait for
	 */
	public void poll(Status status) throws Exception {
		poll(status, -1);
	}

	/**
	 * wait for the job to reach the given status (or a later one)
	 *
	 * @param status - the status to wait for
	 * @param timeout - the timeout in seconds (only active if greater that 0)
	 * @throws TimeoutException  if timeout is exceeded
	 */
	public void poll(Status status, int timeout) throws Exception {
		int i=0;
		while(getStatus().compareTo(status)<=0) {
			Thread.sleep(1000);
			i++;
			if(timeout>0 && i>timeout) {
				throw new TimeoutException();
			}
		}
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
	
	public String getQueue() throws Exception {
		return getProperties().optString("queue", null);
	}
	
	public List<String> getLog() throws Exception {
		JSONArray l = getProperties().getJSONArray("log");
		return JSONUtil.toList(l);
	}

	public String getJobName() throws Exception{
		return getProperties().optString("name", "n/a");
	}

	public String getSubmissionTime() throws Exception{
		return getProperties().optString("submissionTime", "n/a");
	}

	public StorageClient getWorkingDirectory() throws Exception {
		Endpoint ep = endpoint.cloneTo(getLinkUrl("workingDirectory"));
		return new StorageClient(ep, security, auth);
	}
	
	public SiteClient getParentSite() throws Exception {
		Endpoint ep = endpoint.cloneTo(getLinkUrl("parentTSS"));
		return new SiteClient(ep, security, auth);
	}
	
	public void start() throws Exception {
		executeAction("start", null);
	}
	
	public void restart() throws Exception {
		executeAction("restart", null);
	}
	
	public void abort() throws Exception {
		executeAction("abort", null);
	}

	public JSONObject getBSSDetails() throws Exception {
		bc.pushURL(getLinkUrl("details"));
		try{
			return bc.getJSON();
		}finally {
			bc.popURL();
		}
	}

	public JSONObject getSubmittedJobDescription() throws Exception {
		bc.pushURL(getLinkUrl("submitted"));
		try{
			return bc.getJSON();
		}finally {
			bc.popURL();
		}
	}

}
