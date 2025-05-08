package eu.unicore.xnjs.tsi;

import java.util.Calendar;
import java.util.Map;

import eu.unicore.security.Client;
import eu.unicore.xnjs.ems.ExecutionException;

/**
 * Resource reservation interface
 * 
 * @author schuller
 */
public interface IReservation {

	/**
	 * Reserve resources starting at a given time
	 * 
	 * @param resources - resources to be reserved
	 * @param startTime - the starting time
	 * @param client - the {@link Client} making the reservation
	 * @return a reservation ID 
	 * @throws ExecutionException
	 */
	public String makeReservation(Map<String,String> resources, Calendar startTime, Client client) throws ExecutionException;

	/**
	 * Cancel a reservation
	 *
	 * @param reservationID the id of the reservation to cancel 
	 * @param client the {@link Client} cancelling the reservation
	 */
	public void cancelReservation(String reservationID, Client client)throws ExecutionException;

	/**
	 * Query a reservation
	 *
	 * @param reservationID the id of the reservation to query
	 * @param client the {@link Client} query the reservation
	 * @return {@link ReservationStatus}
	 */
	public ReservationStatus queryReservation(String reservationID, Client client)throws ExecutionException;

}	

