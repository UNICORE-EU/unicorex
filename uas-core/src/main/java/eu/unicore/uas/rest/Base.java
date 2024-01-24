package eu.unicore.uas.rest;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import eu.unicore.services.rest.Link;
import eu.unicore.services.rest.impl.ApplicationBaseResource;
import eu.unicore.uas.UAS;
import eu.unicore.uas.impl.tss.TargetSystemHomeImpl;
import eu.unicore.util.Log;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Parent resource for the core services, has links to the actual resources 
 * and shows some useful info about the server and the current client
 * 
 * @author schuller
 */
@Path("/")
public class Base extends ApplicationBaseResource {

	private final static Logger logger = Log.getLogger(Log.SERVICES, Base.class);
	
	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	public Response listAll(@QueryParam("fields")String fields) throws Exception {
		try {
			parsePropertySpec(fields);
			return Response.ok().entity(getJSON().toString()).build();
		}catch(Exception ex) {
			return handleError("", ex, logger);
		}
	}

	@GET
	@Path("/")
	@Produces(MediaType.TEXT_HTML)
	public String listAllHtml() throws Exception {
		return getHTML();
	}

	final static String[] resources = new String[]{
			"factories","sites","jobs",
			"reservations",
			"storages","storagefactories",
			"transfers","client-server-transfers", 
			"tasks"
	};

	final static String[] serviceNames = new String[]{
			UAS.TSF, UAS.TSS, UAS.JMS,
			UAS.RESERVATIONS,
			UAS.SMS, UAS.SMF, 
			UAS.SERVER_FTS, UAS.CLIENT_FTS,
			UAS.TASK,
	};

	@Override
	protected Map<String, Object> renderServerProperties() throws Exception {
		Map<String,Object>props = super.renderServerProperties();
		try{
			TargetSystemHomeImpl tsf = (TargetSystemHomeImpl)kernel.getHome(UAS.TSS);
			boolean enabled = tsf!=null && tsf.isJobSubmissionEnabled();
			String msg = tsf==null? "No job submission at this site." 
					: tsf.getHighMessage();
			Map<String,Object> js = new HashMap<>();
			props.put("jobSubmission", js);
			js.put("enabled", enabled);
			js.put("message", msg);
		}catch(Exception ex){}
		return props;
	}

	@Override
	protected void updateLinks() {
		for(int i = 0; i<resources.length; i++){
			String r = resources[i];
			String sName = serviceNames[i];
			if(sName!=null) {
				if(kernel.getHome(sName)==null) {
					continue;
				};
			}
			links.add(new Link(r, getBaseURL()+"/"+r));
		}
	}

}
