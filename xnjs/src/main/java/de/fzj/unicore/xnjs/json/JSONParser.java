package de.fzj.unicore.xnjs.json;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.fzj.unicore.xnjs.idb.ApplicationInfo;
import de.fzj.unicore.xnjs.idb.ApplicationInfo.JobType;
import de.fzj.unicore.xnjs.idb.ApplicationMetadata;
import de.fzj.unicore.xnjs.idb.OptionDescription;
import de.fzj.unicore.xnjs.idb.OptionDescription.Type;
import de.fzj.unicore.xnjs.idb.Partition;
import de.fzj.unicore.xnjs.io.DataStageInInfo;
import de.fzj.unicore.xnjs.io.DataStageOutInfo;
import de.fzj.unicore.xnjs.io.DataStagingCredentials;
import de.fzj.unicore.xnjs.io.DataStagingInfo;
import de.fzj.unicore.xnjs.io.IFileTransfer.ImportPolicy;
import de.fzj.unicore.xnjs.io.IFileTransfer.OverwritePolicy;
import de.fzj.unicore.xnjs.io.impl.OAuthToken;
import de.fzj.unicore.xnjs.io.impl.UsernamePassword;
import de.fzj.unicore.xnjs.resources.BooleanResource;
import de.fzj.unicore.xnjs.resources.DoubleResource;
import de.fzj.unicore.xnjs.resources.IntResource;
import de.fzj.unicore.xnjs.resources.Resource;
import de.fzj.unicore.xnjs.resources.Resource.Category;
import de.fzj.unicore.xnjs.resources.ResourceRequest;
import de.fzj.unicore.xnjs.resources.ResourceSet;
import de.fzj.unicore.xnjs.resources.StringResource;
import de.fzj.unicore.xnjs.resources.ValueListResource;
import de.fzj.unicore.xnjs.util.JSONUtils;
import de.fzj.unicore.xnjs.util.UnitParser;

public class JSONParser {
	
	public static ApplicationInfo parseSubmittedApplication(JSONObject job) throws Exception {
		ApplicationInfo app = new ApplicationInfo();
		app.setName(job.optString("ApplicationName",null));
		app.setVersion(job.optString("ApplicationVersion",null));

		app.setUserPreCommand(JSONUtils.readMultiLine("User precommand", null, job));
		app.setUserPreCommandOnLoginNode(job.optBoolean("RunUserPrecommandOnLoginNode", true));
		app.setUserPreCommandIgnoreExitCode(job.optBoolean("UserPrecommandIgnoreNonZeroExitCode", false));
		
		app.setExecutable(job.optString("Executable",null));
		app.setArguments(JSONUtils.asStringArray(job.optJSONArray("Arguments")));
		app.getEnvironment().putAll(JSONUtils.asStringMap(job.optJSONObject("Parameters")));
		app.getEnvironment().putAll(JSONUtils.asStringMap(job.optJSONObject("Environment")));
		parseEnvironment(job.optJSONArray("Environment"), app);
		app.setIgnoreNonZeroExitCode(job.optBoolean("IgnoreNonZeroExitCode", false));

		app.setUserPostCommand(JSONUtils.readMultiLine("User postcommand", null, job));
		app.setUserPostCommandOnLoginNode(job.optBoolean("RunUserPostcommandOnLoginNode", true));
		app.setUserPostCommandIgnoreExitCode(job.optBoolean("UserPostcommandIgnoreNonZeroExitCode", false));
		
		app.setResourceRequest(parseResourceRequest(job.optJSONObject("Resources")));

		switch(parseJobType(job)){
			case ON_LOGIN_NODE:
				app.setRunOnLoginNode(true);
				break;
			case RAW:
				String file = job.optString("BSS file", null);
				if(file==null)throw new Exception("Job type 'raw' requires 'BSS file'");
				app.setRawBatchFile(file);
				break;
			case ALLOCATE:
				app.setAllocateOnly();
				break;
			case BATCH:
				break;
		}
		app.setPreferredLoginNode(job.optString("Login node", null));
		
		app.setStdout(job.optString("Stdout",null));
		app.setStderr(job.optString("Stderr",null));
		app.setStdin(job.optString("Stdin",null));
		
		return app;
	}
	
	public static String parseUmask(JSONObject job) {
		return JSONUtils.getStringAlt(job, "Umask", "umask");
	}

	public static JobType parseJobType(JSONObject job) {
		String jt = job.optString("Job type", "BATCH").toUpperCase();
		// accept 8.x values for these
		if("INTERACTIVE".equals(jt))jt = "ON_LOGIN_NODE";
		if("NORMAL".equals(jt))jt = "BATCH";
		return JobType.valueOf(jt.toUpperCase());
	}

	private static void parseEnvironment(JSONArray j, ApplicationInfo app){
		if(j==null)return;
		for (int i = 0; i < j.length(); i++) {
			try{
				String val=j.getString(i);
				String[] split=val.split("=",2);
				String name = split[0].trim();
				String value = split.length>1 ? split[1].trim() : "";
				app.getEnvironment().put(name,value);
			}catch(JSONException ex){
				throw new IllegalArgumentException("Error parsing entry "+i+" in environment array! ",ex);
			}
		}
	}
	
	public static DataStageInInfo parseStageIn(JSONObject spec) throws Exception {
		DataStageInInfo dsi = new DataStageInInfo();
		String to = JSONUtils.getStringAlt(spec, "To", "file");
		String source = JSONUtils.getStringAlt(spec, "From", "source");
		if(source==null && JSONUtils.hasKey(spec, "Data")) {
			source = "inline:/dummy";
		}
		dsi.setFileName(to);
		dsi.setSources(new URI[]{new URI(source)});
		if(source.startsWith("inline:")) {
			dsi.setInlineData(JSONUtils.readMultiLine("Data", "", spec));
			spec.put("Data","n/a"); // avoid storing inline data in the DB forever
		}
		extractDataStagingOptions(spec, dsi);
		return dsi;
	}

	public static DataStageOutInfo parseStageOut(JSONObject spec) throws Exception {
		DataStageOutInfo dso = new DataStageOutInfo();
		String from = JSONUtils.getStringAlt(spec, "From", "file");
		String target = JSONUtils.getStringAlt(spec, "To", "target");
		dso.setFileName(from);
		dso.setTarget(new URI(target));
		extractDataStagingOptions(spec, dso);
		return dso;
	}
	
	public static void extractDataStagingOptions(JSONObject spec, DataStagingInfo dsi) throws Exception {
		String creation = JSONUtils.getOrDefault(spec, "Mode", "overwrite");
		if("append".equalsIgnoreCase(creation)){
			dsi.setOverwritePolicy(OverwritePolicy.APPEND);
		}
		else if("nooverwrite".equalsIgnoreCase(creation)){
			dsi.setOverwritePolicy(OverwritePolicy.DONT_OVERWRITE);
		}
		else dsi.setOverwritePolicy(OverwritePolicy.OVERWRITE);

		Boolean failOnError = Boolean.parseBoolean(JSONUtils.getOrDefault(spec, "FailOnError", "true"));
		dsi.setIgnoreFailure(!failOnError);

		Boolean readOnly=Boolean.parseBoolean(JSONUtils.getOrDefault(spec, "ReadOnly", "false"));
		if(readOnly && dsi instanceof DataStageInInfo) {
			((DataStageInInfo)dsi).setImportPolicy(ImportPolicy.PREFER_LINK);
		}

		JSONObject credentials = spec.optJSONObject("Credentials");
		if(credentials!=null) {
			dsi.setCredentials(extractCredentials(credentials));
		}
	}

	public static DataStagingCredentials extractCredentials(JSONObject jCredentials) throws Exception {
		DataStagingCredentials creds=null;
		if(JSONUtils.getString(jCredentials, "Username")!=null) {
			creds = new UsernamePassword(JSONUtils.getString(jCredentials, "Username"), 
					JSONUtils.getString(jCredentials, "Password"));
		}
		else if(JSONUtils.getString(jCredentials, "BearerToken")!=null){
			creds = new OAuthToken(JSONUtils.getString(jCredentials, "BearerToken"));
		}
		return creds;
	}

	public static ApplicationInfo parseApplicationInfo(JSONObject source) throws Exception {
		ApplicationInfo info = new ApplicationInfo();
		info.setName(source.getString("Name"));
		info.setVersion(source.getString("Version"));
		info.setDescription(source.optString("Description", null));
		
		info.setPreCommand(JSONUtils.readMultiLine("PreCommand", null, source));
		info.setPrologue(JSONUtils.readMultiLine("Prologue", null, source));
		info.setExecutable(source.getString("Executable"));
		info.setArguments(JSONUtils.asStringArray(source.optJSONArray("Arguments")));
		info.getEnvironment().putAll(JSONUtils.asStringMap(source.optJSONObject("Environment")));
		info.setEpilogue(JSONUtils.readMultiLine("Epilogue", null, source));
		
		info.setRunOnLoginNode(source.optBoolean("RunOnLoginNode", false));
		info.setIgnoreNonZeroExitCode(source.optBoolean("IgnoreNonZeroExitCode", false));
		info.setPostCommand(JSONUtils.readMultiLine("PostCommand", null, source));

		info.setResourceRequest(parseResourceRequest(source.optJSONObject("Resources")));
		info.setMetadata(parseApplicationMetadata(source.optJSONObject("Parameters")));
		
		return info;
	}
	
	public static ApplicationMetadata parseApplicationMetadata(JSONObject source) throws Exception {
		ApplicationMetadata meta = new ApplicationMetadata();
		if(source!=null) {
			for(String name: source.keySet()) {
				JSONObject optionJ = source.getJSONObject(name);
				meta.getOptions().add(parseOptionDescription(name, optionJ));
			}
		}
		return meta;
	}
	
	public static String parseScriptTemplate(String key, JSONObject idb) {
		return JSONUtils.readMultiLine(key, null, idb);
	}

	public static List<ResourceRequest> parseResourceRequest(JSONObject source) throws Exception {
		List<ResourceRequest> req = new ArrayList<>();
		if(source!=null && source.length()>0) {
			for(String name: source.keySet()) {
				String value = parseResourceValue(name, String.valueOf(source.get(name)));
				if("Memory".equals(name))name=ResourceSet.MEMORY_PER_NODE;
				req.add(new ResourceRequest(name, value));
			}
		}
		return req;
	}

	private static String parseResourceValue(String name, String value) {
		if(ResourceSet.MEMORY_PER_NODE.equals(name)
				|| "Memory".equals(name)) {
			return String.valueOf(UnitParser.getCapacitiesParser(0).getLongValue(value));
		}
		else if(ResourceSet.RUN_TIME.equals(name))
		{
			return String.valueOf(UnitParser.getTimeParser(0).getLongValue(value));
		}
		return value;
	}
	
	public static OptionDescription parseOptionDescription(String name, JSONObject source) throws JSONException {
		OptionDescription option = new OptionDescription();
		option.setName(name);
		String type =  source.optString("Type", "STRING").toUpperCase();
		option.setType(Type.valueOf(type));
		option.setDescription(source.optString("Description", null));
		if("CHOICE".equals(type)) {
			option.setValidValues(JSONUtils.asStringArray(source.getJSONArray("ValidValues")));
		}
		return option;
	}
	
	public static Partition parsePartition(JSONObject source) throws JSONException {
		return parsePartition(source.getString("Name"), source);
	}
	
	public static Partition parsePartition(String name, JSONObject source) throws JSONException {
		Partition p = new Partition();
		p.setName(name);
		p.setDescription(source.optString("Description"));
		p.setOperatingSystem(source.optString("OperatingSystem", "LINUX"));
		p.setOperatingSystemVersion(source.optString("OperatingSystemVersion", null));
		p.setCpuArchitecture(source.optString("CPUArchitecture", "x86"));
		p.setDefaultPartition(source.optBoolean("IsDefaultPartition", false));
		JSONObject resources = source.getJSONObject("Resources");
		for(String resource: resources.keySet()) {
			JSONObject spec = resources.optJSONObject(resource);
			if(spec!=null) {
				p.getResources().putResource(createResource(resource, spec));	
			}
			else {
				p.getResources().putResource(createIntResource(resource, resources.getString(resource)));
			}
		}
		return p;
	}

	public static Resource createResource(JSONObject doc)
			throws JSONException {
		String name = doc.getString("Name");
		if (name.trim().isEmpty())
			throw new JSONException("Resource 'Name' can not be empty"); 
		return createResource(name, doc);
	}

	
	public static Resource createResource(String name, JSONObject doc)
			throws JSONException {
		Resource resource;
		if("Memory".equals(name))name=ResourceSet.MEMORY_PER_NODE;
		
		String description = doc.optString("Description", null);
		String valueSpec = doc.optString("Range", "0-1");
		String min = null, max = null;
		String defaultValue = doc.optString("Default", null);
		
		
		String type = doc.optString("Type", "INT").toUpperCase();
		UnitParser up = getUnitParser(name);
		
		if("INT".equals(type)|| "DOUBLE".equals(type)){
			String[] rangeTokens = valueSpec.split("-");
			min = rangeTokens[0];
			max = rangeTokens[1];
		}
		
		if("INT".equals(type)){
			Long value = defaultValue!=null ? Long.valueOf(defaultValue):null;
			Long minI=min!=null ? up.getLongValue(min):null;
			Long maxI=max!=null ? up.getLongValue(max):null;
			resource=new IntResource(name, value, maxI, minI, ResourceSet.getCategory(name));
		}
		else if("DOUBLE".equals(type)){
			Double value=defaultValue!=null?Double.valueOf(defaultValue):null;
			Double minF=min!=null ? up.getDoubleValue(min):null;
			Double maxF=max!=null ? up.getDoubleValue(max):null;
			resource=new DoubleResource(name, value, maxF, minF, ResourceSet.getCategory(name));
		}
		else if("STRING".equals(type)){
			resource=new StringResource(name,defaultValue);
		}
		else if("CHOICE".equals(type)){
			JSONArray choices = doc.optJSONArray("AllowedValues");
			if(choices==null)choices = doc.optJSONArray("Allowed values");
			if(choices==null)choices = doc.getJSONArray("allowed_values");
			
			String[] values = JSONUtils.asStringArray(choices);
			if(values.length<1)throw new JSONException("Resource of type CHOICE needs one or more 'AllowedValues'");
			resource=new ValueListResource(name,defaultValue,Arrays.asList(values),Category.OTHER);
		} 
		else if("BOOLEAN".equals(type)){
			Boolean value = defaultValue!=null? Boolean.valueOf(defaultValue) : null;
			resource=new BooleanResource(name, value, Category.OTHER);
		}
		else {
			throw new JSONException("Unknown type of Resource " + name + ": " + type);
		}
		
		if (description != null)
			resource.setDescription(description);
		return resource;
	}
	
	public static Resource createIntResource(String name, String valueSpec)
			throws JSONException {
		if("Memory".equals(name))name=ResourceSet.MEMORY_PER_NODE;
		
		String min = null, max = null;
		
		String[] tokens = valueSpec.split(":");
		String defaultValue = tokens.length>1? tokens[1] : null;
		valueSpec = tokens[0];
		
		String[] rangeTokens = valueSpec.split("-");
		min = rangeTokens[0];
		max = rangeTokens[1];
		
		UnitParser up = getUnitParser(name);
		
		Long minI=min!=null ? up.getLongValue(min) :null;
		Long maxI=max!=null ? up.getLongValue(max):null;
		Long valI=defaultValue!=null ? up.getLongValue(defaultValue):null;
		
		return new IntResource(name, valI,maxI,minI, ResourceSet.getCategory(name));
	}
	
	public static UnitParser getUnitParser(String resourceName) {
		if(ResourceSet.RUN_TIME.equalsIgnoreCase(resourceName)) {
			return UnitParser.getTimeParser(0);
		}
		else {
			return UnitParser.getCapacitiesParser(0);
		}
	}
	
	public static List<String>parseNotificationURLs(JSONObject job){
		List<String> res = new ArrayList<>();
		JSONObject spec = job.optJSONObject("NotificationSettings");
		String url = null;
		if(spec!=null) {
			url = spec.optString("URL");
		} else {
			url = job.optString("Notification");
		}
		if(url!=null && !url.isEmpty()) {
			res.add(url);
		}
		return res;
	}

	public static List<String>parseNotificationTriggers(JSONObject job){
		List<String> res = new ArrayList<>();
		JSONObject spec = job.optJSONObject("NotificationSettings");
		if(spec!=null && spec.optJSONArray("status")!=null) {
			String[] triggers = JSONUtils.asStringArray(spec.optJSONArray("status"));
			for(String s: triggers) {
				res.add(s);
			}
		} else {
			return null;
		}
		return res;
	}
	
	public static List<String>parseNotificationBSSTriggers(JSONObject job){
		List<String> res = new ArrayList<>();
		JSONObject spec = job.optJSONObject("NotificationSettings");
		if(spec!=null) {
			String[] triggers = JSONUtils.asStringArray(spec.optJSONArray("bssStatus"));
			for(String s: triggers) {
				res.add(s);
			}
		}
		return res;
	}
}
