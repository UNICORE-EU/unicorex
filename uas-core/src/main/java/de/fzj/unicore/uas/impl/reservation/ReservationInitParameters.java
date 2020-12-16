package de.fzj.unicore.uas.impl.reservation;

import java.util.Calendar;
import java.util.Map;

import de.fzj.unicore.uas.impl.BaseInitParameters;

public class ReservationInitParameters extends BaseInitParameters {

	public Map<String,String> resources;
	
	public Calendar starttime;
	
	public String tssReference;

	public ReservationInitParameters() {
		super(null,TerminationMode.DEFAULT);
	}

}
