package eu.unicore.uas.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import eu.unicore.client.Endpoint;
import eu.unicore.client.admin.AdminServiceClient;
import eu.unicore.client.admin.AdminServiceClient.AdminCommand;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.services.restclient.RESTException;
import eu.unicore.services.restclient.UsernamePassword;
import eu.unicore.uas.Base;

public class TestAdmin extends Base {

    @Test
    public void testAdminClient() throws Exception {
		String url = kernel.getServer().getUrls()[0].toExternalForm()+"/rest/admin";
    	var client = new AdminServiceClient(
    			new Endpoint(url),
    			kernel.getClientConfiguration(),
    			getAdminAuth());
    	assertTrue(client.getProperties().toString().contains("connectionStatus"));
    	for(AdminCommand ac: client.getCommands()) {
    		System.out.println(ac);
    	}
    	System.out.println(client.runCommand("ShowServerUsageOverview", null));

    	var c2  =  new AdminServiceClient(
    			new Endpoint(url),
    			kernel.getClientConfiguration(),
    			getAuth());
    	RESTException re = assertThrows(RESTException.class, ()->c2.runCommand("ShowServerUsageOverview", null));
    	assertEquals(403, re.getStatus());
    }

	protected IAuthCallback getAdminAuth() {
		return new UsernamePassword("admin", "test123");
	}
}
