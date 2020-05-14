package de.fzj.unicore.uas.client;

import java.util.Calendar;

import org.apache.xmlbeans.XmlObject;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.ResourcesDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.ResourcesType;
import org.unigrids.x2006.x04.services.jms.TargetSystemReferenceDocument;
import org.unigrids.x2006.x04.services.reservation.ReservationPropertiesDocument;
import org.unigrids.x2006.x04.services.reservation.ReservationReferenceDocument;
import org.unigrids.x2006.x04.services.reservation.ReservationStatusType;
import org.unigrids.x2006.x04.services.reservation.StartTimeDocument;
import org.unigrids.x2006.x04.services.tss.SubmitDocument;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.wsrflite.xmlbeans.WSUtilities;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * Client for managing a resource reservation.<br/>
 * It can be used to
 * <ul> 
 * <li>query the reservation properties</li>
 * <li>claim the booking by submitting a job including the reservation reference</li>
 * <li>cancel the booking using the WSRF destroy() operation</li>
 * </ul>
 * @author schuller
 */
public class ReservationClient extends BaseUASClient {

	public ReservationClient(String url, EndpointReferenceType epr,
			IClientConfiguration sec) throws Exception {
		super(url, epr, sec);
	}

	public ReservationClient(EndpointReferenceType epr,
			IClientConfiguration sec) throws Exception {
		this(epr.getAddress().getStringValue(),epr,sec);
	}

	/**
	 * Submit a job to the TSS used in this reservation<br/>
	 * If not present, the booking reference is inserted 
	 * automatically into the resource description.
	 * 
	 * @param submitDocument
	 */
	public JobClient submit(SubmitDocument submitDocument)throws Exception{
		JobDefinitionType job=submitDocument.getSubmit().getJobDefinition();
		EndpointReferenceType tssEPR=getTSSEpr();
		TSSClient tss=new TSSClient(tssEPR.getAddress().getStringValue(),tssEPR,getSecurityConfiguration());
		//if necessary, add in the reservation reference
		ResourcesType rt=job.getJobDescription().getResources();
		if(rt==null){
			rt=job.getJobDescription().addNewResources();
		}
		if(!rt.toString().contains("ReservationReference")){
			job.getJobDescription().setResources(addReservationReference(rt));
		}
		return tss.submit(submitDocument);
	}
	
	/**
	 * add the reservation reference to the resources
	 * The reference is added as an XML element as follows:
	 * &lt;ReservationReference xmlns="http://www.unicore.eu/unicore/xnjs"&gt;reference&lt;/ReservationReference&gt;
	 * @param resources
	 * @throws Exception
	 */
	public ResourcesType addReservationReference(ResourcesType resources) throws Exception{
		String resID="<u6rr:ReservationReference xmlns:u6rr=\"http://www.unicore.eu/unicore/xnjs\">"+getReservationReference()+"</u6rr:ReservationReference>";
		XmlObject o=XmlObject.Factory.parse(resID);
		ResourcesDocument rd=ResourcesDocument.Factory.newInstance();
		rd.setResources(resources);
		WSUtilities.append(o, rd);
		return rd.getResources();
	}
	
	/**
	 * get the StartTime property
	 * @return start time for the reservation
	 * @throws Exception
	 */
	public Calendar getStartTime()throws Exception{
		return getSingleResourceProperty(StartTimeDocument.class).getStartTime();
	}
	
	/**
	 * get the reservation reference
	 * @return reservation reference String
	 * @throws Exception
	 */
	public String getReservationReference()throws Exception{
		return getSingleResourceProperty(ReservationReferenceDocument.class).getReservationReference();
	}

	/**
	 * get the resources that have been reserved
	 * @return a {@link ResourcesDocument}
	 * @throws Exception
	 */
	public ResourcesDocument getResources()throws Exception{
		return getSingleResourceProperty(ResourcesDocument.class);
	}
	
	/**
	 * get the status of the reservation
	 * @throws Exception
	 */
	public ReservationStatusType.Enum getReservationStatus()throws Exception{
		return getResourcePropertiesDocument().getReservationProperties().getReservationStatus();
	}
	
	/**
	 * get the status description of the reservation
	 * @return a String containing the human-friendly status description
	 * @throws Exception
	 */
	public String getReservationStatusDescription()throws Exception{
		return getResourcePropertiesDocument().getReservationProperties().getReservationStatusDescription();
	}
	
	/**
	 * get the EPR of the TargetSystem where the reservation has been made
	 * @return an {@link EndpointReferenceType} holding the EPR
	 * @throws Exception
	 */
	public EndpointReferenceType getTSSEpr()throws Exception{
		return getSingleResourceProperty(TargetSystemReferenceDocument.class).getTargetSystemReference();
	}

	/**
	 * get the resource properties document of the Reservation WS-Resource
	 * 
	 * @return a {@link ReservationPropertiesDocument}
	 * @throws Exception
	 */
	public ReservationPropertiesDocument getResourcePropertiesDocument() throws Exception{
		return ReservationPropertiesDocument.Factory.parse(GetResourcePropertyDocument().getGetResourcePropertyDocumentResponse().newInputStream());
	}
		
}
