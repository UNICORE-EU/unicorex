package de.fzj.unicore.uas.trigger.impl;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.fzj.unicore.uas.json.JSONUtil;
import de.fzj.unicore.uas.trigger.Action;
import de.fzj.unicore.uas.trigger.Rule;
import de.fzj.unicore.uas.trigger.RuleSet;
import de.fzj.unicore.uas.trigger.xnjs.ScanSettings;
import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.io.IStorageAdapter;
import de.fzj.unicore.xnjs.io.XnjsFile;
import eu.unicore.security.Client;

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
//		if(result.isCheckParents()){
//			File path=new File(directory);
//			if(path.getParent()!=null){
//				result.addAll(getRules(path.getParent()));
//			}
//		}
		return result;
	}
	

	protected RuleSet readRules(InputStream input)throws ExecutionException, IOException{
		RuleSet result = new RuleSet(null);
		try{
			String source=IOUtils.toString(input, "UTF-8");
			JSONObject json=JSONUtil.read(source);
			JSONArray rules=json.getJSONArray("Rules");
			for(int i=0; i<rules.length(); i++){
				JSONObject rule = rules.getJSONObject(i);
				result.add(makeRule(rule));	
			}
		}
		catch(JSONException je){
			throw new IOException(je);
		}
		finally{
			input.close();
		}
		return result;
	}
	
	
	public RuleSet readRuleFile(String ruleFile)throws ExecutionException, IOException{
		InputStream is=storage.getInputStream(ruleFile);
		try{
			return readRules(is);
		}finally{
			if(is!=null)is.close();
		}
		
	}
	
	/**
	 * read settings for periodic invocation from json and apply them to the rule set
	 * 
	 * @param baseDirectory - directory containing the rule file
	 * @return {@link ScanSettings} or null if none found
	 */
	public ScanSettings parseSettings(String baseDirectory)throws ExecutionException, IOException {
		ScanSettings settings=new ScanSettings();
		settings.baseDirectory=baseDirectory;
		XnjsFile ruleFile=storage.getProperties(baseDirectory+"/"+RULE_FILE_NAME);
		if(ruleFile==null){
			return null;
		}
		String filePath=ruleFile.getPath();
		
		JSONObject json=null;
		InputStream is=null;
		try{
			is=storage.getInputStream(filePath);
			String source=IOUtils.toString(is, "UTF-8");
			json=new JSONObject(source).optJSONObject(SCAN_SETTINGS);
		}catch(JSONException e){
			throw new IOException(e);
		}
		finally{
			if(is!=null)is.close();
		}
		if(json == null){
			return null;
		}
		String interval=json.optString(UPD_INTERVAL, null);
		if(interval!=null){
			//do not allow intervals below 30 secs
			settings.updateInterval=Math.max(30, Integer.parseInt(interval));
		}
		
		String grace=json.optString(GRACE_PERIOD, null);
		if(grace!=null){
			//do not allow grace periods below 10 secs
			settings.gracePeriod=Math.max(10, Integer.parseInt(grace));
		}
		
		JSONArray inc=json.optJSONArray(INCLUDE);
		if(inc!=null){
			settings.includes=asArray(inc);
		}
		JSONArray excl=json.optJSONArray(EXCLUDE);
		if(excl!=null){
			settings.excludes=asArray(inc);
		}
		String depth=json.optString(MAXDEPTH, "10");
		settings.maxDepth=Integer.parseInt(depth);
		
		String enabled=json.optString(ENABLED, "true");
		settings.enabled=Boolean.parseBoolean(enabled);
		
		return settings;
	}
	
	private String[] asArray(JSONArray json){
		String[] res=new String[json.length()];
		for(int i=0; i<json.length(); i++){
			try{
				res[i]=json.getString(i);
			}catch(JSONException e){}
		}
		return res;
	}
	
	protected Rule makeRule(JSONObject json)throws JSONException{
		String name=json.optString("Name", "<unnamed>");
		String match=json.getString("Match");
		Action action=makeAction(json.getJSONObject("Action"));
		SimpleRule r=new SimpleRule(name, match, action);
		return r;
	}
	
	protected Action makeAction(JSONObject json)throws JSONException{
		String type=json.getString("Type");
		if("NOOP".equals(type))return noop;
		else if("LOCAL".equals(type))return makeLocalAction(json);
		else if("BATCH".equals(type))return makeBatchAction(json);
		else if("EXTRACT".equals(type))return makeExtractAction(json);
		return null;
	}
	
	protected Action makeLocalAction(JSONObject json)throws JSONException{
		String script=json.getString("Command");
		LocalAction la=new LocalAction(script);
		String outDir=json.optString("Outcome", null);
		if(outDir!=null)la.setOutcomeDir(outDir);
		String stdout=json.optString("Stdout", null);
		if(stdout!=null)la.setStdout(stdout);
		String stderr=json.optString("Stderr", null);
		if(stderr!=null)la.setStderr(stderr);
		
		return la;
	}
	
	
	protected Action makeBatchAction(JSONObject json)throws JSONException{
		JSONObject job=json.getJSONObject("Job");
		BatchJobAction ba=new BatchJobAction(job);
		return ba;
	}
	
	protected Action makeExtractAction(JSONObject json)throws JSONException{
		JSONObject settings=json.optJSONObject("Settings");
		ExtractMetadataAction ema=new ExtractMetadataAction(settings, uniqueStorageID);
		return ema;
	}
	
	private static final NOOP noop=new NOOP();
	
	public static class NOOP implements Action{

		@Override
		public void fire(IStorageAdapter s, String path, Client c, XNJS xnjs) {
		}
		
		public String toString(){
			return "NOOP";
		}
	}
}
