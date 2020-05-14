package de.fzj.unicore.uas.impl.reservation;

import org.ggf.schemas.jsdl.x2005.x11.jsdl.ResourcesDocument;

import de.fzj.unicore.uas.impl.UASBaseModel;
import eu.unicore.security.Xlogin;

public class ReservationModel extends UASBaseModel {

	private static final long serialVersionUID = 1L;

	String reservationReference;

	//original Xlogin that was used to create the reservation
	Xlogin xlogin;

	ResourcesDocument resources;
	
	public String getReservationReference() {
		return reservationReference;
	}

	public void setReservationReference(String reservationReference) {
		this.reservationReference = reservationReference;
	}

	public Xlogin getXlogin() {
		return xlogin;
	}

	public void setXlogin(Xlogin xlogin) {
		this.xlogin = xlogin;
	}

	public ResourcesDocument getResources() {
		return resources;
	}

	public void setResources(ResourcesDocument resources) {
		this.resources = resources;
	}

}
