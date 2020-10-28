package de.fzj.unicore.xnjs.tsi.remote;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.junit.Assert;
import org.junit.Test;

import de.fzj.unicore.xnjs.tsi.ReservationStatus;
import de.fzj.unicore.xnjs.tsi.ReservationStatus.Status;

public class TestReservation {

	@Test
	public void testParseReply()throws Exception{
		String date="2012-09-26T22:00:00+0200";
		SimpleDateFormat sf=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		Calendar c=Calendar.getInstance();
		c.setTime(sf.parse(date));
		String desc="All is fine";
		String rep="TSI_OK\nWAITING "+date+"\n"+desc;
		ReservationStatus rs=new Reservation().parseTSIReply(rep);
		Assert.assertEquals(Status.WAITING, rs.getStatus());
		Assert.assertEquals(c.getTimeInMillis(), rs.getStartTime().getTimeInMillis());
		Assert.assertEquals(desc, rs.getDescription());
	}

	@Test
	public void testParseReply2()throws Exception{
		String date="2012-09-26T22:00:00+0200";
		SimpleDateFormat sf=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		Calendar c=Calendar.getInstance();
		c.setTime(sf.parse(date));
		String rep="TSI_OK\nWAITING "+date+"\n";
		ReservationStatus rs=new Reservation().parseTSIReply(rep);
		Assert.assertEquals(Status.WAITING, rs.getStatus());
		Assert.assertEquals(c.getTimeInMillis(), rs.getStartTime().getTimeInMillis());
		Assert.assertNull(rs.getDescription());
	}

}
