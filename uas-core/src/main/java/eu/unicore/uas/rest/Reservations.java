package eu.unicore.uas.rest;

import java.net.URI;
import java.util.Calendar;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import eu.unicore.persist.PersistenceException;
import eu.unicore.security.AuthorisationException;
import eu.unicore.services.rest.Link;
import eu.unicore.services.rest.USEResource;
import eu.unicore.services.rest.impl.ServicesBase;
import eu.unicore.services.restclient.utils.UnitParser;
import eu.unicore.uas.UAS;
import eu.unicore.uas.impl.reservation.ReservationManagementImpl;
import eu.unicore.uas.impl.reservation.ReservationModel;
import eu.unicore.uas.impl.tss.TargetSystemImpl;
import eu.unicore.uas.json.JSONUtil;
import eu.unicore.util.ConcurrentAccess;
import eu.unicore.util.Log;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
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

	@Override
	protected String getResourcesName(){
		return "reservations";
	}

	/**
	 * create a new reservation on any of our accessible target system instances
	 * 
	 * @param json - incoming JSON describing the requested reservation
	 * 
	 * @throws JSONException
	 * @throws PersistenceException
	 */
	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@ConcurrentAccess(allow=true)
	public Response create(String json) {
		try{
			TargetSystemImpl tss = Sites.findTSS(kernel);
			String id = doCreate(new JSONObject(json), tss);
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
			startTime.setTime(UnitParser.getISO8601().parse(startTimeS));
		}
		try(TargetSystemImpl tss2 = (TargetSystemImpl)tss.getHome().getForUpdate(tss.getUniqueID())){
			return tss2.createReservationResource(resources, startTime);
		}
	}

}