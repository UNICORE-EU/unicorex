package de.fzj.unicore.xnjs.resources;

import de.fzj.unicore.xnjs.jsdl.JSDLResourceSet;

public class ReservationResource extends BaseResource{

	private static final long serialVersionUID=1l;
	
	private String reservationID;
	
	public ReservationResource(String reservationID){
		super(JSDLResourceSet.RESERVATION_ID,Category.RESERVATION);
		this.reservationID=reservationID;
	}
	
	public Resource copy() {
		return new ReservationResource(reservationID);
	}

	public Object getValue() {
		return reservationID;
	}

	public void setStringValue(String v){
		this.reservationID=v;
	}
	
	public boolean isInRange(Object otherValue) {
		if(reservationID!=null){
			return reservationID.equals(otherValue);
		}
		else return otherValue==null;
	}
	
	public String toString(){
		return getName()+"[string] "+reservationID;
	}
}
