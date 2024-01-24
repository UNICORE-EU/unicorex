package eu.unicore.uas.impl.reservation;

import java.util.Map;

import eu.unicore.security.Xlogin;
import eu.unicore.uas.impl.UASBaseModel;

public class ReservationModel extends UASBaseModel {

	private static final long serialVersionUID = 1L;

	String reservationReference;

	//original Xlogin that was used to create the reservation
	Xlogin xlogin;

	Map<String,String> resources;
	
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

	public Map<String,String> getResources() {
		return resources;
	}

	public void setResources(Map<String,String> resources) {
		this.resources = resources;
	}

}
