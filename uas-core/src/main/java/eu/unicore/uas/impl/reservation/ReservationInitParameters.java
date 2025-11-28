package eu.unicore.uas.impl.reservation;

import java.util.Calendar;
import java.util.Map;

import eu.unicore.uas.impl.BaseInitParameters;

public class ReservationInitParameters extends BaseInitParameters {

	public Map<String,String> resources;

	public Calendar starttime;

	public String tssReference;

	public boolean isBSSAllocation = true;

	public ReservationInitParameters() {
		super(null,TerminationMode.DEFAULT);
	}

}
