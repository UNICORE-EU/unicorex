package eu.unicore.client.admin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import de.fzj.unicore.uas.json.JSONUtil;
import eu.unicore.client.Endpoint;
import eu.unicore.client.core.BaseServiceClient;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.util.httpclient.IClientConfiguration;

public class AdminServiceClient extends BaseServiceClient {

	public AdminServiceClient(Endpoint endpoint, IClientConfiguration security, IAuthCallback auth) {
		super(endpoint, security, auth);
	}

	public Result runCommand(String command, Map<String,String> params) throws Exception {
		Result r = new Result();
		JSONObject res = executeAction(command, JSONUtil.asJSON(params));
		r.successful = Boolean.parseBoolean(String.valueOf(res.get("success")));
		r.message = res.getString("message");
		r.results = JSONUtil.asMap(res.getJSONObject("results"));
		return r;
	}
	
	public List<AdminCommand> getCommands() throws Exception {
		List<AdminCommand> r = new ArrayList<>();
		JSONObject links = getProperties().getJSONObject("_links");
		Iterator<String> keys = links.keys();
		while(keys.hasNext()) {
			String key = keys.next();
			if(!key.startsWith("action:"))continue;
			JSONObject link = links.getJSONObject(key);
			AdminCommand ac = new AdminCommand();
			ac.name = key.substring(7);
			ac.description = link.getString("description");
			r.add(ac);
		}
		return r;
	}
	
	public Map<String,String> getMetrics() throws Exception {
		return JSONUtil.asMap(getProperties().getJSONObject("metrics"));
	}
	
	public static class Result {
		public String message;
		public boolean successful;
		public Map<String,String>results;
		public String toString() {
			return "Result: success="+successful+
					", message="+message+
					", results="+String.valueOf(results);
		}
	}
	
	public static class AdminCommand {
		public String name;
		public String description;
		public String toString() {
			return "AdminCommand "+name+": "+description;
		}
	}
	
}
