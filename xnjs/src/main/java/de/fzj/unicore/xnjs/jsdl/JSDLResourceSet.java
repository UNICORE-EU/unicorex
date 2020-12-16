package de.fzj.unicore.xnjs.jsdl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.logging.log4j.Logger;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.RangeType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.RangeValueType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.ResourcesType;

import de.fzj.unicore.xnjs.beans.idb.ResourceDocument;
import de.fzj.unicore.xnjs.resources.DoubleResource;
import de.fzj.unicore.xnjs.resources.IntResource;
import de.fzj.unicore.xnjs.resources.RangeResource;
import de.fzj.unicore.xnjs.resources.ReservationResource;
import de.fzj.unicore.xnjs.resources.Resource;
import de.fzj.unicore.xnjs.resources.Resource.Category;
import de.fzj.unicore.xnjs.resources.ResourceRequest;
import de.fzj.unicore.xnjs.resources.ResourceSet;
import de.fzj.unicore.xnjs.resources.StringResource;
import de.fzj.unicore.xnjs.tsi.remote.TSIUtils;
import de.fzj.unicore.xnjs.util.LogUtil;
import eu.unicore.jsdl.extensions.ResourceRequestDocument;

/**
 * helper for handling JSDL resource specifications
 * 
 * @author schuller
 */
public class JSDLResourceSet extends ResourceSet {

	private static final long serialVersionUID=1L;

	private static final Logger logger=LogUtil.getLogger(LogUtil.JOBS,JSDLResourceSet.class);

	protected final List<ResourceRequest>extensionJSDLResources=new ArrayList<ResourceRequest>();


	/**
	 * QName of the XML element for representing a reservation reference.
	 */
	public static final QName RESERVATION_REFERENCE = new QName("http://www.unicore.eu/unicore/xnjs","ReservationReference");
	
	
	public JSDLResourceSet(ResourcesType source){
		super();
		if(source!=null)init(source);
	}

	public JSDLResourceSet(){}
	
	public List<ResourceRequest>getExtensionJSDLResources(){
		return extensionJSDLResources;
	}

	protected void init(ResourcesType source){
		if(source.getIndividualPhysicalMemory()!=null){	
			Resource r=createNumericResource(MEMORY_PER_NODE, source.getIndividualPhysicalMemory(),Category.MEMORY);
			if(r!=null)resources.put(MEMORY_PER_NODE,r);
		}
		if(source.getIndividualCPUTime()!=null){
			Resource r=createNumericResource(RUN_TIME, source.getIndividualCPUTime(),Category.TIME);
			if(r!=null)resources.put(RUN_TIME,r);
		}
		if(source.getIndividualCPUCount()!=null){
			Resource r=createNumericResource(CPUS_PER_NODE,source.getIndividualCPUCount(),Category.PROCESSING);
			if(r!=null)resources.put(CPUS_PER_NODE,r);
		}
		if(source.getTotalCPUCount()!=null){
			Resource r=createNumericResource(TOTAL_CPUS, source.getTotalCPUCount(),Category.PROCESSING);
			if(r!=null)resources.put(TOTAL_CPUS,r);
		}
		if(source.getTotalResourceCount()!=null){
			Resource r=createNumericResource(NODES, source.getTotalResourceCount(),Category.PROCESSING);
			if(r!=null)resources.put(NODES,r);
		}
		String reservation=TSIUtils.extractReservationID(source);
		if(reservation!=null){
			resources.put(RESERVATION_ID,new ReservationResource(reservation));
		}
		
		processNonJSDLResources(source);

		checkConsistency();
	}

	protected Float getMemoryPerNode(ResourcesType resources){
		try{
			if(resources.getIndividualPhysicalMemory()!=null && resources.getIndividualPhysicalMemory().getExactArray()!=null){
				Float f=Float.valueOf(resources.getIndividualPhysicalMemory().getExactArray()[0].getStringValue());
				return f;
			}
		}catch(Exception e){
			logger.warn("IndividualPhysicalMemory (memory per node) setting in IDB: not a float.");
		}
		return null;
	}

	protected Float getCPUTime(ResourcesType resources){
		try{
			if(resources.getIndividualCPUTime()!=null && resources.getIndividualCPUTime().getExactArray().length>0){
				Float f=Float.valueOf(resources.getIndividualCPUTime().getExactArray()[0].getStringValue());
				return f;
			}
		}catch(Exception e){
			logger.warn("Individual cpu time setting in IDB: not a float.");
		}
		return null;
	}

	protected Float getNodes(ResourcesType resources){	
		try{
			if(resources.getTotalResourceCount()!=null && resources.getTotalResourceCount().getExactArray().length>0){
				Float f=Float.valueOf(resources.getTotalResourceCount().getExactArray()[0].getStringValue());
				return f;
			}
		}catch(Exception e){
			logger.warn("Total resource count (nodes) setting in IDB: not a float.");
		}
		return null;
	}

	protected Float getCPUsPerNode(ResourcesType resources){	
		try{
			if(resources.getIndividualCPUCount()!=null && resources.getIndividualCPUCount().getExactArray().length>0){
				Float f=Float.valueOf(resources.getIndividualCPUCount().getExactArray()[0].getStringValue());
				return f;
			}
		}catch(Exception e){
			e.printStackTrace();
			logger.warn("Individual CPU count (CPUs per nodes) setting in IDB: not a float.");
		}
		return null;
	}

	protected Float getTotalCPUs(ResourcesType resources){	
		try{
			if(resources.getTotalCPUCount()!=null && resources.getTotalCPUCount().getExactArray().length>0){
				Float f=Float.valueOf(resources.getTotalCPUCount().getExactArray()[0].getStringValue());
				return f;
			}
		}catch(Exception e){
			logger.warn("Total CPU count setting in IDB: not a float.");
		}
		return null;
	}

	/**
	 * process the "site-specific" i.e. non-JSDL resources
	 * @param res - the JSDL resources possibly including non-JSDL ones
	 */
	protected void processNonJSDLResources(ResourcesType res){
		try{
			List<ResourceDocument>resources=JSDLUtils.extractResourceSpecification(res);
			for(ResourceDocument rs: resources){
				addResource(rs);
			}
		}catch(Exception e){
			logger.warn("Error parsing site specific resources",e);
		}
		//in incoming jsdl, we also have resource requests
		try{
			List<ResourceRequestDocument>resources=JSDLUtils.extractResourceRequest(res);
			for(ResourceRequestDocument rs: resources){
				String name=rs.getResourceRequest().getName();
				String value=rs.getResourceRequest().getValue();
				ResourceRequest rr=new ResourceRequest(name,value);
				extensionJSDLResources.add(rr);
			}
		}catch(Exception e){
			logger.warn("Error parsing resource requests",e);
		}
	}


	/**
	 * adds a site-specific resource
	 * 
	 * @param rs - ResourceSetting
	 * @throws Exception  
	 */
	private void addResource(ResourceDocument rs) throws Exception {
		ResourceDocument.Resource res=rs.getResource();
		Resource resource;
		resource = JSDLUtils.createResource(rs);
		String name=res.getName();
		resources.put(name, resource);
	}

	public Double getDoubleResourceValue(String name){
		Resource r=getResource(name);
		if(r!=null){
			return ((DoubleResource)r).getValue();
		}
		return null;
	}

	
	public String toString(){
		StringBuilder sb=new StringBuilder();
		for(Resource r: resources.values()){
			sb.append(r.toString()).append("\n");
		}
		return sb.toString();
	}
	

	/**
	 * create a new {@link Resource} from the given JSDL RangeValue
	 * 
	 * @param name - resource name
	 * @param rvt - JSDL range/value
	 * @param category - resource {@link Category}
	 * @return a {@link Resource} or <code>null</code> if needDefaultValue is <code>true</code> and there
	 * is no default set
	 */
	public Resource createNumericResource(String name, RangeValueType rvt, Category category){
		Resource r = null;
		if(JSDLUtils.hasExpression(rvt)){
			String expr = JSDLUtils.getExpression(rvt);
			r = new StringResource(name, expr, category);
		}
		else{
			Double defaultValue=rvt.getExactArray().length>0?Double.valueOf(rvt.getExactArray()[0].getStringValue()):null;
			Long lower=null;
			Long upper=null;
			if(rvt.getRangeArray().length>0){
				RangeType range=rvt.getRangeArray()[0];
				if(range.getLowerBound()!=null){
					lower=Double.valueOf(range.getLowerBound().getStringValue()).longValue();
				}
				if(range.getUpperBound()!=null){
					upper=Double.valueOf(range.getUpperBound().getStringValue()).longValue();
				}
			}
			Long def = defaultValue==null? null : defaultValue.longValue(); 
			r = new IntResource(name, def, upper, lower, category);
		}
		return r;
	}

	private static Collection<String>jsdlResourceNames=new HashSet<String>();
	
	static{
		jsdlResourceNames.add(MEMORY_PER_NODE);
		jsdlResourceNames.add(TOTAL_CPUS);
		jsdlResourceNames.add(NODES);
		jsdlResourceNames.add(CPUS_PER_NODE);
		jsdlResourceNames.add(RUN_TIME);
	}
	
	/**
	 * check whether the given resource name is one of the standard JSDL resources
	 * (Nodes, etc)
	 * 
	 * @param resourceName - the name to check
	 * @return true if the resource name matches a standard JSDL resource name. For example, "Nodes" is a standard JSDL resource
	 * name (corresponding to TotalResourceCount)
	 */
	public static boolean isJSDLResourceName(String resourceName){
		return jsdlResourceNames.contains(resourceName);
	}
	
	/**
	 * Validate the default values of all resources in this set.
	 * For each resource where the default value is NOT within the allowed range,
	 * emit an error message, and re-set the default to the minimum value of the
	 * given range
	 * 
	 * @return a non zero list of error messages
	 */
	public List<String>validateDefaults(){
		List<String>errors=new ArrayList<String>();
		for(Map.Entry<String, Resource> e: resources.entrySet()){
			Resource r=e.getValue();
			if(r.getValue()!=null){
				Object o=r.getValue();
				if(!r.isInRange(o)){
					String msg="Resource "+r.getName()+": default value <"+o+"> is not in the validity range!";
					if(r instanceof RangeResource){
						r.setStringValue(String.valueOf(((RangeResource)r).getLower()));
						msg += " Resetting default to "+r.getStringValue();
					}
					errors.add(msg);
				}
			}
		}
		return errors;
	}

}
