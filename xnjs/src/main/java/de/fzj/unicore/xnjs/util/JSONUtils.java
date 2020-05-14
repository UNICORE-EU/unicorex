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
}
