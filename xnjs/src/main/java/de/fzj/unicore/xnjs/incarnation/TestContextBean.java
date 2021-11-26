/*
 * Copyright (c) 2010 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 18-12-2010
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
package de.fzj.unicore.xnjs.incarnation;

import java.util.ArrayList;
import java.util.List;

import de.fzj.unicore.xnjs.ems.ExecutionContext;
import de.fzj.unicore.xnjs.idb.ApplicationInfo;
import de.fzj.unicore.xnjs.resources.ResourceRequest;
import eu.unicore.security.Client;
import eu.unicore.security.Role;
import eu.unicore.security.Xlogin;

public class TestContextBean extends RootCtxBean
{
	public TestContextBean()
	{
		List<ResourceRequest> resourceRequest=new ArrayList<ResourceRequest>();
		setResources(resourceRequest);

		Client client = new Client();
		client.setRole(new Role());
		client.setXlogin(new Xlogin());
		setClient(client);
		
		ApplicationInfo appInfo = new ApplicationInfo();
		appInfo.setName("CAT");
		appInfo.setVersion("1.0");
		
		setApp(appInfo);
		
		setEc(new ExecutionContext());
	}
}
