/*
 * Copyright (c) 2010 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 19-12-2010
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
package de.fzj.unicore.xnjs.incarnation;

import java.util.List;

import org.ggf.schemas.jsdl.x2005.x11.jsdl.ResourcesType;

import de.fzj.unicore.xnjs.jsdl.JSDLResourceSet;
import de.fzj.unicore.xnjs.resources.ResourceRequest;
import de.fzj.unicore.xnjs.resources.ResourceSet;

/**
 * Wrapper around JavaBeans {@link ResourcesType}. Provides methods to set and get exact
 * values in a simple way from SpEL and Groovy.
 * 
 * @author golbi
 */
public class ResourcesWrapper
{
	private final List<ResourceRequest> wrapped;

	public ResourcesWrapper(List<ResourceRequest>wrapped)
	{
		this.wrapped = wrapped;
	}
	
	public Double getIndividualCPUCount()
	{
		return safeGetValue(ResourceRequest.find(wrapped,JSDLResourceSet.CPUS_PER_NODE));
	}
	
	public void setIndividualCPUCount(double value)
	{
		safeSetValue(JSDLResourceSet.CPUS_PER_NODE, String.valueOf(value));
	}
	
	public Double getTotalResourceCount()
	{
		return safeGetValue(JSDLResourceSet.NODES);
	}
	
	public void setTotalResourceCount(double value)
	{
		safeSetValue(JSDLResourceSet.NODES, String.valueOf(value));
	}
	
	public Double getTotalCPUCount()
	{
		return safeGetValue(JSDLResourceSet.TOTAL_CPUS);
	}
	
	public void setTotalCPUCount(double value)
	{
		safeSetValue(JSDLResourceSet.TOTAL_CPUS, String.valueOf(value));
	}
	
	public Double getIndividualPhysicalMemory()
	{
		return safeGetValue(JSDLResourceSet.MEMORY_PER_NODE);
	}
	
	public void setIndividualPhysicalMemory(double value)
	{
		safeSetValue(JSDLResourceSet.MEMORY_PER_NODE, String.valueOf(value));
	}

	public Double getIndividualCPUTime()
	{
		return safeGetValue(JSDLResourceSet.RUN_TIME);
	}
	
	public void setIndividualCPUTime(double value)
	{
		safeSetValue(JSDLResourceSet.RUN_TIME, String.valueOf(value));
	}
	
	public void setQueue(String queue)
	{
		safeSetValue(ResourceSet.QUEUE, queue);
	}
	
	public String getQueue()
	{
		ResourceRequest queue=ResourceRequest.find(wrapped,ResourceSet.QUEUE);
		return queue!=null ? queue.getRequestedValue() : null;
	}
	
	public String getReservationId()
	{
		ResourceRequest reservation=ResourceRequest.find(wrapped,JSDLResourceSet.RESERVATION_ID);
		return reservation!=null ? reservation.getRequestedValue() : null;
	}
	
	public void setReservationId(String reservation)
	{
		safeSetValue(JSDLResourceSet.RESERVATION_ID, reservation);
	}
	
	public List<ResourceRequest> getAllResources()
	{
		return wrapped;
	}
	
	//internal utility methods
	private Double safeGetValue(String name)
	{
		return safeGetValue(ResourceRequest.find(wrapped, name));
	}
	
	private Double safeGetValue(ResourceRequest from)
	{
		return from!=null? Double.valueOf(from.getRequestedValue()) : null;
	}
	
	private void safeSetValue(String name, String value)
	{
		ResourceRequest req = ResourceRequest.find(wrapped, name);
		if(req==null){
			req=new ResourceRequest(name, value);
			wrapped.add(req);
		}
		else{
			req.setRequestedValue(value);
		}
	}
	
}
