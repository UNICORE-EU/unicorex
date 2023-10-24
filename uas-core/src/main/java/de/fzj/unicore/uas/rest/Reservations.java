package de.fzj.unicore.uas.rest;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.impl.reservation.ReservationManagementImpl;
import de.fzj.unicore.uas.impl.reservation.ReservationModel;
import de.fzj.unicore.uas.impl.tss.TargetSystemImpl;
import de.fzj.unicore.uas.json.JSONUtil;
import de.fzj.unicore.uas.xnjs.XNJSFacade;
import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.tsi.IExecution;
import eu.unicore.persist.PersistenceException;
import eu.unicore.security.AuthorisationException;
import eu.unicore.services.rest.Link;
import eu.unicore.services.rest.USEResource;
import eu.unicore.services.rest.impl.ServicesBase;
import eu.unicore.util.ConcurrentAccess;
import eu.unicore.util.Log;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST interface to reservations
 *
 * @author schuller
 */
@Path("/reservations")
@USEResource(home=UAS.RESERVATIONS)
public class Reservations extends ServicesBase {

	private static final Logger logger = Log.getLogger("unicore.rest", Reservations.class);
	
	protected String getResourcesName(){
		return "reservations";
	}

	/**
	 * get the BSS job details for this reservation
	 */
	@GET
	@Path("/{uniqueID}/details")
	@Produces(MediaType.APPLICATION_JSON)
	public Response details() {
		try{
			ReservationManagementImpl resource = getResource();
			String xnjsReference = resource.getXNJSReference();
			Action action = resource.getXNJSAction();
			XNJSFacade xnjs = XNJSFacade.get(xnjsReference,kernel);
			String detailString = xnjs.getXNJS().get(IExecution.class).getBSSJobDetails(action);
			JSONObject details;
			try {
				details = new JSONObject(detailString);
			}catch(JSONException ex) {
				details = new JSONObject();
				details.put("rawDetailsData", detailString);
			}
			return Response.ok(details.toString()).build();
		}catch(Exception ex){
			return handleError("Could not get job details", ex, logger);
		}
	}
	
	/**
	 * create a new allocation / reservation on any of our accessible target system instances
	 * 
	 * @param json - JSON describing the requested allocation
	 * 
	 * @throws JSONException
	 * @throws PersistenceException
	 */
	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@ConcurrentAccess(allow=true)
	public Response create(String jsonS) {
		try{
			JSONObject json = new JSONObject(jsonS);
			TargetSystemImpl tss = Sites.findTSS(kernel);
			String id = doCreate(json, tss);
			return Response.created(new URI(baseURL+"/reservations/"+id)).build();
		}catch(Exception ex){
			int status = 500;
			if (ex.getClass().isAssignableFrom(AuthorisationException.class)) {
				status = 401;
			}
			return handleError(status, "Could not create reservation", ex, logger);
		}
	}

	@Override
	protected Map<String,Object>getProperties() throws Exception {
		Map<String,Object> props = super.getProperties();
		ReservationModel model = getModel();
		ReservationManagementImpl resource = getResource();
		props.put("status", resource.getReservationStatus().getStatus());
		props.put("reservationID", model.getReservationReference());
		props.put("resources", model.getResources());
		return props;
	}

	@Override
	public ReservationModel getModel(){
		return (ReservationModel)model;
	}

	@Override
	public ReservationManagementImpl getResource(){
		return (ReservationManagementImpl)resource;
	}

	@Override
	protected void updateLinks() {
		super.updateLinks();
		ReservationModel model = getModel();
		String base =  baseURL+"/reservations/"+resource.getUniqueID();
		links.add(new Link("details", base+"/details", "BSS reservation details"));
		links.add(new Link("parentTSS", baseURL+"/sites/"+model.getParentUID(), "Parent TSS"));
	}

	/**
	 * creates the resource and returns its ID 
	 */
	protected String doCreate(JSONObject spec, TargetSystemImpl tss) throws Exception {
		Map<String,String> resources = JSONUtil.asMap(spec.getJSONObject("Resources"));
		String startTimeS = spec.optString("Start time", null);
		Calendar startTime = Calendar.getInstance();
		if(startTimeS!=null) {
			Date date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(startTimeS);
			startTime.setTime(date);
		}
		try(TargetSystemImpl tss2 = (TargetSystemImpl)tss.getHome().getForUpdate(tss.getUniqueID())){
			return tss2.createReservationResource(resources, startTime);
		}
	}
	
}
