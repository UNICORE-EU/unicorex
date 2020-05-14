/*
 * Copyright (c) 2010 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 2010-11-14
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
package de.fzj.unicore.uas.security;

import de.fzj.unicore.wsrflite.ContainerProperties;
import de.fzj.unicore.wsrflite.security.IContainerSecurityConfiguration;
import de.fzj.unicore.wsrflite.security.pdp.ActionDescriptor;
import de.fzj.unicore.wsrflite.security.pdp.PDPResult;
import de.fzj.unicore.wsrflite.security.pdp.PDPResult.Decision;
import de.fzj.unicore.wsrflite.security.pdp.UnicoreXPDP;
import de.fzj.unicore.wsrflite.security.util.ResourceDescriptor;
import eu.unicore.security.Client;
import eu.unicore.util.httpclient.IClientConfiguration;

public class MockPDP implements UnicoreXPDP
{
	public MockPDP() {}

	public static boolean allowAll = false;
	
	public void setAllowAll(boolean what){
		allowAll = what;
	}
	
	@Override
	public PDPResult checkAuthorisation(Client c, ActionDescriptor action,
			ResourceDescriptor d) throws Exception
	{
		if(allowAll)return new PDPResult(Decision.PERMIT, "");
		
		if (d.getServiceName().equals("testPlainWSAuthz"))
			return new PDPResult(Decision.DENY, "");
		
		if (c.getRole().getName().equals("user"))
			return new PDPResult(Decision.PERMIT, "");
		
		return new PDPResult(Decision.DENY, "");
	}

	@Override
	public void initialize(String configuration, ContainerProperties baseSettings,
			IContainerSecurityConfiguration securityConfiguration,
			IClientConfiguration clientConfiguration) throws Exception
	{
	}
}
