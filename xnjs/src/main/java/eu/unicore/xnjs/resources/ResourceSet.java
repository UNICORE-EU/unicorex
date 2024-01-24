package eu.unicore.xnjs.resources;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.unicore.xnjs.resources.Resource.Category;


/**
 * a set of resources
 *
 * @author schuller
 */
public class ResourceSet implements Serializable {

	private static final long serialVersionUID=1L;
	
	public static final String MEMORY_PER_NODE="MemoryPerNode";
	public static final String RUN_TIME="Runtime";
	
	public static final String CPUS_PER_NODE="CPUsPerNode";
	public static final String TOTAL_CPUS="TotalCPUs";
	public static final String NODES="Nodes";

	public static final String GPUS_PER_NODE="GPUsPerNode";

	public static final String RESERVATION_ID="Reservation";

	public static final String ARRAY_SIZE = "ArraySize";
	public static final String ARRAY_LIMIT = "ArrayLimit";

	public static final String QUEUE = "Queue";
	public static final String PROJECT = "Project";
	public static final String NODE_CONSTRAINTS = "NodeConstraints";
	public static final String QOS = "QoS";
	public static final String EXCLUSIVE="Exclusive";
	public static final String ALLOCATION = "Allocation";
	
	protected final Map<String,Resource>resources = new HashMap<>();

	/**
	 * get the named resource
	 * @param name - the name of the resource
	 */
	public Resource getResource(String name){
		return resources.get(name);
	}
	
	/**
	 * add the specified resource to this set, overwriting a potential previous value
	 */
	public void putResource(Resource value){
		resources.put(value.getName(),value);
	}

	/**
	 * returns an immutable view on the resources in this set
	 */
	public Collection<Resource> getResources(){
		return Collections.unmodifiableCollection(resources.values());
	}

	public String toString(){
		return "ResourceSet "+resources.keySet();
	}
	
	public List<ResourceRequest> getDefaults(){
		List<ResourceRequest> defs = new ArrayList<>();
		for(Resource r: resources.values()) {
			if(r.getStringValue()!=null) {
				defs.add(new ResourceRequest(r.getName(), r.getStringValue()));
			}
		}
		return defs;
	}
	
	private static final String[] compute = { 
			CPUS_PER_NODE, TOTAL_CPUS, NODES, };
	
	public static Category getCategory(String name) {
		for(String c:compute) {
			if(c.equalsIgnoreCase(name))return Category.PROCESSING;
		}
		if(MEMORY_PER_NODE.equalsIgnoreCase(name))return Category.MEMORY;
		if(RUN_TIME.equalsIgnoreCase(name))return Category.TIME;
		return Category.OTHER;
	}
}
