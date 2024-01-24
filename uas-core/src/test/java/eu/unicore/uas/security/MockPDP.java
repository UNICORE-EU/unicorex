package eu.unicore.uas.security;

import eu.unicore.security.Client;
import eu.unicore.services.security.pdp.ActionDescriptor;
import eu.unicore.services.security.pdp.PDPResult;
import eu.unicore.services.security.pdp.PDPResult.Decision;
import eu.unicore.services.security.pdp.UnicoreXPDP;
import eu.unicore.services.security.util.ResourceDescriptor;

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

}
