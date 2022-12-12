package de.fzj.unicore.uas.json;

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
 * JSON utilities to work around some small issues with the JSON.org parser 
 * 
 * @author schuller
 */
public class JSONUtil {
	
	private JSONUtil(){}
	
	/**
	 * get the requested value
	 * @param obj - the json object
	 * @param key - the key
	 * @param defaultValue - the default value
	 */
	public static String getString(JSONObject obj, String key, String defaultValue){
		try{
			return String.valueOf(obj.get(key));
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
	
	/**
	 * improve the error message by including the line number (not just the position)
	 * @param json
	 * @param ex
	 * @return error message containing the line number in the JSON
	 */
	public static String makeParseErrorMessage(String json, JSONException ex){
		String line="unknown";
		String[] parts=ex.getMessage().split("at character ");
		if(parts.length>1){
			try{
				int pos=Integer.parseInt(parts[1]);
				line=String.valueOf(getLineNumber(json, pos));
			}
			catch(Exception e){}
		}
		return "Error parsing (around line "+line+ ") "+ex.getMessage();
	}
	
	private static int getLineNumber(String s, int pos)throws IOException{
		int l=0;
		List<String>lines = lines(s);
		int c=0;
		for(String line: lines){
			if(line.matches(commentPattern))continue;
			c+=line.length();
			if(c>=pos)break;
			l++;
		}
		return l;
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
	
	private static String commentPattern = "(?m)^\\s*#.*\\n";
	
	/**
	 * read the JSONObject, ignoring comments
	 * @param json
	 */
	public static JSONObject read(String json) throws JSONException {
		return new JSONObject(json.replace("\r\n", "\n").replaceAll(commentPattern, "\n"));
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

