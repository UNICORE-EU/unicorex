package de.fzj.unicore.uas.json;

import java.io.File;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import eu.unicore.util.Log;

/**
 * Helper to convert a UNICORE job description in JSON to JSDL form 
 * 
 * @author schuller
 */
public class Builder {

	private static final Logger logger = Log.getLogger(Log.UNICORE, Builder.class);

	protected final List<ArgumentSweep> sweeps;
	
	protected final JSONObject json;

	protected final Set<Requirement>requirements=new HashSet<Requirement>();

	protected boolean initialised;

	protected boolean isSweepJob=false;

	protected String preferredProtocol = "BFT";

	protected boolean convertRESTtoWSRF = false;
	
	/**
	 * reads a JSON string from the supplied File
	 * and creates the builder from it
	 * 
	 * @param jsonFile
	 * @throws Exception
	 */
	public Builder(File jsonFile)throws Exception{
		this(FileUtils.readFileToString(jsonFile, "UTF-8"));
	}

	/**
	 * Creates the builder from the supplied JSON string
	 * 
	 * @param jsonString
	 * @throws IllegalArgumentException on JSON parsing errors
	 */
	public Builder(String jsonString) {
		sweeps = new ArrayList<>();
		try{
			json=JSONUtil.read(jsonString);
			isSweepJob = json.optBoolean("isSweepJob", false);
		}catch(JSONException ex){
			String message=JSONUtil.makeParseErrorMessage(jsonString, ex);
			throw new IllegalArgumentException(message);
		}
	}


	/**
	 * Creates an empty builder. All content has to be set via the API
	 * @throws Exception
	 */
	public Builder()throws Exception{
		this("{}");
		json.put("Output",".");
	}

	public void setConvertRESTtoWSRF(boolean convert){
		this.convertRESTtoWSRF = convert;
	}
	
	public JSONObject getJSON() {
		return json;
	}
	
	public String[] getTags(){
		JSONArray tags = json.optJSONArray("Tags");
		if(tags==null)tags = json.optJSONArray("tags");
		if(tags!=null){
			String[] ret = new String[tags.length()];
			for(int i=0;i<tags.length();i++){
				ret[i]=tags.optString(i);
			}
			return ret;
		}
		return null;
	}

	protected void build(){
		if(initialised)return;
		initialised=true;
		preferredProtocol = parseProtocol();
		extractRequirements();
	}

	protected String parseProtocol(){
		return getProperty("Preferred protocol", "BFT");
	}

	protected void extractRequirements(){
		String appName=JSONUtil.getString(json,"ApplicationName");
		String appVersion=JSONUtil.getString(json,"ApplicationVersion");
		if(appName!=null){
			ApplicationRequirement appRequired=new ApplicationRequirement(appName,appVersion);
			requirements.add(appRequired);
		}

		JSONObject ee=json.optJSONObject("Execution environment");
		if(ee!=null){
			throw new IllegalArgumentException("Tag 'Execution environment' is no longer supported");
		}
	}

	protected Location createLocation(String descriptor){
		return new RawLocation(descriptor);
	}

	protected boolean hasCredentials(JSONObject jObj){
		return jObj!=null && jObj.optJSONObject("Credentials")!=null;
	}

	private static final Pattern restURLPattern = Pattern.compile("(https||http)://(.*)/rest/core/storages/([^/]*)/files/(.*)");

	/**
	 * Converts a UNICORE REST URL to a UNICORE WSRF URL
	 * (heuristically, i.e. using pattern matching). If the URL does
	 * not match the REST URL pattern, it is returned unchanged
	 */
	public static String convertRESTToWSRF(String url){
		Matcher m = restURLPattern.matcher(url);
		if(!m.matches())return url;
		String scheme=m.group(1);
		String base=m.group(2);
		String storageID=m.group(3);
		String path=m.group(4);
		String wsrfURL = "BFT:"+scheme+"://"+base+"/services/StorageManagement?res="+storageID+"#/"+path;
		if(logger.isDebugEnabled())
			logger.debug("Converted REST URL <"+url+"> to WSRF URL <"+wsrfURL+">");
		return wsrfURL;
	}

	//list of common OSs for which we want to ignore case
	static final String[] knownOSs=new String[]{"LINUX", "MACOS", "AIX", 
		"FreeBSD", "NetBSD", "Solaris", "WINNT", "IRIX", "HPUX", "Unknown"};


	public void setProperty(String key, String value) {
		try{
			if(value==null){
				json.remove(key);
			}
			else{
				json.put(key, value);
			}
		}catch(Exception e){}
	}

	/**
	 * returns the given property, or null if not found
	 * @param key - the property key 
	 */
	public String getProperty(String key) {
		return JSONUtil.getString(json, key);
	}

	public String getProperty(String key,String defaultValue) {
		return JSONUtil.getString(json, key,defaultValue);
	}

	public void writeTo(Writer os) {
		try{
			os.write(json.toString(2)+"\n");
			os.flush();
		}catch(Exception e){logger.error("",e);}
	}

	public Collection<Requirement>getRequirements(){
		build();
		return requirements;
	}
	
	public String toString(){
		try{
			return json.toString(2);
		}
		catch(Exception e){}
		return super.toString()+"<invalid JSON object>";
	}

	public boolean isSweepJob(){
		return isSweepJob;
	}

}
