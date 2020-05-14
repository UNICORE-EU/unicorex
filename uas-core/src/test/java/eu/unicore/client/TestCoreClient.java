package eu.unicore.client;

import org.junit.Test;

import de.fzj.unicore.uas.Base;
import eu.unicore.client.core.CoreClient;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.services.rest.client.UsernamePassword;

public class TestCoreClient extends Base {

	@Test
	public void testClientProperties() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest";
		String resource = url+"/core";
		System.out.println("Accessing "+resource);
		Endpoint ep = new Endpoint(resource);
		IAuthCallback auth = new UsernamePassword("demouser", "test123");
		CoreClient client = new CoreClient(ep, kernel.getClientConfiguration(), auth);
		
		System.out.println("Client info: " +client.getClientInfo().toString(2));
	}

}
