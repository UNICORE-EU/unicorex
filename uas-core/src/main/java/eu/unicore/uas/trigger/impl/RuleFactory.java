package eu.unicore.uas.trigger.impl;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import eu.unicore.security.Client;
import eu.unicore.services.utils.UnitParser;
import eu.unicore.uas.json.JSONUtil;
import eu.unicore.uas.trigger.TriggeredAction;
import eu.unicore.uas.trigger.Rule;
import eu.unicore.uas.trigger.RuleSet;
import eu.unicore.uas.trigger.xnjs.ScanSettings;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.ems.ExecutionException;
import eu.unicore.xnjs.io.IStorageAdapter;
import eu.unicore.xnjs.io.XnjsFile;

/**
 * finds rules applicable to a certain directory
 * 
 * @author schuller
 */
public class RuleFactory {
	
	public static final String RULE_FILE_NAME= ".UNICORE_Rules"; // yes it does
	
	public static final String SCAN_SETTINGS="DirectoryScan";
	
	public static final String UPD_INTERVAL="Interval";
	
	public static final String GRACE_PERIOD="Grace";
	
	public static final String INHERIT="InheritFromParent";
	
	public static final String INCLUDE="IncludeDirs";
	
	public static final String EXCLUDE="ExcludeDirs";
	
	public static final String MAXDEPTH="MaxDepth";
	
	public static final String ENABLED="Enabled";
	
	public static final String LOGGING="Logging";
	
	private final IStorageAdapter storage;
	
	private final String uniqueStorageID;
	
	public RuleFactory(IStorageAdapter storage, String uniqueStorageID){
		this.storage=storage;
		this.uniqueStorageID=uniqueStorageID;
	}
	
	/**
	 * Get the set of rules applicable to the given directory.
	 * 
	 * @param directory
	 */
	public RuleSet getRules(String directory)throws IOException, ExecutionException{
		RuleSet result = new RuleSet(directory);
		XnjsFile f=storage.getProperties(directory+"/"+RULE_FILE_NAME);
		if(f!=null){
			result.addAll(readRuleFile(f.getPath()));
		}
		return result;
	}
	

	protected RuleSet readRules(InputStream input)throws ExecutionException, IOException{
		try{
			RuleSet result = new RuleSet(null);
			String source=IOUtils.toString(input, "UTF-8");
			JSONObject json=JSONUtil.read(source);
			JSONArray rules=json.getJSONArray("Rules");
			for(int i=0; i<rules.length(); i++){
				JSONObject rule = rules.getJSONObject(i);
				result.add(makeRule(rule));	
			}
			return result;
		}
		catch(JSONException je){
			throw new IOException(je);
		}
	}
	
	
	public RuleSet readRuleFile(String ruleFile)throws ExecutionException, IOException{
		try(InputStream is=storage.getInputStream(ruleFile)){
			return readRules(is);
		}
	}
	
	/**
	 * read settings for periodic invocation from json and apply them to the rule set
	 * 
	 * @param baseDirectory - directory containing the rule file
	 * @return {@link ScanSettings} or null if none found
	 */
	public ScanSettings parseSettings(String baseDirectory)throws ExecutionException, IOException {
		XnjsFile ruleFile = storage.getProperties(baseDirectory+"/"+RULE_FILE_NAME);
		if(ruleFile==null){
			return null;
		}
		ScanSettings settings = new ScanSettings();
		settings.baseDirectory = baseDirectory;
		String filePath = ruleFile.getPath();
		try(InputStream is = storage.getInputStream(filePath)){
			updateSettings(settings, is);
		}catch(JSONException e){
			throw new IOException(e);
		}
		return settings;
	}
	
	public void updateSettings(ScanSettings settings, InputStream source) throws IOException, JSONException {
		JSONObject json= new JSONObject(IOUtils.toString(source, "UTF-8")).optJSONObject(SCAN_SETTINGS);
		if(json==null)return;

		String interval = JSONUtil.getString(json, UPD_INTERVAL, "60");
		int request = (int)UnitParser.getTimeParser(0).getDoubleValue(interval);
		// do not allow intervals below 30 secs
		settings.updateInterval = Math.max(30, request);

		JSONArray inc=json.optJSONArray(INCLUDE);
		if(inc!=null){
			settings.includes = JSONUtil.toArray(inc);
		}
		JSONArray excl=json.optJSONArray(EXCLUDE);
		if(excl!=null){
			settings.excludes = JSONUtil.toArray(excl);
		}
		settings.maxDepth = Integer.parseInt(JSONUtil.getString(json, MAXDEPTH, "10"));
		settings.enabled = json.optBoolean(ENABLED, true);
		settings.enableLogging = json.optBoolean(LOGGING, true);
	}
	
	protected Rule makeRule(JSONObject json)throws JSONException{
		String name=json.optString("Name", "<unnamed>");
		String match=json.getString("Match");
		TriggeredAction action=makeAction(json.getJSONObject("Action"));
		SimpleRule r=new SimpleRule(name, match, action);
		return r;
	}
	
	protected TriggeredAction makeAction(JSONObject json)throws JSONException{
		String type=json.optString("Type", null);
		if("NOOP".equals(type) || json.keySet().size()==0) {
			return noop;
		}
		else if("JOB".equals(type)|| json.optJSONObject("Job")!=null) {
			return makeJobAction(json);
		}
		else if("EXTRACT".equals(type)|| json.optJSONObject("Extract")!=null) {
			return makeExtractAction(json);
		}
		else if("NOTIFY".equals(type)|| json.optString("Notification", null)!=null) {
			return makeNotifyAction(json);
		}
		return null;
	}
	
	protected TriggeredAction makeJobAction(JSONObject json)throws JSONException{
		return new JobAction(json.getJSONObject("Job"));
	}

	protected TriggeredAction makeExtractAction(JSONObject json)throws JSONException{
		return new ExtractMetadataAction(json.optJSONObject("Extract"), uniqueStorageID);
	}

	protected TriggeredAction makeNotifyAction(JSONObject json)throws JSONException{
		return new NotificationAction(json.getString("URL"));
	}
	
	private static final NOOP noop=new NOOP();
	
	public static class NOOP implements TriggeredAction{

		@Override
		public String run(IStorageAdapter s, String path, Client c, XNJS xnjs) {
			return null;
		}
		
		public String toString(){
			return "NOOP";
		}
	}
}
