package eu.unicore.uas.json;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * JSON utilities
 *
 * @author schuller
 */
public class JSONUtil {

	private JSONUtil(){}

	public static void putQuietly(JSONObject obj, String key, String val){
		try{
			if(obj!=null)obj.put(key, val);
		}
		catch(JSONException je){
		}
	}

	public static void putQuietly(JSONObject obj, String key, JSONArray val){
		try{
			if(obj!=null)obj.put(key, val);
		}
		catch(JSONException je){
		}
	}
	
	public static void putQuietly(JSONObject obj, String key, JSONObject val){
		try{
			if(obj!=null)obj.put(key, val);
		}
		catch(JSONException je){
		}
	}

	public static Map<String,String>asMap(JSONObject o){
		Map<String,String>result=new HashMap<>();
		if(o!=null){
			Iterator<String>i = o.keys();
			while(i.hasNext()){
				String s = i.next();
				try{
					result.put(s, String.valueOf(o.get(s)));
				}catch(JSONException ex){}
			}
		}
		return result;
	}
	
	public static JSONObject asJSON(Map<String,String>map){
		JSONObject o=new JSONObject();
		if(map!=null){
			for(Map.Entry<String, String>entry: map.entrySet()){
				try{
					o.put(entry.getKey(), entry.getValue());
				}catch(JSONException e){}
			}
		}
		return o;
	}
	
	public static List<String> toList(JSONArray array) throws JSONException {
		List<String> result = new ArrayList<>();
		if(array!=null) {
			for(int i=0; i<array.length(); i++){
				result.add(String.valueOf(array.get(i)));
			}
		}
		return result;
	}
	
	public static String[] toArray(JSONArray array) throws JSONException {
		List<String> result = toList(array);
		return (String[])result.toArray(new String[result.size()]);
	}

	public static List<String> lines(String json) throws IOException {
		return IOUtils.readLines(new StringReader(json.replace("\r\n", "\n").replace("\r","\n")));
	}
	
	public static String toCommaSeparated(List<String>tags){
		StringBuilder sb = new StringBuilder();
		for(String s: tags){
			if(sb.length()>0)sb.append(",");
			sb.append(s);
		}
		return sb.toString();
	}
	
	public static String toCommaSeparated(String... tags){
		StringBuilder sb = new StringBuilder();
		for(String s: tags){
			if(sb.length()>0)sb.append(",");
			sb.append(s);
		}
		return sb.toString();
	}
	
	public static JSONArray getOrCreateArray(JSONObject json, String name) {
		JSONArray arr = json.optJSONArray(name);
		if(arr==null) {
			arr = new JSONArray();
			JSONUtil.putQuietly(json, name, arr);
		}
		return arr;
	}
	
	public static JSONObject getOrCreateObject(JSONObject json, String name) {
		JSONObject obj = json.optJSONObject(name);
		if(obj==null) {
			obj = new JSONObject();
			JSONUtil.putQuietly(json, name, obj);
		}
		return obj;
	}

	/**
	 * read a string value that can optionally be an array
	 * of strings, and return it as a single string
	 * (with '\n' after each line)
	 *
	 * @param key - the JSON element name
	 * @param defaultValue - a default value (can be null)
	 * @param source - source JSON object
	 * @return
	 */
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

}

