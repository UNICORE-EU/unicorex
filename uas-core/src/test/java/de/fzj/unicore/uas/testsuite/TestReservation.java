package de.fzj.unicore.uas.testsuite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.Date;

import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.ResourcesDocument;
import org.unigrids.x2006.x04.services.reservation.ReservationPropertiesDocument;
import org.unigrids.x2006.x04.services.tss.SubmitDocument;

import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.client.JobClient;
import de.fzj.unicore.uas.client.ReservationClient;
import de.fzj.unicore.uas.xnjs.MockReservation;
import de.fzj.unicore.wsrflite.impl.DefaultHome;
import eu.unicore.bugsreporter.annotation.FunctionalTest;

public class TestReservation extends RunDate {

	@Override
	@FunctionalTest(id="ReservationTest", description="Tests the reservation management WS interface.")
	public void testRunJob()throws Exception{
		initClients();

		//as our config uses a dummy reservation module, we support reservations
		assertTrue(tss.supportsReservation());
		ResourcesDocument resources=ResourcesDocument.Factory.newInstance();
		resources.addNewResources().addNewIndividualCPUTime().addNewExact().setDoubleValue(7200);
		Calendar startTime=Calendar.getInstance();
		startTime.add(Calendar.MONTH,1);
		while(!"READY".equals(tss.getServiceStatus())){
			Thread.sleep(1000);
		}
		ReservationClient reservation=tss.createReservationClient(resources, startTime);
		MockReservation.clearCallCounter();
		ReservationPropertiesDocument props=reservation.getResourcePropertiesDocument();
		//check that backend is invoked only once
		assertEquals(1,MockReservation.count);
		
		String resID=props.getReservationProperties().getReservationReference();
		String resStatus=String.valueOf(props.getReservationProperties().getReservationStatus());
		String resDesc=props.getReservationProperties().getReservationStatusDescription();
		Date tt=props.getReservationProperties().getTerminationTime().getDateValue();
		System.out.println("+++ Reservation ID="+resID);
		System.out.println("+++ Reservation Status="+resStatus);
		System.out.println("+++ Reservation Description="+resDesc);
		System.out.println("+++ Reservation TerminationTime="+tt);
		assertEquals("OK",resDesc);

		//must be starttime + two days (configured in wsrflite.xml)
		Calendar expectedTT=(Calendar)startTime.clone();
		expectedTT.add(Calendar.SECOND, 172800);
		assertEquals(expectedTT.getTimeInMillis(), tt.getTime());
		

		//check that termination time is larger than the start time
		assertTrue(0>startTime.compareTo(props.getReservationProperties().getTerminationTime().getCalendarValue()));
		
		//check we have a reference in the TSS properties
		assertEquals(1, tss.getReservations().size());

		//check we have a mock reservation
		assertTrue(MockReservation.hasReservation(resID));

		//now submit a job using the reservation
		JobDefinitionDocument jdd=getJob();
		SubmitDocument req=SubmitDocument.Factory.newInstance();
		req.addNewSubmit().setJobDefinition(jdd.getJobDefinition());
		JobClient job=reservation.submit(req);
		job.waitUntilReady(180*1000);
		job.start();
		job.waitUntilDone(180*1000);

		//check we have a reference in the new TSS's properties
		assertEquals(1, tss.getReservations().size());

		//check re-creation of reservation list after TSS destroy and re-creation
		tss.destroy();
		tss=tsf.createTSS();
		Thread.sleep(3000);
		//check we have a reference in the new TSS's properties
		assertEquals(1, tss.getReservations().size());

		//now delete the reservation
		reservation.destroy();

		//and check the reservation list on the TSS is updated
		tss.setUpdateInterval(0);
		assertEquals(0, tss.getReservations().size());
		
		//check that mock reservation was canceled
		assertFalse(MockReservation.hasReservation(resID));

		//create another one
		reservation=tss.createReservationClient(resources, startTime);
		props=reservation.getResourcePropertiesDocument();
		resID=props.getReservationProperties().getReservationReference();
		
		reservation.setTerminationTime(Calendar.getInstance());
		((DefaultHome)kernel.getHome(UAS.RESERVATIONS)).runExpiryCheckNow();
		//check it was cancelled
		assertFalse(MockReservation.hasReservation(resID));
		
	}

}
