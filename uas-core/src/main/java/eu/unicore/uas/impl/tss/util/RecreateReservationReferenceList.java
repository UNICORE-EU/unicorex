package eu.unicore.uas.impl.tss.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.Logger;

import eu.emi.security.authn.x509.impl.X500NameUtils;
import eu.unicore.persist.PersistenceException;
import eu.unicore.security.Client;
import eu.unicore.services.Home;
import eu.unicore.services.Kernel;
import eu.unicore.services.exceptions.ResourceUnknownException;
import eu.unicore.uas.UAS;
import eu.unicore.uas.impl.reservation.ReservationManagementImpl;
import eu.unicore.uas.impl.tss.TargetSystemImpl;
import eu.unicore.uas.util.LogUtil;

/**
 * Re-creates the list of accessible reservation references in a TSS as it is created.
 * This only happens if it is the only TSS for the user.
 * <p>
 * We must be careful here: as this is invoked from the thread pool, the client 
 * identity must be set upon creation (that's why we don't use the actual thread-local
 * user).
 * 
 * @author schuller
 */
public class RecreateReservationReferenceList implements Runnable{

	private final Logger logger = LogUtil.getLogger(LogUtil.JOBS, RecreateReservationReferenceList.class);

	private final String tssID;

	private final Client client;

	private final Home tssHome;

	private final Home reservations;

	public RecreateReservationReferenceList(Kernel kernel, String tssID, Client client)throws PersistenceException{
		this.tssID=tssID;
		this.client=client;
		this.tssHome=kernel.getHome(UAS.TSS);
		this.reservations=kernel.getHome(UAS.RESERVATIONS);
	}

	@Override
	public void run(){
		try{
			if(reservations==null){
				//nothing to do
				return;
			}
			String user = client.getDistinguishedName();
			//check if owner has more TSSs
			Collection<String>tssIds=tssHome.getStore().getUniqueIDs();
			tssIds.remove(tssID);
			for(String id: tssIds){
				TargetSystemImpl t=(TargetSystemImpl)tssHome.get(id);
				if (X500NameUtils.equal(t.getOwner(), user)){
					//nothing to do
					return;
				}
			}
			logger.debug("Re-generating reservation reference list for {}", user);
			List<String>oldReservations = new ArrayList<>();
			for(String reservationID: getExistingReservations()){
				try{
					ReservationManagementImpl reservation=(ReservationManagementImpl)reservations.get(reservationID);
					if(reservation.getOwner() ==null || X500NameUtils.equal(reservation.getOwner(), user)){
						oldReservations.add(reservationID);
						try(ReservationManagementImpl r2 =(ReservationManagementImpl)reservations.getForUpdate(reservationID)){
							r2.getModel().setParentUID(tssID);
						}
					}
				}catch(ResourceUnknownException re){
					logger.debug("Reservation <{}> not found any more.", reservationID);
				}
			}
			try(TargetSystemImpl tss=(TargetSystemImpl)tssHome.getForUpdate(tssID)){
				tss.getModel().getReservationIDs().addAll(oldReservations);
				logger.debug("Added <{}> existing reservations to target system", oldReservations.size());
			}
		}catch(Exception ex){
			logger.error("Could not restore reservations for {}", client.getDistinguishedName(),ex);
		}
	}

	private Collection<String>getExistingReservations()throws Exception{
		return reservations.getStore().getUniqueIDs();
	}

}