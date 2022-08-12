package de.fzj.unicore.uas.impl.reservation;

import java.util.Calendar;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.uas.impl.BaseResourceImpl;
import de.fzj.unicore.uas.util.LogUtil;
import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.tsi.IReservation;
import de.fzj.unicore.xnjs.tsi.ReservationStatus;
import eu.unicore.services.InitParameters;
import eu.unicore.services.exceptions.ResourceUnknownException;
import eu.unicore.services.messaging.ResourceDeletedMessage;

/**
 * WS resource representing a resource reservation
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
		
		IReservation reservation=getXNJSFacade().getReservation();
		if(reservation==null)throw new Exception("Reservation not supported.");
		
		m.reservationReference=reservation.makeReservation(m.resources, startTime, getClient());
	
		//original Xlogin
		m.xlogin=getClient()!=null?getClient().getXlogin():null;

		String tssID=initParams.tssReference;
		m.setParentUID(tssID);
		
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
	transient ReservationStatus reservationStatus;

	public synchronized ReservationStatus getReservationStatus()throws ExecutionException{
		if(reservationStatus==null || System.currentTimeMillis()>lastUpdate+updateInterval){
			reservationStatus=getXNJSFacade().getReservation().queryReservation(getModel().reservationReference, getClient());
			lastUpdate=System.currentTimeMillis();
		}
		return reservationStatus;
	}

	public void cancel()throws Exception{
		try{
			getXNJSFacade().getReservation().cancelReservation(getModel().reservationReference, getClient());
		}
		catch(Exception e){
			LogUtil.logException("Could not cancel resource reservation.",e,logger);
		}
	}

	private Action xnjsAction;

	/**
	 * Get the underlying XNJS action
	 */
	public synchronized Action getXNJSAction(){
		if(xnjsAction == null){
			xnjsAction = getXNJSFacade().getAction(getUniqueID());
			if(xnjsAction==null){
				throw new ResourceUnknownException();
			}
		}
		return xnjsAction;
	}
}
