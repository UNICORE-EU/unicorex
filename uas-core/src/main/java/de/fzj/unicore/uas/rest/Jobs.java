package de.fzj.unicore.uas.rest;

import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import de.fzj.unicore.persist.PersistenceException;
import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.impl.job.JobManagementImpl;
import de.fzj.unicore.uas.impl.job.JobModel;
import de.fzj.unicore.uas.impl.tss.TargetSystemFactoryImpl;
import de.fzj.unicore.uas.impl.tss.TargetSystemHomeImpl;
import de.fzj.unicore.uas.impl.tss.TargetSystemImpl;
import de.fzj.unicore.uas.json.Builder;
import de.fzj.unicore.uas.xnjs.XNJSFacade;
import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.ActionResult;
import de.fzj.unicore.xnjs.ems.ActionStatus;
import de.fzj.unicore.xnjs.ems.processors.JobProcessor;
import de.fzj.unicore.xnjs.tsi.IExecution;
import eu.unicore.security.AuthorisationException;
import eu.unicore.security.Client;
import eu.unicore.services.Home;
import eu.unicore.services.Kernel;
import eu.unicore.services.rest.Link;
import eu.unicore.services.rest.USEResource;
import eu.unicore.services.rest.impl.ServicesBase;
import eu.unicore.services.security.util.AuthZAttributeStore;
import eu.unicore.util.ConcurrentAccess;
import eu.unicore.util.Log;

/**
 * REST interface to jobs
 *
 * @author schuller
 */
@Path("/jobs")
@USEResource(home=UAS.JMS)
public class Jobs extends ServicesBase {

	private static final Logger logger = Log.getLogger("unicore.rest", Jobs.class);
	
	protected String getResourcesName(){
		return "jobs";
	}

	
	/**
	 * submit a job to any of our accessible target system instances
	 * 
	 * @param json - JSON job
	 * @return address of new resource
	 * 
	 * @throws JSONException
	 * @throws PersistenceException
	 */
	@GET
	@Path("/{uniqueID}/details")
	@Produces(MediaType.APPLICATION_JSON)
	public Response details() throws Exception {
		try{
			JobManagementImpl resource = getResource();
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
	 * submit a job to any of our accessible target system instances
	 * 
	 * @param json - JSON job
	 * @return address of new resource
	 * 
	 * @throws JSONException
	 * @throws PersistenceException
	 */
	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@ConcurrentAccess(allow=true)
	public Response submit(String json) throws Exception {
		try{
			checkSubmissionEnabled(kernel);
			Builder job = new Builder(json);
			TargetSystemImpl tss = findTSS(job);
			String location = doSubmit(job, tss, kernel, baseURL);
			return Response.created(new URI(location)).build();
		}catch(Exception ex){
			int status = 500;
			if (ex.getClass().isAssignableFrom(AuthorisationException.class)) {
				status = 401;
			}
			return handleError(status, "Could not submit job", ex, logger);
		}
	}

	@Override
	protected void doHandleAction(String action, JSONObject param) throws Exception {
		JobManagementImpl job = getResource();
		if("start".equals(action)){
			job.start();
		}
		else if("abort".equals(action)){
			job.abort();
		}
		else if("restart".equals(action)){
			job.restart();
		}
	}

	@Override
	protected Map<String,Object>getProperties() throws Exception {
		Map<String,Object> props = super.getProperties();
		JobModel model = getModel();
		JobManagementImpl resource = getResource();
		props.put("submissionTime", getISODateFormatter().format(model.getSubmissionTime().getTime()));
		Action action = resource.getXNJSAction();
		String queue = action.getExecutionContext().getBatchQueue();
		props.put("queue", queue!=null ? queue : "N/A");
		String bssID = action.getBSID();
		props.put("batchSystemID", bssID!=null ? bssID : "N/A");
		props.put("consumedTime", JobProcessor.timeProfile(action.getProcessingContext()).asMap());
		props.put("log", action.getLog());
		String name = action.getJobName();
		props.put("name", name!=null ? name : "N/A");
		props.put("submissionPreferences", model.getStoredPreferences());
		renderStatus(props, resource, action);
		return props;
	}

	protected void renderStatus(Map<String,Object> o, JobManagementImpl resource, Action a) throws Exception{
		String xnjsReference=resource.getXNJSReference();
		String actionID=resource.getUniqueID();
		Client client=AuthZAttributeStore.getClient();
		XNJSFacade xnjs=XNJSFacade.get(xnjsReference,kernel);
		ActionResult result=a.getResult();
		o.put("status", convertStatus(a.getStatus(),result.isSuccessful()));
		o.put("statusMessage", "");
		Integer exitCode=xnjs.getExitCode(actionID,client);
		if(exitCode!=null){
			o.put("exitCode", String.valueOf(exitCode.longValue()));
		}
		Float progress=xnjs.getProgress(actionID,client);
		if(progress!=null){
			o.put("progress", String.valueOf(progress));
		}
		if(!result.isSuccessful()){
			String errorMessage=result.getErrorMessage();
			if(errorMessage==null)errorMessage="";
			o.put("statusMessage", errorMessage);
		}
	}

	@Override
	public JobModel getModel(){
		return (JobModel)model;
	}

	@Override
	public JobManagementImpl getResource(){
		return (JobManagementImpl)resource;
	}

	@Override
	protected void updateLinks() {
		super.updateLinks();
		JobModel model = getModel();

		links.add(new Link("workingDirectory", baseURL+"/storages/"+model.getUspaceId(),"Working directory"));
		links.add(new Link("parentTSS", baseURL+"/sites/"+model.getParentUID(), "Parent TSS"));
		links.add(new Link("details", baseURL+"/jobs/"+resource.getUniqueID()+"/details", "BSS job details"));

		// TODO these should be state-dependent
		links.add(new Link("action:start", baseURL+"/jobs/"+resource.getUniqueID()+"/actions/start","Start"));
		links.add(new Link("action:abort", baseURL+"/jobs/"+resource.getUniqueID()+"/actions/abort","Abort"));
		links.add(new Link("action:restart", baseURL+"/jobs/"+resource.getUniqueID()+"/actions/restart","Restart"));

	}

	synchronized TargetSystemImpl findTSS(Builder job) throws Exception {
		Home home = kernel.getHome(UAS.TSS);
		Client client = AuthZAttributeStore.getClient();
		if(home.getAccessibleResources(client).size()==0){
			TargetSystemFactoryImpl tsf = findTSF();
			try{
				tsf.createTargetSystem();
			}
			finally{
				kernel.getHome(UAS.TSF).persist(tsf);
			}
		}
		String tss = home.getAccessibleResources(client).get(0);
		return (TargetSystemImpl)home.get(tss);
	}

	synchronized TargetSystemFactoryImpl findTSF() throws PersistenceException {
		Home home = kernel.getHome(UAS.TSF);
		Client client = AuthZAttributeStore.getClient();
		List<String> tsfs = home.getAccessibleResources(client);
		if(tsfs == null|| tsfs.size() == 0){
			throw new AuthorisationException("There are no accessible targetsystem factories for: " +client+
					" Please check your security setup!");
		}
		String tsf = tsfs.get(0);
		return (TargetSystemFactoryImpl)home.getForUpdate(tsf);
	}

	/**
	 * submission code used from both Jobs and Sites resources
	 */
	public static String doSubmit(Builder job, TargetSystemImpl tss, Kernel kernel, String baseURL) 
	throws Exception {
		boolean autoRun = !Boolean.parseBoolean(job.getProperty("haveClientStageIn"));
		String id = tss.submit(job.getJSON(), autoRun, null, job.getTags());
		// store new job ID in model - need a write lock on the tss
		TargetSystemImpl tss2 = null;
		try {
			tss2 = (TargetSystemImpl)kernel.getHome(UAS.TSS).getForUpdate(tss.getUniqueID());
			tss2.registerJob(id);
		}
		finally{
			if(tss2!=null)kernel.getHome(UAS.TSS).persist(tss2);
		}
		String location = baseURL+"/jobs/"+id;
		return location;
	}

	public static void checkSubmissionEnabled(Kernel kernel) throws WebApplicationException {		
		TargetSystemHomeImpl tssHome = (TargetSystemHomeImpl)kernel.getHome(UAS.TSS);
		if(!tssHome.isJobSubmissionEnabled()){
			throw new WebApplicationException(createErrorResponse(503,tssHome.getHighMessage()));
		}
	}

	/**
	 * converts from the XNJS action status to state from unigridsTypes.xsd
	 * 
	 * <xsd:simpleType name="StatusType">
	 <xsd:restriction base="xsd:string">
	 <xsd:enumeration value="UNDEFINED"/>
	 <xsd:enumeration value="READY"/>
	 <xsd:enumeration value="QUEUED"/>
	 <xsd:enumeration value="RUNNING"/>
	 <xsd:enumeration value="SUCCESSFUL"/>
	 <xsd:enumeration value="FAILED"/>
	 <xsd:enumeration value="STAGINGIN"/>
	 <xsd:enumeration value="STAGINGOUT"/>
	 </xsd:restriction>
	 </xsd:simpleType>
	 *
	 * @param emsStatus
	 * @return UNICORE status
	 */
	public static String convertStatus(Integer emsStatus, boolean successful){
		int i=emsStatus.intValue(); 
		switch (i){
			case ActionStatus.PREPROCESSING: 
				return "STAGINGIN";
			case ActionStatus.POSTPROCESSING: 
				return "STAGINGOUT";
			case ActionStatus.RUNNING: 
				return "RUNNING";
			case ActionStatus.PENDING:
				return "QUEUED";
			case ActionStatus.QUEUED:
				return "QUEUED";
			case ActionStatus.READY: 
			case ActionStatus.CREATED:
				return "READY";
			case ActionStatus.DONE:
				if(successful){
					return "SUCCESSFUL";
				}
				else{
					return "FAILED";
				}
			default:
				return "UNDEFINED";
		}
	}
	
}
