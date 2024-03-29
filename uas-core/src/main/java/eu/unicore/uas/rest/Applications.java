package eu.unicore.uas.rest;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import eu.unicore.security.Client;
import eu.unicore.services.Kernel;
import eu.unicore.services.rest.RESTUtils;
import eu.unicore.services.rest.impl.RESTRendererBase;
import eu.unicore.services.security.util.AuthZAttributeStore;
import eu.unicore.uas.xnjs.XNJSFacade;
import eu.unicore.util.ConcurrentAccess;
import eu.unicore.util.Log;
import eu.unicore.util.Pair;
import eu.unicore.xnjs.idb.ApplicationInfo;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST applications sub-resource
 *
 * @author schuller
 */
public class Applications extends RESTRendererBase {

	private static final Logger logger = Log.getLogger("unicore.rest", Applications.class);
	
	String xnjsRef;
	Kernel kernel;

	public Applications(Kernel kernel, String xnjsRef, String baseURL){
		this.xnjsRef = xnjsRef;
		this.kernel = kernel;
		this.baseURL = baseURL;
	}

	/**
	 * retrieve the list of applications in JSON format
	 */
	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	@ConcurrentAccess(allow=true)
	public Response getAppsJSON() throws Exception {
		try{
			XNJSFacade xnjs = XNJSFacade.get(xnjsRef, kernel);
			JSONObject o = new JSONObject();
			JSONArray apps = new JSONArray();
			Client client = AuthZAttributeStore.getClient();
			for(ApplicationInfo app: xnjs.getDefinedApplications(client)){
				apps.put(app.getName()+IDBContentRendering.appSeparator+app.getVersion());
			}
			o.put("applications",apps);
			return Response.ok(o.toString(),MediaType.APPLICATION_JSON).build();
		}catch(Exception ex){
			return handleError("Error getting application info", ex, logger);
		}
	}
	
	/**
	 * retrieve the list of applications in HTML format
	 */
	@GET
	@Path("/")
	@Produces(MediaType.TEXT_HTML)
	@ConcurrentAccess(allow=true)
	public String getAppsHTML() throws Exception {
		XNJSFacade xnjs = XNJSFacade.get(xnjsRef, kernel);
		RESTUtils.HtmlBuilder b = new RESTUtils.HtmlBuilder();
		Client client = AuthZAttributeStore.getClient();
		for(ApplicationInfo app: xnjs.getDefinedApplications(client)){
			String id = app.getName()+IDBContentRendering.appSeparator+app.getVersion();
			String url = getBaseURL()+"/"+id;
			b.href(url, id);
			b.br();
		}
		return b.build();
	}
	
	/**
	 * retrieve the application's representation in HTML format
	 */
	@GET
	@Path("/{appID}")
	@Produces(MediaType.TEXT_HTML)
	@ConcurrentAccess(allow=true)
	public String getAppHTML(@PathParam("appID") String appID) throws Exception {
		resourceID = appID;
		return getHTML();
	}

	/**
	 * retrieve the application's representation in JSON format
	 * @param appID - the application name/version in the form 'name'--v'version', e.g. "Date---v1.0"
	 */
	@GET
	@Path("/{appID}")
	@Produces(MediaType.APPLICATION_JSON)
	@ConcurrentAccess(allow=true)
	public Response getAppJSON(@PathParam("appID") String appID) throws Exception {
		try{
			resourceID = appID;
			JSONObject o = new JSONObject();
			renderJSONProperties(o);
			return Response.ok(o.toString(),MediaType.APPLICATION_JSON).build();
		}catch(Exception  ex){
			return handleError("Error getting application info", ex, logger);
		}
	}

	@Override
	protected Map<String, Object> getProperties() throws Exception {
		XNJSFacade xnjs = XNJSFacade.get(xnjsRef, kernel);
		Client client = AuthZAttributeStore.getClient();
		Pair<String,String>nv = getAppNameAndVersion(resourceID);
		ApplicationInfo appInfo = xnjs.getIDB().getApplication(nv.getM1(),nv.getM2(),client);
		if(appInfo == null)throw new WebApplicationException(404);
		Map<String,Object> o = new HashMap<>();
		o.put("ApplicationName", appInfo.getName());
		o.put("ApplicationVersion", appInfo.getVersion());
		o.put("ApplicationDescription", appInfo.getDescription());
		o.put("Options", IDBContentRendering.asMap(appInfo.getMetadata()));
		return o;
	}
	
	private Pair<String,String>getAppNameAndVersion(String appID){
		String[]t=appID.split(IDBContentRendering.appSeparator);
		return new Pair<>(t[0],t[1]);
	}
}
