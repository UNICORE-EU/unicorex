package de.fzj.unicore.uas;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.ParameterStyle;
import javax.jws.soap.SOAPBinding.Style;
import javax.jws.soap.SOAPBinding.Use;
import javax.xml.namespace.QName;

import org.ggf.schemas.jsdl.x2005.x11.jsdl.ResourcesDocument;
import org.unigrids.x2006.x04.services.jms.TargetSystemReferenceDocument;
import org.unigrids.x2006.x04.services.reservation.ReservationReferenceDocument;
import org.unigrids.x2006.x04.services.reservation.ReservationStatusDescriptionDocument;
import org.unigrids.x2006.x04.services.reservation.ReservationStatusDocument;
import org.unigrids.x2006.x04.services.reservation.StartTimeDocument;

import de.fzj.unicore.wsrflite.xmlbeans.ResourceLifetime;
import de.fzj.unicore.wsrflite.xmlbeans.ResourceProperties;

/**
 * Service for managing reservations
 */
@WebService(targetNamespace = "http://unigrids.org/2006/04/services/reservation",
		portName="ReservationManagement")
@SOAPBinding(parameterStyle=ParameterStyle.BARE, use=Use.LITERAL, style=Style.DOCUMENT)
public interface ReservationManagement extends ResourceProperties,ResourceLifetime{

	//Namespace
	public static final String NAMESPACE="http://unigrids.org/2006/04/services/reservation";
	
	//Porttype
	public static final QName PORT=new QName(NAMESPACE,"ReservationManagement");

	/**
	 * the resource property containing the reference to the  
	 * target system where the resources have been reserved
	 */
	public static final QName RPTargetSystemReference = TargetSystemReferenceDocument.type.getDocumentElementName();

	/**
	 * the resource property containing the start time of the
	 * reservation
	 */
	public static final QName RPStartTime = StartTimeDocument.type.getDocumentElementName();

	/**
	 * the resource property containing the status of the reservation
	 */
	public static final QName RPReservationStatus = ReservationStatusDocument.type.getDocumentElementName();

	/**
	 * the resource property containing the status description of the reservation
	 */
	public static final QName RPReservationStatusDescription = ReservationStatusDescriptionDocument.type.getDocumentElementName();

	/**
	 * the resource property containing the booked resources
	 */
	public static final QName RPResources = ResourcesDocument.type.getDocumentElementName();

	/**
	 * the resource property containing the reference used
	 * for claiming the reservation with the batch system
	 */
	public static final QName RPReservationReference = ReservationReferenceDocument.type.getDocumentElementName();

	
}
