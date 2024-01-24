package de.fzj.unicore.uas.rest;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.impl.tss.TSSModel;
import de.fzj.unicore.uas.impl.tss.TargetSystemFactoryImpl;
import de.fzj.unicore.uas.impl.tss.TargetSystemImpl;
import de.fzj.unicore.uas.json.Builder;
import eu.unicore.persist.PersistenceException;
import eu.unicore.security.Client;
import eu.unicore.services.Home;
import eu.unicore.services.Kernel;
import eu.unicore.services.rest.Link;
import eu.unicore.services.rest.PagingHelper;
import eu.unicore.services.rest.RESTUtils;
import eu.unicore.services.rest.USEResource;
import eu.unicore.services.rest.impl.ServicesBase;
import eu.unicore.services.security.util.AuthZAttributeStore;
import eu.unicore.util.ConcurrentAccess;
import eu.unicore.util.Log;
import eu.unicore.xnjs.ems.BudgetInfo;
import eu.unicore.xnjs.idb.ApplicationInfo;
import eu.unicore.xnjs.idb.IDB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

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
		List<String> apps = new ArrayList<>();
		for(ApplicationInfo app: tss.getXNJSFacade().getDefinedApplications(client)){
			apps.add(app.getName()+IDBContentRendering.appSeparator+app.getVersion());
		}
		props.put("applications", apps);
		
		Map<String,String> textInfo = new HashMap<>();
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
	@ConcurrentAccess(allow=true)
	public Response submit(String json, @PathParam("uniqueID")String id) throws Exception {
		try{
			Builder job = new Builder(json);
			TargetSystemImpl tss = (TargetSystemImpl)resource;
			Jobs.checkSubmissionEnabled(kernel);
			String jobID = Jobs.doSubmit(job, tss, kernel);
			return Response.created(new URI(getBaseURL()+"/jobs/"+jobID)).build();
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
			Home tsfHome = kernel.getHome(UAS.TSF);
			String tsfID = SiteFactories.findTSF(kernel);
			try(TargetSystemFactoryImpl tsf = (TargetSystemFactoryImpl)tsfHome.getForUpdate(tsfID)){
				String id = SiteFactories.createTSS(tsf,jsonString);
				return Response.created(new URI(getBaseURL()+"/sites/"+id)).build();
			}
		}catch(Exception ex){
			return handleError("Could not create TSS",ex,logger);
		}
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
			@QueryParam("tags") String tags){
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
	
	protected List<String>getTaggedJobs(String tagSpec) throws Exception { 
		Home jmsHome = kernel.getHome(UAS.JMS);
		Client c=AuthZAttributeStore.getClient(); 
		String[] tags = tagSpec.split("[ +,]");
		List<String> tagged = jmsHome.getStore().getTaggedResources(tags);
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
	public Response listJobsHTML(@QueryParam("offset") int offset, 
			@QueryParam("num") @DefaultValue(value="100") int num){
		try {
			PagingHelper ph = new PagingHelper(getBaseURL()+"/"+getResourcesName()+"/"+model.getUniqueID(), 
					getBaseURL(),"jobs");
			return Response.ok(ph.renderHTML(offset, num, getModel().getJobIDs()), MediaType.TEXT_HTML).build();
		}catch(Exception ex) {
			return handleError("Could not list jobs", ex, logger);
		}
	}
	

	@Path("/{uniqueID}/applications")
	public Applications getApplicationResource() {
		String appURL = getBaseURL()+"/"+getResourcesName()+"/"+resourceID+"/applications";
		return new Applications(kernel, getModel().getXnjsReference(), appURL);
	}

	public static synchronized TargetSystemImpl findTSS(Kernel kernel) throws Exception {
		Home home = kernel.getHome(UAS.TSS);
		Client client = AuthZAttributeStore.getClient();
		if(home.getAccessibleResources(client).size()==0){
			String tsfID = SiteFactories.findTSF(kernel);
			Home tsfHome = kernel.getHome(UAS.TSF);
			try(TargetSystemFactoryImpl tsf = (TargetSystemFactoryImpl)tsfHome.getForUpdate(tsfID)){
				tsf.createTargetSystem();
			}
		}
		String tss = home.getAccessibleResources(client).get(0);
		return (TargetSystemImpl)home.get(tss);
	}

	@Override
	public boolean usesKernelMessaging() {
		return true;
	}
}
