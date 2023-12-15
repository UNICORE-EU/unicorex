package de.fzj.unicore.uas.security;

import eu.unicore.services.ContainerProperties;
import eu.unicore.services.security.IContainerSecurityConfiguration;
import eu.unicore.services.security.pdp.ActionDescriptor;
import eu.unicore.services.security.pdp.PDPResult;
import eu.unicore.services.security.pdp.PDPResult.Decision;
import eu.unicore.services.security.pdp.UnicoreXPDP;
import eu.unicore.services.security.util.ResourceDescriptor;
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
