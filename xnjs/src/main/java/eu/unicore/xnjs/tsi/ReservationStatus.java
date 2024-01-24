package eu.unicore.xnjs.tsi;

import java.util.Calendar;

/**
 * Reservation status
 */
public class ReservationStatus {

    public enum Status {
    	
    	/**
    	 * the status is unknown, e.g.no status query has been 
		 * performed yet
    	 */
    	UNKNOWN, 
    	
    	/**
    	 * an error occurred in the reservation process, or
    	 * the batch system chose to cancel the reservation
    	 */
    	INVALID, 
    	
    	/**
    	 * the reservation is valid, but the start time has not 
    	 * been reached yet
    	 */
    	WAITING,
    	
    	/**
    	 * the reservation is valid, the start time has been reached
    	 * but no job has been submitted yet
    	 */
    	READY, 
    	
    	/**
    	 * a job is running in the reserved slot
    	 */
    	ACTIVE, 
    	
    	/**
    	 * the allocated time has passed
    	 */
    	FINISHED, 
    	
    	OTHER
    }
	
    private String description;
    
    private Status status;
    
	private Calendar startTime;
	
	public Calendar getStartTime() {
		return startTime;
	}

	public void setStartTime(Calendar startTime) {
		this.startTime = startTime;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	
	
	
}
