package eu.unicore.client.core;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.unicore.client.Endpoint;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.uas.json.JSONUtil;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * manage a single job
 * 
 * @author schuller
 */
public class JobClient extends BaseServiceClient {

	public static enum Status {
	    UNDEFINED,
	    READY,
	    QUEUED,
	    RUNNING,
	    SUCCESSFUL,
	    FAILED,
	    STAGINGIN,
	    STAGINGOUT
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
		return getProperties().getString("queue");
	}
	
	public List<String> getLog() throws Exception {
		JSONArray l = getProperties().getJSONArray("log");
		return JSONUtil.toList(l);
	}
	
	public StorageClient getWorkingDirectory() throws Exception {
		String url = getLinkUrl("workingDirectory");
		Endpoint ep = endpoint.cloneTo(url);
		return new StorageClient(ep, security, auth);
	}
	
	public SiteClient getParentSite() throws Exception {
		String url = getLinkUrl("parentTSS");
		Endpoint ep = endpoint.cloneTo(url);
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
		String url = getLinkUrl("details");
		bc.pushURL(url);
		try{
			return bc.getJSON();
		}finally {
			bc.popURL();
		}
	}

	public JSONObject getSubmittedJobDescription() throws Exception {
		String url = getLinkUrl("submitted");
		bc.pushURL(url);
		try{
			return bc.getJSON();
		}finally {
			bc.popURL();
		}
	}

}
