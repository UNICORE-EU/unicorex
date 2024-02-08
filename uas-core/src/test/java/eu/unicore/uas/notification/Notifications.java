package eu.unicore.uas.notification;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import eu.unicore.services.rest.USEResource;
import eu.unicore.services.rest.impl.ServicesBase;
import eu.unicore.uas.UAS;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Simplistic notifications receiver
 *
 * @author schuller
 */
@Path("/")
@USEResource(home=UAS.TASK)
public class Notifications extends ServicesBase {

	public static final List<JSONObject> notifications = new ArrayList<>();
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public Response receive(String json) {
		notifications.add(new JSONObject(json));
		return Response.noContent().build();
	}
}
