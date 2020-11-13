package de.fzj.unicore.uas.rest;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import de.fzj.unicore.persist.PersistenceException;
import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.impl.tss.TSSModel;
import de.fzj.unicore.uas.impl.tss.TargetSystemFactoryImpl;
import de.fzj.unicore.uas.impl.tss.TargetSystemImpl;
import de.fzj.unicore.uas.json.Builder;
import de.fzj.unicore.wsrflite.Home;
import de.fzj.unicore.wsrflite.security.util.AuthZAttributeStore;
import de.fzj.unicore.xnjs.ems.BudgetInfo;
import de.fzj.unicore.xnjs.idb.ApplicationInfo;
import de.fzj.unicore.xnjs.idb.IDB;
import eu.unicore.security.AuthorisationException;
import eu.unicore.security.Client;
import eu.unicore.services.rest.Link;
import eu.unicore.services.rest.PagingHelper;
import eu.unicore.services.rest.RESTUtils;
import eu.unicore.services.rest.USEResource;
import eu.unicore.services.rest.impl.ServicesBase;
import eu.unicore.util.ConcurrentAccess;
import eu.unicore.util.Log;

/**
 * REST interface to sites (TSS instances)
 *
 * @author schuller
 */
@Path("/sites")
@USEResource(home=UAS.TSS)
public class Sites extends ServicesBase {
	
	private static final Logger logger = Log.getLogger("unicore.rest", Sites.class);
	
	protected String getResourcesName(){
		return "sites";
	}
	
	@Override
	protected Map<String,Object>getProperties() throws Exception {
		TargetSystemImpl tss = (TargetSystemImpl)resource;
		Map<String,Object> props = super.getProperties();
		TSSModel model = getModel();
		
		props.put("umask", model.getUmask());
		props.put("supportsReservation", String.valueOf(model.getSupportsReservation()));
		props.put("storages", RESTUtils.makeHrefs(kernel, "core/storages", model.getStorageIDs()));
		props.put("numberOfJobs", model.getJobIDs().size());
		
		IDB idb = tss.getXNJSFacade().getIDB();
		Map<String,Object> resources = IDBContentRendering.asMap(idb.getPartitions());
		props.put("resources", resources);
		
		Client client = AuthZAttributeStore.getClient();
		List<String> apps = new ArrayList<String>();
		for(ApplicationInfo app: tss.getXNJSFacade().getDefinedApplications(client)){
			apps.add(app.getName()+IDBContentRendering.appSeparator+app.getVersion());
		}
		props.put("applications", apps);
		
		Map<String,String> textInfo = new HashMap<String, String>();
		textInfo.putAll(idb.getTextInfoProperties());
		props.put("otherInfo", textInfo);
		
		List<BudgetInfo> budget = tss.getXNJSFacade().getComputeTimeBudget(client);
		props.put("remainingComputeTime", IDBContentRendering.budgetToMap(budget));
		
		return props;
	}

	/**
	 * submit a job to this target system instance
	 * 
	 * @param json - JSON job
	 * @return address of new resource
	 * 
	 * @throws JSONException
	 * @throws PersistenceException
	 */
	@POST
	@Path("/{uniqueID}")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response submit(String json, @PathParam("uniqueID")String id) throws Exception {
		try{
			Builder job = new Builder(json);
			TargetSystemImpl tss = (TargetSystemImpl)resource;
			Jobs.checkSubmissionEnabled(kernel);
			String location = Jobs.doSubmit(job, tss, kernel, getBaseURL());
			return Response.created(new URI(location)).build();
		}catch(Exception ex){
			return handleError("Could not submit job", ex, logger);
		}
	}
	
	@Override
	public TSSModel getModel(){
		return (TSSModel)model;
	}
	
	/**
	 * create a new TSS via the target system factory service
	 * 
	 * @param jsonString - settings for the new TSS
	 */
	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response createTSS(String jsonString) throws Exception {
		try{
			String id = null;
			TargetSystemFactoryImpl tsf = findTSF();
			try{
				id = SiteFactories.createTSS(tsf,jsonString);
			}
			finally{
				kernel.getHome(UAS.TSF).persist(tsf);
			}
			String location = getBaseURL()+"/sites/"+id;
			return Response.created(new URI(location)).build();
		}catch(Exception ex){
			return handleError("Could not create TSS",ex,logger);
		}
	}
	
	// returns the first available TSF instance (usually there will be just one!)
	synchronized TargetSystemFactoryImpl findTSF() throws PersistenceException {
		Home home = kernel.getHome(UAS.TSF);
		if(home == null)
			throw new IllegalStateException("TargetSystemFactory service is not available at this site!");	
		
		Client client = AuthZAttributeStore.getClient();
		List<String> tsfs = home.getAccessibleResources(client);
		if(tsfs == null|| tsfs.size() == 0){
			throw new AuthorisationException("There are no accessible targetsystem factories for: " +client+
					" Please check your security setup!");
		}
		String tsf = tsfs.get(0);
		return (TargetSystemFactoryImpl)home.getForUpdate(tsf);
	}
	
	@Override
	protected void updateLinks() {
		super.updateLinks();
		links.add(new Link("jobs",getBaseURL()+"/"+getResourcesName()+"/"+resource.getUniqueID()+"/jobs"));
		links.add(new Link("applications",getBaseURL()+"/"+getResourcesName()+"/"+resource.getUniqueID()+"/applications"));
		String tsfID = getModel().getParentUID();
		if(tsfID!=null){
			links.add(new Link("parent",getBaseURL()+"/"+getResourcesName()+"/factories/"+tsfID));
		}
	}
	
	/**
	 * list jobs (JSON)
	 * 
	 * @throws JSONException
	 * @throws PersistenceException
	 */
	@GET
	@Path("/{uniqueID}/jobs")
	@Produces(MediaType.APPLICATION_JSON)
	@ConcurrentAccess(allow=true)
	public Response listJobs(@QueryParam("offset") int offset, 
			@QueryParam("num") @DefaultValue(value="100") int num,
			@QueryParam("tags") String tags
			) throws JSONException, PersistenceException {
		try{
			PagingHelper ph = new PagingHelper(getBaseURL()+"/"+getResourcesName()+"/"+model.getUniqueID()+"/jobs", 
					getBaseURL()+"/jobs","jobs");
			List<String> thisSite = getModel().getJobIDs();
			List<String> candidates = thisSite;
			if(tags!=null){
				candidates.retainAll(getTaggedJobs(tags));
			}
			JSONObject list = ph.renderJson(offset, num, candidates);
			return Response.ok(list.toString(), MediaType.APPLICATION_JSON).build();
		}catch(Exception ex){
			return handleError("Could not list jobs", ex, logger);
		}
	}
	
	protected List<String>getTaggedJobs(String tagSpec) throws PersistenceException { 
		Home jmsHome = kernel.getHome(UAS.JMS);
		Client c=AuthZAttributeStore.getClient(); 
		String[] tags = tagSpec.split("[ +,]");
		Collection<String> tagged = jmsHome.getStore().getTaggedResources(tags);
		return jmsHome.getAccessibleResources(tagged, c);
	}
	
	/**
	 * list jobs (HTML)
	 * 
	 * @throws JSONException
	 * @throws PersistenceException
	 */
	@GET
	@Path("/{uniqueID}/jobs")
	@Produces(MediaType.TEXT_HTML)
	@ConcurrentAccess(allow=true)
	public String listJobsHTML(@QueryParam("offset") int offset, 
			@QueryParam("num") @DefaultValue(value="100") int num) throws PersistenceException {
		PagingHelper ph = new PagingHelper(getBaseURL()+"/"+getResourcesName()+"/"+model.getUniqueID(), 
				getBaseURL(),"jobs");
		return ph.renderHTML(offset, num, getModel().getJobIDs());
	}
	

	@Path("/{uniqueID}/applications")
	public Applications getApplicationResource() {
		String appURL = getBaseURL()+"/"+getResourcesName()+"/"+resourceID+"/applications";
		return new Applications(kernel, getModel().getXnjsReference(), appURL);
	}

}
