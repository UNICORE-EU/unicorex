package de.fzj.unicore.uas.rest;

import java.util.Map;

import org.junit.Test;

import de.fzj.unicore.uas.Base;
import eu.unicore.client.Endpoint;
import eu.unicore.client.admin.AdminServiceClient;
import eu.unicore.client.admin.AdminServiceClient.AdminCommand;

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
    	for(Map.Entry<String, String> e: client.getMetrics().entrySet()) {
    		System.out.println(e.getKey()+": "+e.getValue());
    	}
    	System.out.println(client.runCommand("ShowServerUsageOverview", null));
    }

}
