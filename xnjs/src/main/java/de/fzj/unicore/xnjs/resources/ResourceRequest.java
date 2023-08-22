package de.fzj.unicore.xnjs.resources;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.fzj.unicore.xnjs.resources.Resource.Category;

/**
 * Represents a client request for a resource required to run a job.
 * It is read from the job description. Immediately before submission, 
 * the set of resource requests is checked against the site's resource 
 * limits, and "incarnated" into the actual set of resources,
 * also taking any site defaults into account. 
 *
 * @author schuller
 */
public class ResourceRequest implements Serializable  {
	
	private static final long serialVersionUID=1L;
	
	private final String name;
	
	private String requestedValue;
	
	public ResourceRequest(String name, String value){
		this.name=name;
		this.requestedValue=value;
	}
	
	public String getName() {
		return name;
	}

	public String getRequestedValue() {
		return requestedValue;
	}
	
	public void setRequestedValue(String value) {
		requestedValue = value;
	}

	public String toString(){
		return name+"="+requestedValue;
	}

	public int toInt() {
		Double d = Double.parseDouble(String.valueOf(requestedValue));
		return (int)Math.round(d);
	}

	public int toIntSafe() {
		try {
			return toInt();
		}catch(RuntimeException re) { return -1; }
	}

	/**
	 * helper to check whether a certain resource has been requested
	 * @param req - the set of resources
	 * @param name - the name to check for
	 * @return <code>true</code> if the given collection contains a request for the specified resource 
	 */
	public static boolean contains(Collection<ResourceRequest>req, String name){
		for(ResourceRequest r: req){
			if(r.getName().equals(name))return true;
		}
		return false;
	}

	public static boolean contains(Collection<ResourceRequest>req, Category name){
		for(ResourceRequest r: req){
			if(ResourceSet.getCategory(r.getName()).equals(name))return true;
		}
		return false;
	}
	
	public static ResourceRequest find(Collection<ResourceRequest>req, String name){
		for(ResourceRequest r: req){
			if(r.getName().equals(name))return r;
		}
		return null;
	}
	
	public static ResourceRequest findAndRemove(Collection<ResourceRequest>req, String name){
		for(ResourceRequest r: req){
			if(r.getName().equals(name)) {
				req.remove(r);
				return r;
			}
		}
		return null;
	}

	public static void removeQuietly(Collection<ResourceRequest>req, String name){
		findAndRemove(req, name);
	}

	public static List<ResourceRequest> merge(Collection<ResourceRequest > defaults, Collection<ResourceRequest>overrides){
		 List<ResourceRequest> merged = new ArrayList<>();
		 merged.addAll(overrides);
		 if(defaults!=null && defaults.size()>0) {
			 for(ResourceRequest rr: defaults){
				 if(!contains(merged, rr.getName())) {
					 merged.add(rr);
				 }
			 }
		 }
		 return merged; 
	}
}
