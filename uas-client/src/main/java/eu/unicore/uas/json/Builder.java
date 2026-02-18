package eu.unicore.uas.json;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import eu.unicore.client.Job;

/**
 * Helper for handling JSON job descriptions
 * 
 * @author schuller
 */
public class Builder {

	protected final JSONObject json;

	protected final Set<Requirement>requirements=new HashSet<>();

	protected boolean initialised;

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
	 * @throws JSONException
	 */
	public Builder(String jsonString) throws JSONException {
		json = new JSONObject(jsonString);
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
		return JSONUtil.toArray(tags);
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
		String appName = json.optString("ApplicationName", null);
		String appVersion = json.optString("ApplicationVersion", null);
		if(appName!=null){
			requirements.add(new ApplicationRequirement(appName,appVersion));
		}
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
		return json.optString(key, null);
	}

	public String getProperty(String key, String defaultValue) {
		return json.optString(key,defaultValue);
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

	public Collection<Requirement>getRequirements(){
		build();
		return requirements;
	}

	@Override
	public String toString(){
		return json.toString(2);
	}

}
