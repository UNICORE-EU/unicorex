package eu.unicore.uas.json;

import java.io.File;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import eu.unicore.client.Job;

/**
 * Helper to convert a UNICORE job description in JSON to JSDL form 
 * 
 * @author schuller
 */
public class Builder {

	protected final List<ArgumentSweep> sweeps;

	protected final JSONObject json;

	protected final Set<Requirement>requirements=new HashSet<>();

	protected boolean initialised;

	protected boolean isSweepJob=false;

	protected String preferredProtocol = "BFT";

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
	
	public void setJobType(Job.Type jobType) {
		json.put("Job type", jobType.toString());
	}

	public JSONObject getParameters() {
		JSONObject parameters = json.optJSONObject("Parameters");
		if(parameters==null) {
			parameters = new JSONObject();
			json.put("Parameters", parameters);
		}
		return parameters;
	}

	public void writeTo(Writer os) throws Exception {
		os.write(json.toString(2)+"\n");
		os.flush();
	}

	public Collection<Requirement>getRequirements(){
		build();
		return requirements;
	}

	@Override
	public String toString(){
		return json.toString(2);
	}

	public boolean isSweepJob(){
		return isSweepJob;
	}

}
