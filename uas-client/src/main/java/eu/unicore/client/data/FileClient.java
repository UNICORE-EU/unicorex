package eu.unicore.client.data;

import java.util.Map;

import org.apache.http.HttpResponse;
import org.json.JSONObject;

import de.fzj.unicore.uas.json.JSONUtil;
import eu.unicore.client.Endpoint;
import eu.unicore.client.core.BaseServiceClient;
import eu.unicore.client.utils.TaskClient;
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
		o.put("permissions", unixPermissions);
		HttpResponse res = bc.put(o);
		bc.checkError(res);
		JSONObject reply = bc.asJSON(res);
		String msg = reply.optString("permissions", "n/a");
		if(!"OK".contentEquals(msg)) {
			throw new Exception("Could not change permissions: "+msg);
		}
	}

	public Map<String, String> getMetadata() throws Exception {
		return JSONUtil.asMap(bc.getJSON().getJSONObject("metadata"));
	}
	
	
	public void putMetadata(Map<String, String> metadata) throws Exception {
		JSONObject req = new JSONObject();
		req.put("metadata", JSONUtil.asJSON(metadata));
		HttpResponse res = bc.put(req);
		bc.checkError(res);
		String reply = bc.asJSON(res).optString("metadata", "n/a");
		if(!"OK".equals(reply)) {
			throw new Exception("Error updating metadata: "+reply);
		}
	}
	
	/**
	 * launch metadata extraction
	 */
	public TaskClient startMetadataExtraction(int depth, String ... resources) throws Exception {
		JSONObject op = new JSONObject();
		op.put("depth", depth);
		JSONObject response = executeAction("extract", op);
		String href = response.optString("taskHref", null);
		return href!=null? 
			new TaskClient(new Endpoint(href), security, auth) : null;
	}
}
