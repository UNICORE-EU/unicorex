package de.fzj.unicore.uas.impl.reservation;

import java.util.Calendar;

import org.ggf.schemas.jsdl.x2005.x11.jsdl.ResourcesDocument;

import de.fzj.unicore.uas.impl.BaseInitParameters;

public class ReservationInitParameters extends BaseInitParameters {

	public ResourcesDocument resources;
	
	public Calendar starttime;
	
	public String tssReference;

	public ReservationInitParameters() {
		super(null,TerminationMode.DEFAULT);
	}

}
