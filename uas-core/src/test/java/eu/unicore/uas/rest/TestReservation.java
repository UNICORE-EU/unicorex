package eu.unicore.uas.rest;

import org.junit.Test;

import eu.unicore.client.Job;
import eu.unicore.client.Job.Resources;
import eu.unicore.services.rest.client.BaseClient;
import eu.unicore.services.rest.client.UsernamePassword;
import eu.unicore.uas.Base;

public class TestReservation extends Base {

	@Test
	public void test1() throws Exception{
		BaseClient c = new BaseClient(
				kernel.getContainerProperties().getContainerURL()+"/rest/core/reservations",
				kernel.getClientConfiguration(),
				new UsernamePassword("demouser", "test123"));
		Job j = new Job();
		Resources r = j.resources();
		r.runtime("60m");
		r.nodes(1);
		String url = c.create(j.getJSON());
		BaseClient c2 = new BaseClient(url, kernel.getClientConfiguration(),
				new UsernamePassword("demouser", "test123"));
		System.out.println(c2.getJSON().toString(2));
	}

}
