package de.fzj.unicore.uas.rest;

import java.net.URI;
import java.nio.channels.SocketChannel;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.impl.ResponseBuilderImpl;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONException;
import org.json.JSONObject;

import de.fzj.unicore.persist.PersistenceException;
import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.impl.job.JobManagementImpl;
import de.fzj.unicore.uas.impl.job.JobModel;
import de.fzj.unicore.uas.impl.tss.TargetSystemHomeImpl;
import de.fzj.unicore.uas.impl.tss.TargetSystemImpl;
import de.fzj.unicore.uas.json.Builder;
import de.fzj.unicore.uas.xnjs.XNJSFacade;
import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.ActionResult;
import de.fzj.unicore.xnjs.ems.ActionStatus;
import de.fzj.unicore.xnjs.ems.processors.JobProcessor;
import de.fzj.unicore.xnjs.tsi.IExecution;
import de.fzj.unicore.xnjs.tsi.TSI;
import de.fzj.unicore.xnjs.tsi.remote.TSIMessages;
import eu.unicore.client.Job;
import eu.unicore.security.AuthorisationException;
import eu.unicore.security.Client;
import eu.unicore.services.Home;
import eu.unicore.services.Kernel;
import eu.unicore.services.messaging.ResourceAddedMessage;
import eu.unicore.services.rest.Link;
import eu.unicore.services.rest.RestServlet;
import eu.unicore.services.rest.USEResource;
import eu.unicore.services.rest.client.ForwardingHelper;
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
	 * get the BSS job details for this job
	 */
	@GET
	@Path("/{uniqueID}/details")
	@Produces(MediaType.APPLICATION_JSON)
	public Response details() {
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
	public Response submit(String json) {
		boolean ok = true;
		Client client = AuthZAttributeStore.getClient();
		String id = null;
		try{
			checkSubmissionEnabled(kernel);
			Builder job = new Builder(json);
			TargetSystemImpl tss = Sites.findTSS(kernel);
			id = doSubmit(job, tss, kernel);
			return Response.created(new URI(baseURL+"/jobs/"+id)).build();
		}catch(Exception ex){
			int status = 500;
			if (ex.getClass().isAssignableFrom(AuthorisationException.class)) {
				status = 401;
			}
			ok = false;
			return handleError(status, "Could not submit job", ex, logger);
		}
		finally {
			if(ok)logger.info("Submitted job with id {} for client {}", id, client);
		}
	}

	/**
	 * For allocations, this allows to submit a job into the
	 * allocation. If this is not an allocation job, an error
	 * will be raised
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
	public Response submitIntoAllocation(String json, @PathParam("uniqueID")String id) throws Exception {
		try{
			Action action = getResource().getXNJSAction();
			if(!action.getApplicationInfo().isAllocateOnly()) {
				throw new Exception("This job is not an allocation.");
			}
			Builder job = new Builder(json);
			job.setJobType(Job.Type.ON_LOGIN_NODE);
			if(ActionStatus.RUNNING!=action.getStatus()) {
				throw new Exception("Allocation is not active. Status is "+
						ActionStatus.toString(action.getStatus()));
			}
			String bssID =  action.getBSID();
			if(bssID==null){
				throw new Exception("Allocation ID cannot be null.");
			}
			if(bssID.startsWith("INTERACTIVE")) {
				throw new Exception("Allocation is not yet ready!");
			}
			String varName = action.getProcessingContext().getAs(TSIMessages.ALLOCATION_ID, String.class);
			if(varName!=null)job.getParameters().put(varName, bssID);
			job.setProperty(TSIMessages.ALLOCATION_ID, bssID);
			Home tssHome = (TargetSystemHomeImpl)kernel.getHome(UAS.TSS);
			String tssID = getModel().getParentUID();
			TargetSystemImpl tss = (TargetSystemImpl)tssHome.get(tssID);
			Jobs.checkSubmissionEnabled(kernel);
			String jobID = Jobs.doSubmit(job, tss, kernel);
			return Response.created(new URI(getBaseURL()+"/jobs/"+jobID)).build();
		}catch(Exception ex){
			return handleError("Could not submit task into allocation", ex, logger);
		}
	}

	@GET
	@Path("/{uniqueID}/forward-port")
	@Produces(MediaType.APPLICATION_JSON)
	public Response startForwarding(
			@HeaderParam(value="Upgrade") String upgrade,
			@QueryParam(value="host") String host,
			@QueryParam(value="port") String portS,
			@QueryParam(value="loginNode") String loginNode)
	{
		if(ForwardingHelper.REQ_UPGRADE_HEADER_VALUE.equalsIgnoreCase(upgrade)) {
			try{
				Action action = getResource().getXNJSAction();
				String bssID = action.getBSID();
				if(bssID==null){
					throw new Exception("Job BSSID cannot be null.");
				}
				if(portS==null) {
					// TODO we might already have the host/port via the job
					throw new Exception("Port cannot be null");
				}
				return doStartForwarding(host, portS, loginNode);
			}catch(Exception ex){
				return handleError("Could not connect to backend service", ex, logger);
			}
		}
		else {
			try {
				// TODO return infos about accessible services
				JSONObject info = new JSONObject();
				info.put("status", "Nothing to see here yet.");
				return Response.ok(info.toString()).build();
			}catch(Exception ex) {
				return handleError("Error generating forwarding information.", ex, logger);
			}
		}
	}

	protected Response doStartForwarding(String host, String portS, String loginNode) {
		try{
			if(host==null)host="localhost";
			int port = Integer.valueOf(portS);
			SocketChannel backend = getBackend(host, port, loginNode);
			ResponseBuilderImpl res = new ResponseBuilderImpl();
			res.status(HttpStatus.SWITCHING_PROTOCOLS_101);
			res.header("Upgrade", "UNICORE-Socket-Forwarding");
			RestServlet.backends.set(backend);
			return res.build();
		}catch(Exception ex){
			return handleError("Could not connect to backend service", ex, logger);
		}
	}

	protected SocketChannel getBackend(String host, int port, String loginNode) throws Exception {
		XNJS xnjs = getResource().getXNJSFacade().getXNJS();
		Action action = getResource().getXNJSAction();
		String tsiNode = loginNode!=null ?
				loginNode : action.getExecutionContext().getPreferredExecutionHost();
		TSI tsi = xnjs.getTargetSystemInterface(AuthZAttributeStore.getClient(), tsiNode);
		return tsi.openConnection(host, port);
	}

	@Override
	protected JSONObject doHandleAction(String action, JSONObject param) throws Exception {
		JobManagementImpl job = getResource();
		JSONObject reply = null;
		if("start".equals(action)){
			job.start();
		}
		else if("abort".equals(action)){
			job.abort();
		}
		else if("restart".equals(action)){
			job.restart();
		}
		return reply;
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
		props.put("jobType", getJobType(action));
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
		String base =  baseURL+"/jobs/"+resource.getUniqueID();
		links.add(new Link("details", base+"/details", "BSS job details"));
		links.add(new Link("action:start", base+"/actions/start", "Start"));
		links.add(new Link("action:abort", base+"/actions/abort", "Abort"));
		links.add(new Link("action:restart", base+"/actions/restart", "Restart"));
		links.add(new Link("forwarding", base+"/forward-port", "Start port forwarding"));
		links.add(new Link("workingDirectory", baseURL+"/storages/"+model.getUspaceId(), "Working directory"));
		links.add(new Link("parentTSS", baseURL+"/sites/"+model.getParentUID(), "Parent TSS"));
	}

	/**
	 * submission code used from both Jobs and Sites resources
	 * returns ID of the new job instance
	 */
	public static String doSubmit(Builder job, TargetSystemImpl tss, Kernel kernel) 
	throws Exception {
		boolean autoRun = !Boolean.parseBoolean(job.getProperty("haveClientStageIn"));
		final String id = tss.submit(job.getJSON(), autoRun, null, job.getTags());
		ResourceAddedMessage m = new ResourceAddedMessage(UAS.JMS, id);
		kernel.getMessaging().getChannel(tss.getUniqueID()).publish(m);
		return id;
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

	public static String getJobType(Action a) {
		String jt = "n/a";
		if(a.getApplicationInfo()!=null) {
			if(a.getApplicationInfo().isRunOnLoginNode())jt = "ON_LOGIN_NODE";
			else if(a.getApplicationInfo().isAllocateOnly())jt = "ALLOCATE";
			else jt ="BATCH";
		}
		return jt;
	}

}
