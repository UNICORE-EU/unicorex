package de.fzj.unicore.xnjs.resources;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.fzj.unicore.xnjs.resources.Resource.Category;


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
	public static final String RESERVATION_ID="Reservation";
	public static final String ARRAY_SIZE = "ArraySize";
	public static final String ARRAY_LIMIT = "ArrayLimit";

	public static final String QUEUE = "Queue";
	public static final String PROJECT = "Project";
	public static final String NODE_CONSTRAINTS = "NodeConstraints";
	public static final String QOS = "QoS";
	
	protected final Map<String,Resource>resources=new HashMap<String,Resource>();

	private final List<String>unmatched=new ArrayList<String>();

	/**
	 * remove all entries in this resource set
	 */
	public void clear(){
		resources.clear();
		unmatched.clear();
	}
	
	/**
	 * return a deep copy of this resource set
	 */
	public ResourceSet copy(){
		return copy(false);
	}
	
	/**
	 * return a deep copy of this resource set
	 */
	public ResourceSet copy(boolean onlyDefaults){
		ResourceSet s=new ResourceSet();
		for(Entry<String,Resource>e:resources.entrySet()){
			if(onlyDefaults && e.getValue().getValue()==null)continue;
			s.resources.put(e.getKey(), e.getValue().copy());
		}
		return s;
	}
	
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
	 * add all specified resources to this set, overwriting potential previous values
	 */
	public void putAllResources(Collection<Resource> values){
		for(Resource r: values){
			resources.put(r.getName(),r);
		}
	}
	
	/**
	 * remove the specified resource
	 * @param name - the name of the resource to remove
	 * @return the resource previously stored under the given name, or <code>null</code>
	 */
	public Resource removeResource(String name){
		return resources.remove(name);
	}

	/**
	 * returns an immutable view on the resources in this set
	 */
	public Collection<Resource> getResources(){
		return Collections.unmodifiableCollection(resources.values());
	}
	
	/**
	 * checks if the resources in the "other" set are within the bounds as
	 * given by this set
	 * @param other - the ResourceSet to check
	 */
	public boolean checkBounds(ResourceSet other){
		boolean ok=true;
		for(Resource r: other.resources.values()){
			Resource def=resources.get(r.getName());
			if(def!=null && !def.isInRange(r)){
				other.markUnmatchedResource(r.getName());
				ok=false;
			}
		}
		return ok;
	}

	/**
	 * check whether this set contains a resource of the requested category
	 * @param category
	 */
	public boolean containsResource(Category category){
		boolean result=false;
		for(Resource r: resources.values()){
			if(category.equals(r.getCategory())){
				result=true;
				break;
			}
		}
		return result;
	}

	public void markUnmatchedResource(String name){
		unmatched.add(name);
	}

	public List<String>getUnmatchedResourceNames(){
		return unmatched;
	}

	public String printAsRequest(){
		StringBuilder sb=new StringBuilder();
		for(Resource r: resources.values()){
			sb.append(r.getName()+" = "+r.getStringValue()).append("\n");
		}
		return sb.toString();
	}
	

	public String printSelected(){
		StringBuilder sb=new StringBuilder();
		boolean first=true;
		for(Resource r: resources.values()){
			if(!first){
				sb.append(", ");
			}else first=false;
			
			sb.append(r.getName()).append(" = ").append(r.getStringValue());
		}
		return sb.toString();
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
	
	protected void checkConsistency(){
		Resource totalCPUs=getResource(TOTAL_CPUS);
		boolean haveTotalCPUs=totalCPUs!=null && totalCPUs.getValue()!=null;
		Resource nodesRes=getResource(NODES);
		boolean haveNodes=nodesRes!=null && nodesRes.getValue()!=null;
		Resource CPUsPerNode=getResource(CPUS_PER_NODE);
		boolean haveCPUsPerNode = CPUsPerNode!=null && CPUsPerNode.getValue()!=null;

		if(haveTotalCPUs && haveNodes && haveCPUsPerNode){
			int nodes = nodesRes.getDoubleValue().intValue();
			int ppn = CPUsPerNode.getDoubleValue().intValue();
			int total = totalCPUs.getDoubleValue().intValue();
			if(total != nodes * ppn){
				throw new IllegalArgumentException("Both TotalCPUs and (Nodes + CPUsPerNode) found, and values are inconsistent.");
			}
		}
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
