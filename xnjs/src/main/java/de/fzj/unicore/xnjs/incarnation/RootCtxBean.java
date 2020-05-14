/*
 * Copyright (c) 2010 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 14-12-2010
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
package de.fzj.unicore.xnjs.incarnation;

import java.util.List;

import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.ExecutionContext;
import de.fzj.unicore.xnjs.idb.ApplicationInfo;
import de.fzj.unicore.xnjs.idb.IDB;
import de.fzj.unicore.xnjs.resources.ResourceRequest;
import eu.unicore.security.Client;

public class RootCtxBean 
{
	private IDB idb;

	private ApplicationInfo app;
	
	private ResourcesWrapper resources;
	private Client client;
	private ExecutionContext ec;
	
	public RootCtxBean() 
	{
	}
	
	public RootCtxBean(ApplicationInfo app, Action action, IDB idb) 
	{
		this.app = app;
		List<ResourceRequest>req=action.getExecutionContext().getResourceRequest();
		this.resources = new ResourcesWrapper(req);
		this.client = action.getClient();
		this.ec = action.getExecutionContext();
		this.idb = idb;
	}
	
	public void setIdb(IDB idb)
	{
		this.idb = idb;
	}
	public void setClient(Client client)
	{
		this.client = client;
	}
	public void setEc(ExecutionContext ec)
	{
		this.ec = ec;
	}
	public IDB getIdb()
	{
		return idb;
	}
	public Client getClient()
	{
		return client;
	}
	public ExecutionContext getEc()
	{
		return ec;
	}
	public void setApp(ApplicationInfo app) 
	{
		this.app = app;
	}
	public ApplicationInfo getApp() 
	{
		return app;
	}
	public void setResources(List<ResourceRequest> resources)
	{
		this.resources = new ResourcesWrapper(resources);
	}
	public ResourcesWrapper getResources()
	{
		return resources;
	}
}