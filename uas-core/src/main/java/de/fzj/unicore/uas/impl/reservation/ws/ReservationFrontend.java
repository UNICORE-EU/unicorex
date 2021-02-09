package de.fzj.unicore.uas.impl.reservation.ws;

import javax.xml.namespace.QName;

import org.unigrids.x2006.x04.services.reservation.ReservationPropertiesDocument;
import org.unigrids.x2006.x04.services.reservation.ReservationReferenceDocument;

import de.fzj.unicore.uas.ReservationManagement;
import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.impl.UASBaseFrontEnd;
import de.fzj.unicore.uas.impl.reservation.ReservationManagementImpl;
import de.fzj.unicore.wsrflite.xmlbeans.renderers.AddressRenderer;
import de.fzj.unicore.wsrflite.xmlbeans.renderers.FieldRenderer;
import de.fzj.unicore.wsrflite.xmlbeans.renderers.ValueRenderer;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.tsi.ReservationStatus;

/**
 * WS resource representing a resource reservation
 * 
 * @author schuller
 */
public class ReservationFrontend extends UASBaseFrontEnd implements ReservationManagement {

	private final ReservationManagementImpl resource;
	
	public ReservationFrontend(ReservationManagementImpl r){
		super(r);
		this.resource = r;
		addRenderer(new ValueRenderer(r, ReservationReferenceDocument.type.getDocumentElementName()) {
			@Override
			protected ReservationReferenceDocument getValue() throws Exception {
				ReservationReferenceDocument res=ReservationReferenceDocument.Factory.newInstance();
				res.setReservationReference(r.getModel().getReservationReference());
				return res;
			}
		});
		addRenderer(new StartTimeResourceProperty(r));
		addRenderer(new ReservationStatusResourceProperty(r));
		addRenderer(new ReservationStatusDescriptionResourceProperty(r));
		addRenderer(new FieldRenderer(r, RPResources, "resources"));
		addRenderer(new AddressRenderer(r, RPTargetSystemReference, false){
			@Override
			protected String getServiceSpec() {
				return UAS.TSS+"?res=" + r.getModel().getParentUID();
			}
			
		});
	}

	@Override
	public QName getResourcePropertyDocumentQName() {
		return ReservationPropertiesDocument.type.getDocumentElementName();
	}

	@Override
	public QName getPortType() {
		return ReservationManagement.PORT;
	}

	long lastUpdate=0;
	final long updateInterval=3000;
	transient ReservationStatus reservationStatus;

	public synchronized ReservationStatus getReservationStatus()throws ExecutionException{
		if(reservationStatus==null || System.currentTimeMillis()>lastUpdate+updateInterval){
			reservationStatus = resource.getXNJSFacade().getReservation().queryReservation(
					resource.getModel().getReservationReference(), resource.getClient());
			lastUpdate=System.currentTimeMillis();
		}
		return reservationStatus;
	}

}
