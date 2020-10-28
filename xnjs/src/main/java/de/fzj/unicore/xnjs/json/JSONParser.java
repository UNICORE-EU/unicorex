package de.fzj.unicore.xnjs.json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.fzj.unicore.xnjs.idb.ApplicationInfo;
import de.fzj.unicore.xnjs.idb.ApplicationInfoParser;
import de.fzj.unicore.xnjs.idb.ApplicationMetadata;
import de.fzj.unicore.xnjs.idb.OptionDescription;
import de.fzj.unicore.xnjs.idb.OptionDescription.Type;
import de.fzj.unicore.xnjs.idb.Partition;
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

public class JSONParser implements ApplicationInfoParser<JSONObject>{

	@Override
	public ApplicationInfo parseApplicationInfo(JSONObject source) throws Exception {
		ApplicationInfo info = new ApplicationInfo();
		info.setName(source.getString("Name"));
		info.setVersion(source.getString("Version"));
		info.setDescription(source.optString("Description", null));
		
		info.setPreCommand(source.optString("PreCommand", null));
		info.setPrologue(source.optString("Prologue", null));
		info.setExecutable(source.getString("Executable"));
		info.setArguments(JSONUtils.asStringArray(source.optJSONArray("Arguments")));
		info.getEnvironment().putAll(JSONUtils.asStringMap(source.optJSONObject("Environment")));
		info.setEpilogue(source.optString("Epilogue", null));
		info.setRunOnLoginNode(source.optBoolean("RunOnLoginNode", false));
		info.setIgnoreNonZeroExitCode(source.optBoolean("IgnoreNonZeroExitCode", false));
		info.setPostCommand(source.optString("PostCommand", null));
		
		info.setResourceRequest(parseResourceRequest(source.optJSONObject("Resources")));
		info.setMetadata(parseApplicationMetadata(source.optJSONObject("Parameters")));
		
		return info;
	}
	
	public ApplicationMetadata parseApplicationMetadata(JSONObject source) throws Exception {
		ApplicationMetadata meta = new ApplicationMetadata();
		if(source!=null) {
			for(String name: JSONObject.getNames(source)) {
				JSONObject optionJ = source.getJSONObject(name);
				meta.getOptions().add(parseOptionDescription(name, optionJ));
			}
		}
		return meta;
	}
	
	public String parseScriptTemplate(String key, JSONObject idb) {
		return JSONUtils.readMultiLine(key, null, idb);
	}

	public List<ResourceRequest> parseResourceRequest(JSONObject source) throws Exception {
		List<ResourceRequest> req = new ArrayList<ResourceRequest>();
		if(source!=null) {
			for(String name: JSONObject.getNames(source)) {
				String value = source.getString(name);
				req.add(new ResourceRequest(name, value));
			}
		}
		return req;
	}
	
	public OptionDescription parseOptionDescription(String name, JSONObject source) throws JSONException {
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
	
	public Partition parsePartition(JSONObject source) throws JSONException {
		return parsePartition(source.getString("Name"), source);
	}
	
	public Partition parsePartition(String name, JSONObject source) throws JSONException {
		Partition p = new Partition();
		p.setName(name);
		p.setDescription(source.optString("Description"));
		p.setOperatingSystem(source.optString("OperatingSystem", "LINUX"));
		p.setOperatingSystemVersion(source.optString("OperatingSystemVersion", null));
		p.setCpuArchitecture(source.optString("CPUArchitecture", "x86"));
		p.setDefaultPartition(source.optBoolean("IsDefaultPartition", false));
		JSONObject resources = source.getJSONObject("Resources");
		for(String resource: JSONObject.getNames(resources)) {
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

	public Resource createResource(JSONObject doc)
			throws JSONException {
		String name = doc.getString("Name");
		if (name.trim().isEmpty())
			throw new JSONException("Resource 'Name' can not be empty"); 
		return createResource(name, doc);
	}

	
	public Resource createResource(String name, JSONObject doc)
			throws JSONException {
		Resource resource;
		
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
	
	public Resource createIntResource(String name, String valueSpec)
			throws JSONException {
		
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
	
	protected UnitParser getUnitParser(String resourceName) {
		if(ResourceSet.RUN_TIME.equalsIgnoreCase(resourceName)) {
			return UnitParser.getTimeParser(0);
		}
		else {
			return UnitParser.getCapacitiesParser(0);
		}
	}
}
