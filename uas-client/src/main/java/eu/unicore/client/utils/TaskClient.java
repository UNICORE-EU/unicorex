package eu.unicore.client.utils;

import java.util.Map;

import de.fzj.unicore.uas.json.JSONUtil;
import eu.unicore.client.Endpoint;
import eu.unicore.client.core.BaseServiceClient;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * access tasks
 * 
 * @author schuller
 */
public class TaskClient extends BaseServiceClient {

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

	public TaskClient(Endpoint endpoint, IClientConfiguration security, IAuthCallback auth) {
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
		
	public void abort() throws Exception {
		executeAction("abort", null);
	}
	
	public Map<String,String> getResult() throws Exception {
		return JSONUtil.asMap(getProperties().getJSONObject("result"));
	}
	
}
