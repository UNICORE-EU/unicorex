package de.fzj.unicore.xnjs.util;

import org.json.JSONException;
import org.json.JSONObject;

import de.fzj.unicore.xnjs.ems.ExecutionContext;
import de.fzj.unicore.xnjs.tsi.remote.TSIUtils;

public class UFTPUtils {

	public static String makeUFTPCommand(JSONObject spec, ExecutionContext ec) throws JSONException {
		boolean get = "GET".equalsIgnoreCase(spec.getString("operation"));

		if(get) {
		return TSIUtils.makeUFTPGetFileCommand(
				spec.getString("host"),
				spec.getInt("port"),
				spec.getString("secret"),
				spec.getString("remote"),
				spec.getString("local"),
				spec.getString("workdir"),
				spec.optLong("offset", 0),
				spec.optLong("length", -1),
				spec.optBoolean("partial", false),
				ec);
		}
		else {
			return TSIUtils.makeUFTPPutFileCommand(
					spec.getString("host"),
					spec.getInt("port"),
					spec.getString("secret"),
					spec.getString("remote"),
					spec.getString("local"),
					spec.getString("workdir"),
					spec.optLong("offset", 0),
					spec.optLong("length", -1),
					spec.optBoolean("partial", false),
					ec);
		}

	}

	public static JSON jsonBuilder() {
		return new JSON();
	}


	public static class JSON {

		JSONObject spec = new JSONObject();
		private boolean get = true;

		public JSON() {
			try {
				spec.put("partial", false);
			}catch(JSONException e) {}
			offset(0l);
			length(-1l);
		}

		public JSONObject build() {
			return spec;
		}

		public JSON put() {
			try{
				get = false;
				spec.put("operation", "PUT");
			}catch(JSONException e) {}
			return this;
		}

		public JSON get() {
			try{
				get = true;
				spec.put("operation", "GET");
			}catch(JSONException e) {}
			return this;
		}

		public JSON host(String host) {
			try{
				spec.put("host", host);
			}catch(JSONException e) {}
			return this;
		}

		public JSON port(int port) {
			try{
				spec.put("port", port);
			}catch(JSONException e) {}
			return this;
		}

		public JSON secret(String secret) {
			try{
				spec.put("secret", secret);
			}catch(JSONException e) {}
			return this;
		}

		public JSON from(String from) {
			try{
				spec.put( (get? "remote" : "local"), from);
			}catch(JSONException e) {}
			return this;
		}

		public JSON to(String to) {
			try{
				spec.put( (get? "local" : "remote"), to);
			}catch(JSONException e) {}
			return this;
		}

		public JSON partial() {
			try{
				spec.put("partial", true);
			}catch(JSONException e) {}
			return this;
		}

		public JSON offset(long offset) {
			try{
				spec.put("offset", offset);
			}catch(JSONException e) {}
			return this;
		}

		public JSON length(long length) {
			try{
				spec.put("length", length);
			}catch(JSONException e) {}
			return this;
		}

		public JSON workdir(String workdir) {
			try{
				spec.put("workdir", workdir);
			}catch(JSONException e) {}
			return this;
		}

	}
	
}
