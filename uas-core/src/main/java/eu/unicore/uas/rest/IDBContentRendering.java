package eu.unicore.uas.rest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.unicore.security.Client;
import eu.unicore.services.security.util.AuthZAttributeStore;
import eu.unicore.xnjs.ems.BudgetInfo;
import eu.unicore.xnjs.idb.ApplicationMetadata;
import eu.unicore.xnjs.idb.OptionDescription;
import eu.unicore.xnjs.idb.Partition;
import eu.unicore.xnjs.resources.BooleanResource;
import eu.unicore.xnjs.resources.RangeResource;
import eu.unicore.xnjs.resources.Resource;
import eu.unicore.xnjs.resources.ResourceSet;
import eu.unicore.xnjs.resources.ValueListResource;

/**
 * for generating/parsing the JSON representation of a resource set,
 * applications etc
 *
 * @author schuller
 */
public class IDBContentRendering {

	public static final String appSeparator = "---v";

	public static Map<String, Object> asMap(ResourceSet rs){
		Map<String,Object> resources = new HashMap<>();
		try{
			for(Resource r: rs.getResources()){
				resources.put(r.getName(), render(r));
			}
		}
		catch(Exception e){}
		return resources;
	}

	public static Map<String, Object> asMap(List<Partition> partitions){
		Map<String,Object> resources = new HashMap<>();
		try{
			for(Partition p: partitions){
				if(isAvailable(p.getName())) {
					resources.put(p.getName(), asMap(p.getResources()));
				}
			}
		}
		catch(Exception e){}
		return resources;
	}

	private static Object render(Resource r){
		if(r instanceof RangeResource){
			RangeResource dr = (RangeResource)r;
			return dr.getLower()+"-"+dr.getUpper();
		}
		if(r instanceof ValueListResource){
			ValueListResource vlr = (ValueListResource)r;
			return Arrays.asList(vlr.getValidValues().clone());
		}
		if(r instanceof BooleanResource){
			return new String[]{"true","false"};
		}
		return "*";
	}

	public static Map<String,Object> asMap(ApplicationMetadata meta){
		Map<String,Object> result = new HashMap<>();
		for(OptionDescription opt: meta.getOptions()){
			if(opt.getName()!=null)result.put(opt.getName(), asMap(opt));
		}
		return result;
	}

	public static Map<String,Object> asMap(OptionDescription opt){
		Map<String,Object> result = new HashMap<>();
		result.put("Description", opt.getDescription());
		result.put("Type", String.valueOf(opt.getType()));
		String[] vv = opt.getValidValues();
		if(vv!=null) {
			result.put("ValidValues", vv);
		}
		return result;
	}

	public static boolean isAvailable(String partition){
		Client client = AuthZAttributeStore.getClient();
		if (client!=null && 
				client.getQueue()!=null && 
				client.getQueue().getValidQueues()!=null &&
				client.getQueue().getValidQueues().length>0)
		{
			for(String q: client.getQueue().getValidQueues()) {
				if(q.equals(partition))return true;
			}
			return false;
		}
		return true;
	}

	public static Map<String,Map<String,String>> budgetToMap(List<BudgetInfo> budget) {
		Map<String,Map<String,String>>result = new HashMap<>();
		for(BudgetInfo info: budget) {
			Map<String,String> j = new HashMap<>();
			j.put("remaining", String.valueOf(info.getRemaining()));
			j.put("units", info.getUnits());
			j.put("rel", info.getPercentRemaining()+"%");
			result.put(info.getProjectName(), j);
		}
		return result;
	}
}