package de.fzj.unicore.xnjs.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JSONUtils {
	
	private JSONUtils() {}
	
	public static String[] asStringArray(JSONArray array) throws JSONException {
		if(array==null) return new String[0];
		
		String[] res = new String[array.length()];
		for(int i=0;i<res.length;i++) {
			res[i] = array.getString(i);
		}
		return res;
	}
	
	public static Map<String,String> asStringMap(JSONObject map) throws JSONException {
		Map<String,String> res = new HashMap<>();
		if(map!=null) {
			Iterator<?> iter = map.keys();
			while(iter.hasNext()) {
				String key = String.valueOf(iter.next());
				String val = map.getString(key);
				res.put(key,val);
			}
		}
		return res;
	}
	
	public static String readMultiLine(String key, String defaultValue, JSONObject source){
		JSONArray a = source.optJSONArray(key);
		if(a==null) {
			return source.optString(key, defaultValue);
		}
		else if(a.length()>0){
			StringBuilder sb = new StringBuilder();
			for(int i=0;i<a.length();i++) {
				String v = a.optString(i, "");
				sb.append(v);
				if(!v.endsWith("\n"))sb.append("\n");
			}
			return sb.toString();
		}
		else return defaultValue;
	}

	/**
	 * get the requested value
	 * @param obj - the json object
	 * @param key - the key
	 * @param defaultValue - the default value
	 */
	public static String getString(JSONObject obj, String key, String defaultValue){
		try{
			return obj.getString(key);
		}
		catch(JSONException je){
			return defaultValue;
		}
	}
	
	/**
	 * get the requested value or <code>null</code> if it does not exist in the json
	 * @param obj
	 * @param key
	 */
	public static String getString(JSONObject obj, String key){
		return getString(obj, key, null);
	}
	
}
