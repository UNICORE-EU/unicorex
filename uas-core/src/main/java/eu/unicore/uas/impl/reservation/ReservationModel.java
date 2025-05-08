package eu.unicore.uas.impl.reservation;

import java.util.Map;

import eu.unicore.uas.impl.UASBaseModel;

public class ReservationModel extends UASBaseModel {

	private static final long serialVersionUID = 1L;

	String reservationReference;

	Map<String,String> resources;
	
	public String getReservationReference() {
		return reservationReference;
	}

	public void setReservationReference(String reservationReference) {
		this.reservationReference = reservationReference;
	}

	public Map<String,String> getResources() {
		return resources;
	}

	public void setResources(Map<String,String> resources) {
		this.resources = resources;
	}

}
