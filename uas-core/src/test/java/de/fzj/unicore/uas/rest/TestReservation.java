package de.fzj.unicore.uas.rest;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.fzj.unicore.uas.Base;
import eu.unicore.client.Endpoint;
import eu.unicore.client.core.CoreClient;
import eu.unicore.client.core.SiteClient;
import eu.unicore.services.rest.client.UsernamePassword;

/**
 * TBD
 */
public class TestReservation extends Base {

	@Test
	public void test1() throws Exception{
		CoreClient c = new CoreClient(
				new Endpoint(kernel.getContainerProperties().getContainerURL()+"/rest/core"),
				kernel.getClientConfiguration(),
				new UsernamePassword("demouser", "test123"));
		SiteClient tss = c.getSiteFactoryClient().getOrCreateSite();

		//as our config uses a dummy reservation module, we support reservations
		assertTrue(Boolean.parseBoolean(tss.getProperties().getString("supportsReservation")));
			
	}

}
