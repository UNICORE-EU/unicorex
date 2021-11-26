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
import de.fzj.unicore.xnjs.jsdl.JSDLResourceSet;
import de.fzj.unicore.xnjs.resources.ResourceRequest;
import eu.unicore.security.Client;
import eu.unicore.security.SecurityTokens;
import eu.unicore.security.Xlogin;

public class MockContextBean extends RootCtxBean
{
	public MockContextBean()
	{
		List<ResourceRequest> resourceRequest=new ArrayList<ResourceRequest>();
		resourceRequest.add(new ResourceRequest(JSDLResourceSet.RUN_TIME, "100"));
		setResources(resourceRequest);
		
		setClient(createClient());
		
		ApplicationInfo appInfo = new ApplicationInfo();
		appInfo.setName("CAT");
		appInfo.setVersion("1.0");
		
		setApp(appInfo);
		
		setEc(new ExecutionContext());
	}
	

	private Client createClient(){
		Client c=new Client();
		c.setXlogin(new Xlogin(new String[] {"nobody"}));
		SecurityTokens st=new SecurityTokens();
		st.setUserName("CN=Test,O=TestOrg,C=PL");
		st.setConsignorTrusted(true);
		c.setAuthenticatedClient(st);
		return c;
	}
}
