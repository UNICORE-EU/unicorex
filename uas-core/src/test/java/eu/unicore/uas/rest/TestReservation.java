package eu.unicore.uas.rest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import eu.unicore.client.Job;
import eu.unicore.client.Job.Resources;
import eu.unicore.services.restclient.BaseClient;
import eu.unicore.services.restclient.UsernamePassword;
import eu.unicore.uas.Base;
import eu.unicore.uas.xnjs.MockReservation;

public class TestReservation extends Base {

	@Test
	public void test1() throws Exception{
		BaseClient c = new BaseClient(
				kernel.getContainerProperties().getContainerURL()+"/rest/core/reservations",
				kernel.getClientConfiguration(),
				getAuth());
		Job j = new Job();
		Resources r = j.resources();
		r.runtime("60m");
		r.nodes(1);
		String url = c.create(j.getJSON());
		BaseClient c2 = new BaseClient(url, kernel.getClientConfiguration(),
				new UsernamePassword("demouser", "test123"));
		System.out.println(c2.getJSON().toString(2));
		String reservationID = c2.getJSON().getString("reservationID");
		assertTrue(MockReservation.hasReservation(reservationID));
		c2.delete();
		assertFalse(MockReservation.hasReservation(reservationID));
	}

}