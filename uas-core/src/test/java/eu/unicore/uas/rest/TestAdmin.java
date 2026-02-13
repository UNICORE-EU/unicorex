package eu.unicore.uas.rest;

import org.junit.jupiter.api.Test;

import eu.unicore.client.Endpoint;
import eu.unicore.client.admin.AdminServiceClient;
import eu.unicore.client.admin.AdminServiceClient.AdminCommand;
import eu.unicore.uas.Base;

public class TestAdmin extends Base {

    @Test
    public void testAdminClient() throws Exception {
		String url = kernel.getServer().getUrls()[0].toExternalForm()+"/rest/admin";
    	AdminServiceClient client = new AdminServiceClient(
    			new Endpoint(url),
    			kernel.getClientConfiguration(),
    			null);
    	assert client.getProperties().toString().contains("connectionStatus");
    	for(AdminCommand ac: client.getCommands()) {
    		System.out.println(ac);
    	}
    	System.out.println(client.runCommand("ShowServerUsageOverview", null));
    }

}
