package eu.unicore.client;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import de.fzj.unicore.uas.Base;
import eu.unicore.client.registry.RegistryClient;

public class TestRegistryClient extends Base {

	@Test
	public void testRegistryProperties() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest";
		String resource = url+"/registries/default_registry";
		System.out.println("Accessing "+resource);
		
		RegistryClient client = new RegistryClient(resource, kernel.getClientConfiguration(), null);
		
		List<Endpoint> entries = client.listEntries();
		
		boolean found = false;
		for (int i=0; i<entries.size(); i++){
			Endpoint ep = entries.get(i);
			System.out.println(ep.getUrl()+" : "+ep.getInterfaceName());
			if(ep.getInterfaceName().equals("CoreServices")){
				found = true;
				break;
			}
		}
		assertTrue("CoreServices not in registry", found);
		
		Endpoint core = client.listEntries(
				new RegistryClient.ServiceTypeFilter("CoreServices")).get(0);
		
		System.out.println("Found: " +core.getUrl());
	}

}
