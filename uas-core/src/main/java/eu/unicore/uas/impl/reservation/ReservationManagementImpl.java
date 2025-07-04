package eu.unicore.uas.impl.reservation;

import java.util.Calendar;

import org.apache.logging.log4j.Logger;

import eu.unicore.services.InitParameters;
import eu.unicore.services.exceptions.ResourceUnknownException;
import eu.unicore.services.messaging.ResourceDeletedMessage;
import eu.unicore.uas.impl.BaseResourceImpl;
import eu.unicore.uas.util.LogUtil;
import eu.unicore.xnjs.ems.Action;
import eu.unicore.xnjs.ems.ExecutionException;
import eu.unicore.xnjs.tsi.IReservation;
import eu.unicore.xnjs.tsi.ReservationStatus;

/**
 * Represents a resource reservation
 * 
 * @author schuller
 */
public class ReservationManagementImpl extends BaseResourceImpl {

	private static final Logger logger = LogUtil.getLogger(LogUtil.JOBS,ReservationManagementImpl.class);

	public ReservationManagementImpl(){
		super();
	}
	
	@Override
	public ReservationModel getModel(){
		return (ReservationModel)model;
	}
	
	@Override
	public void initialise(InitParameters init) throws Exception {
		if(model==null){
			model = new ReservationModel();
		}
		ReservationModel m = getModel();
		ReservationInitParameters initParams = (ReservationInitParameters)init;
		Calendar startTime = initParams.starttime;
		m.resources = initParams.resources;
		//set reservation lifetime to startTime+defaultLifetime
		Calendar lifetime = Calendar.getInstance();
		lifetime.setTime(startTime.getTime());
		lifetime.add(Calendar.SECOND, getDefaultLifetime());
		super.initialise(new InitParameters(null, lifetime));
		IReservation reservation = getXNJSFacade().getReservation();
		if(reservation==null)throw new Exception("Reservation not supported.");	
		m.reservationReference=reservation.makeReservation(m.resources, startTime, getClient());
		m.setParentUID(initParams.tssReference);
	}

	/**
	 * on destroy(), cancel the booking on the backend.
	 * Also, send a message to parent TSS
	 */
	@Override
	public void destroy() {
		try{
			ResourceDeletedMessage m=new ResourceDeletedMessage("deleted:"+getUniqueID());
			m.setServiceName(getServiceName());
			m.setDeletedResource(getUniqueID());
			kernel.getMessaging().getChannel(getModel().getParentUID()).publish(m);
		}
		catch(Exception e){
			LogUtil.logException("Could not send internal message.",e,logger);
		}
		super.destroy();
	}

	long lastUpdate=0;
	final long updateInterval=3000;
	ReservationStatus reservationStatus;

	public synchronized ReservationStatus getReservationStatus()throws ExecutionException{
		if(reservationStatus==null || System.currentTimeMillis()>lastUpdate+updateInterval){
			reservationStatus=getXNJSFacade().getReservation().queryReservation(getModel().reservationReference, getClient());
			lastUpdate=System.currentTimeMillis();
		}
		return reservationStatus;
	}

	public void cancel()throws Exception{
		getXNJSFacade().getReservation().cancelReservation(getModel().reservationReference, getClient());
	}

	private Action xnjsAction;

	/**
	 * Get the underlying XNJS action
	 */
	public synchronized Action getXNJSAction() throws Exception {
		if(xnjsAction == null){
			xnjsAction = getXNJSFacade().getAction(getUniqueID());
			if(xnjsAction==null){
				throw new ResourceUnknownException();
			}
		}
		return xnjsAction;
	}
}
