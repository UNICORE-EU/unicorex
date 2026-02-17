package eu.unicore.uas.rest;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.function.Predicate;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import eu.unicore.services.Home;
import eu.unicore.services.InitParameters;
import eu.unicore.services.exceptions.InvalidModificationException;
import eu.unicore.services.rest.Link;
import eu.unicore.services.rest.RESTUtils;
import eu.unicore.services.rest.USEResource;
import eu.unicore.services.rest.impl.ServicesBase;
import eu.unicore.services.restclient.utils.UnitParser;
import eu.unicore.services.security.util.AuthZAttributeStore;
import eu.unicore.uas.UAS;
import eu.unicore.uas.UASProperties;
import eu.unicore.uas.fts.FileTransferImpl;
import eu.unicore.uas.fts.FileTransferModel;
import eu.unicore.uas.impl.sms.SMSBaseImpl;
import eu.unicore.uas.impl.sms.SMSModel;
import eu.unicore.uas.impl.sms.StorageFactoryImpl;
import eu.unicore.uas.impl.sms.UspaceStorageImpl;
import eu.unicore.uas.json.JSONUtil;
import eu.unicore.uas.metadata.ExtractionStatistics;
import eu.unicore.uas.metadata.ExtractionWatcher;
import eu.unicore.uas.metadata.FederatedMetadataSearchWatcher;
import eu.unicore.uas.metadata.FederatedSearchResultCollection;
import eu.unicore.uas.metadata.MetadataManager;
import eu.unicore.uas.metadata.MetadataSupport;
import eu.unicore.uas.metadata.SearchResult;
import eu.unicore.uas.trigger.xnjs.TriggerProcessor;
import eu.unicore.uas.util.LogUtil;
import eu.unicore.uas.xnjs.XNJSFacade;
import eu.unicore.util.ConcurrentAccess;
import eu.unicore.util.Log;
import eu.unicore.util.Pair;
import eu.unicore.xnjs.ems.Action;
import eu.unicore.xnjs.ems.ActionStatus;
import eu.unicore.xnjs.io.IStorageAdapter;
import eu.unicore.xnjs.io.XnjsFileWithACL;
import eu.unicore.xnjs.io.XnjsStorageInfo;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST interface to storages
 *
 * @author schuller
 */
@Path("/storages")
@USEResource(home=UAS.SMS)
public class Storages extends ServicesBase {

	private static final Logger logger=Log.getLogger(LogUtil.SERVICES,Storages.class);

	@Override
	protected String getResourcesName(){
		return "storages";
	}

	@Override
	protected Predicate<String>createFilter(String filterSpec){
		if("all".equalsIgnoreCase(filterSpec)) {
			// return everything
			return null;
		}
		else {
			// filter out job directories (storages named "-uspace")
			return t -> t.endsWith("-uspace");
		}
	}

	@Override
	protected Map<String,Object>getProperties() throws Exception {
		Map<String,Object> props = super.getProperties();
		SMSModel model = getModel();
		props.put("protocols", getJSONObject(getResource().getAvailableProtocols()));
		props.put("umask", model.getUmask());
		props.put("description", model.getStorageDescription().getDescription());
		props.put("metadataSupported", !model.getStorageDescription().isDisableMetadata());
		if(model.getStorageDescription().isEnableTrigger()) {
			props.put("dataTriggeredProcessing", getTriggeredProcessingInfo());
		}
		if(getResource().isReady()) {
			IStorageAdapter sip = getResource().getStorageAdapter();
			props.put("mountPoint", sip.getStorageRoot());
			props.put("filesystemDescription", sip.getFileSystemIdentifier());
			try {
				XnjsStorageInfo info = sip.getAvailableDiskSpace(model.getWorkdir());
				props.put("freeSpace", info.getFreeSpace());
				props.put("usableSpace", info.getUsableSpace());
			}catch(Exception ex) {
				logger.error("Problem getting free space", ex);
				props.put("freeSpace", "-1");
				props.put("usableSpace", "-1");
			}
		}
		return props;
	}

	@Override
	public SMSModel getModel(){
		return (SMSModel)model;
	}

	@Override
	public SMSBaseImpl getResource(){
		return (SMSBaseImpl)resource;
	}

	/**
	 * create a new file import resource using a JSON import definition
	 * 
	 * @return address of new file import resource as Location header 
	 *         and a JSON with properties of the new resource
	 */
	@POST
	@Path("/{uniqueID}/imports")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response newImport(String jsonString) throws Exception {
		try{
			JSONObject json = new JSONObject(jsonString);
			String file = json.getString("file");
			String protocol = json.optString("protocol","BFT");
			if(!getResource().getAvailableProtocols().contains(protocol)) {
				throw new IllegalArgumentException("Protocol '"+protocol+"' is not available.");
			}
			Boolean overwrite = Boolean.valueOf(json.optString("overwrite","true"));
			Long numBytes = Long.valueOf(json.optString("numBytes","-1"));
			Map<String,String>extraParameters = JSONUtil.asMap(json.optJSONObject("extraParameters"));
			String id = getResource().createFileImport(file, protocol, overwrite, numBytes, extraParameters);
			String location = getBaseURL()+"/client-server-transfers/"+id;
			JSONObject props = getFiletransferProperties(id, protocol);
			return Response.ok(props.toString(), MediaType.APPLICATION_JSON).
					header("Location",location).build();
		}catch(Exception ex){
			return handleError("Could not create file import", ex, logger);
		}
	}

	/**
	 * create a new file export resource using a JSON export definition
	 * 
	 * @return address of new file export resource as Location header 
	 *         and a JSON with properties of the new resource
	 */
	@POST
	@Path("/{uniqueID}/exports")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response newExport(String jsonString) throws Exception {
		try{
			JSONObject json = new JSONObject(jsonString);
			String file = json.getString("file");
			String protocol = json.optString("protocol","BFT");
			if(!getResource().getAvailableProtocols().contains(protocol)) {
				throw new IllegalArgumentException("Protocol '"+protocol+"' is not available.");
			}
			Map<String,String>extraParameters = JSONUtil.asMap(json.optJSONObject("extraParameters"));
			String id = ((SMSBaseImpl)getResource()).createFileExport(file, protocol, extraParameters);
			String location = getBaseURL()+"/client-server-transfers/"+id;
			JSONObject props = getFiletransferProperties(id, protocol);
			return Response.ok(props.toString(), MediaType.APPLICATION_JSON).
					header("Location",location).build();
		}catch(Exception ex){
			return handleError("Could not create file export", ex, logger);
		}
	}

	/**
	 * create a new server-to-server transfer resource (send/receive file)
	 * using a JSON transfer definition (local file, remote source/target,
	 * protocol, extra parameters)
	 *
	 * @return address of new file import resource
	 */
	@POST
	@Path("/{uniqueID}/transfers")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response transfer(String jsonString) throws Exception {
		try{
			JSONObject json = new JSONObject(jsonString);
			String source, target;
			String localPath = json.getString("file");
			String protocol = json.optString("protocol", null);
			boolean isExport = false;
			String remote = json.optString("source", null);
			if(remote == null){
				remote = json.getString("target");
				isExport = true;
			}
			if(protocol!=null && !remote.startsWith(protocol+":")) {
				remote = protocol+":"+remote;
			}
			if(isExport){
				source = localPath;
				target = remote;
			}
			else{
				source = remote;
				target = localPath;
			}
			JSONArray jtags = json.optJSONArray("Tags");
			if(jtags==null)jtags = json.optJSONArray("tags");
			String[] tags = JSONUtil.toArray(jtags);
			Map<String,String>extraParameters = JSONUtil.asMap(json.optJSONObject("extraParameters"));
			String id = getResource().transferFile(source, target, isExport, extraParameters, tags);
			String location = getBaseURL()+"/transfers/"+id;
			return Response.created(new URI(location)).build();
		}catch(Exception ex){
			return handleError("Could not create transfer", ex, logger);
		}
	}

	/**
	 * handle files as a sub-resource
	 */
	@Path("/{uniqueID}/files")
	public Files getFilesResource() {
		String filesURL = getBaseURL()+"/"+getResourcesName()+"/"+resourceID+"/files";
		return new Files(kernel, getResource(), filesURL);
	}

	public JSONObject getFiletransferProperties(String id, String protocol) throws Exception {
		JSONObject o = new JSONObject();
		FileTransferImpl ftResource = (FileTransferImpl) kernel.getHome(UAS.CLIENT_FTS).get(id);
		o.put("protocol", protocol);
		o.put("parent", getBaseURL()+"/storages/"+resource.getUniqueID());
		FileTransferModel m = ftResource.getModel();
		o.put("isExport", String.valueOf(m.getIsExport()));
		String localFile = m.getIsExport()? m.getSource() : m.getTarget();
		o.put("fileName", localFile);
		Map<String,String> params = ftResource.getProtocolDependentParameters();
		for(String key : params.keySet()){
			o.put(key,params.get(key));
		}
		return o;
	}

	public JSONObject getTriggeredProcessingInfo() throws Exception {
		JSONObject o = new JSONObject();
		String id = resourceID+"-scan";
		Action action = XNJSFacade.get(getModel().getXnjsReference(), kernel).getAction(id);
		if(action!=null) {
			o.put("status", ActionStatus.toString(action.getStatus()));
			long lastRun = TriggerProcessor.getLastRun(action);
			o.put("lastScan", lastRun>0? UnitParser.getISO8601().format(new Date(lastRun)): "n/a");
			o.put("settings", String.valueOf(action.getAjd()));
			o.put("lastRunInfo", TriggerProcessor.getLastRunInfo(action));
		}
		else {
			o.put("status", "Error: action cannot be found");
		}
		return o;
	}

	/**
	 * search 
	 * 
	 * TODO search should be generic and a sub-resource
	 */
	@GET
	@Path("/{uniqueID}/search")
	@Produces(MediaType.APPLICATION_JSON)
	@ConcurrentAccess(allow=true)
	public Response search(@QueryParam("q")String query) throws Exception {
		try{
			JSONObject res = new JSONObject();
			MetadataManager mm = getResource().getMetadataManager();
			if(query==null) {
				res.put("status", "failed");
				res.put("statusMessage", "Query cannot be null.");
			}
			else if(mm == null){
				res.put("status", "failed");
				res.put("statusMessage", "No metadata manager available for this storage.");
			}
			else{
				res.put("status", "OK");
				List<SearchResult> results = mm.searchMetadataByContent(query, true);
				res.put("numberOfResults", results.size());
				String base = getBaseURL()+"/storages/"+resource.getUniqueID();
				int index = 1;
				for(SearchResult sr : results){
					links.add(new Link("search-result-"+index,
							base+FilenameUtils.normalize("/files/"+sr.getResourceName(), true)));
					index++;
				}
				renderJSONLinks(res);
			}
			res.put("query", query);
			return Response.ok(res.toString(), MediaType.APPLICATION_JSON).build();
		}catch(Exception ex){
			return handleError("Error in search", ex, logger);
		}
	}

	/**
	 * create a new storage via the storage factory service
	 * using a JSON description (type, name, parameters, termination time, ...) 
	 * for the new storage
	 */
	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response createSMS(String jsonString) throws Exception {
		try{
			Home smfHome = kernel.getHome(UAS.SMF);
			try(StorageFactoryImpl smf = (StorageFactoryImpl)smfHome.getForUpdate(findSMF())){
				String id = StorageFactories.createSMS(smf,jsonString, null);
				return Response.created(new URI(getBaseURL()+"/storages/"+id)).build();
			}
		}catch(Exception ex){
			return handleError("Error creating storage", ex, logger);
		}
	}

	/**
	 * create a new federated search
	 * using a JSON description (query, urls, parameters, ...) 
	 */
	@POST
	@Path("/search")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response launchFederatedMetadataSearch(String jsonString) throws Exception {
		try{
			JSONObject params = new JSONObject(jsonString);
			MetadataManager mm = MetadataSupport.getManager(kernel, kernel.getAttribute(UASProperties.class));
			String query = params.getString("query");
			boolean isAdvanced = params.optBoolean("advanced");
			List<String>resourcesList = new ArrayList<>();
			JSONArray urls = params.getJSONArray("resources");
			urls.forEach( u ->resourcesList.add(String.valueOf(u)));
			Future<FederatedSearchResultCollection> f = mm.federatedMetadataSearch(AuthZAttributeStore.getClient(),
					query, resourcesList, isAdvanced);
			String taskURL = makeFedSearchMonitoringTask(f);
			return Response.created(new URI(taskURL)).build();
		}catch(Exception ex){
			return handleError("Error launching federated search", ex, logger);
		}
	}

	/**
	 * handle the named action
	 * <ul>
	 * <li>rename: with parameters 'from' and 'to'</li>
	 * <li>copy: with parameters 'from' and 'to'</li>
	 * </ul>
	 */
	protected JSONObject doHandleAction(String name, JSONObject o) throws Exception {
		JSONObject reply = null;
		if("rename".equals(name)){
			String source = o.optString("from", null);
			String target = o.optString("to", null);
			if(source == null || target==null){
				throw new WebApplicationException("Parameters 'from' and 'to' are required.", Response.Status.BAD_REQUEST);
			}
			getResource().rename(source, target);
		}
		else if("copy".equals(name)){
			String source = o.optString("from", null);
			String target= o.optString("to", null);
			if(source == null || target==null){
				throw new WebApplicationException("Parameters 'from' and 'to' are required.", Response.Status.BAD_REQUEST);
			}
			getResource().copy(source, target);
		}
		else if("stop-processing".equals(name)){
			String id = resourceID+"-scan";
			XNJSFacade.get(getModel().getXnjsReference(), kernel).getManager().abort(id, AuthZAttributeStore.getClient());
			getModel().getStorageDescription().setEnableTrigger(false);
		}
		else if("extract".equals(name)){
			reply = startMetadataExtraction(o);
		}
		else{
			reply = super.doHandleAction(name, o);
		}
		return reply;
	}

	@Override
	protected boolean doSetProperty(String name, Object value) throws Exception {
		if("umask".equals(name)){
			getResource().setUmask(String.valueOf(value));
			return true;
		}
		if("description".equals(name)){
			String v = String.valueOf(value);
			if(v.length()>256) {
				throw new InvalidModificationException("Description too long.");
			}
			getModel().getStorageDescription().setDescription(v);
			return true;
		}
		return super.doSetProperty(name, value);
	}

	// returns the ID of the first available SMF instance (usually there will be just one!)
	synchronized String findSMF() throws Exception {
		Home home = kernel.getHome(UAS.SMF);
		if(home == null) {
			throw new IllegalStateException("StorageFactory service is not available at this site!");	
		}
		return home.getAccessibleResources(AuthZAttributeStore.getClient()).get(0);
	}

	@Override
	protected void updateLinks() {
		super.updateLinks();
		String base = getBaseURL()+"/storages/"+resource.getUniqueID();
		SMSModel model = getModel();
		links.add(new Link("files", base+"/files", "Files"));
		if(!model.getStorageDescription().isDisableMetadata()){
			links.add(new Link("metadata-search", base+"/search", "Search in metadata"));
			links.add(new Link("action:extract", base+"/actions/extract", "Start metadata extraction"));
		}
		if(model.getStorageDescription().isEnableTrigger()) {
			links.add(new Link("action:stop-processing", base+"/actions/stop-processing", "Stop the data-triggered processing"));
		}
		links.add(new Link("action:copy", base+"/actions/copy", "Copy file 'from' to file 'to'."));
		links.add(new Link("action:rename", base+"/actions/rename","Rename file 'from' to file 'to'."));
		String smfID = model.getParentUID();
		if(smfID != null){
			links.add(new Link("factory",getBaseURL()+"/storagefactories/"+smfID, "Storage Factory"));
		}
		if(getResource() instanceof UspaceStorageImpl) {
			String jobID = model.getStorageDescription().getPathSpec();
			links.add(new Link("job",getBaseURL()+"/jobs/"+jobID, "Parent job"));
		}
	}

	private JSONObject startMetadataExtraction(JSONObject spec) throws Exception {
		String path = spec.optString("path", null);
		if(path==null || path.isEmpty())path="/";
		if(!path.startsWith("/"))path="/"+path;
		XnjsFileWithACL props = getResource().getProperties(path);
		if(props == null){
			throw new WebApplicationException(404);
		}

		MetadataManager mm = getResource().getMetadataManager();
		if(mm == null)throw new WebApplicationException(404);

		JSONObject reply = new JSONObject();

		List<String>files = new ArrayList<>(); 
		List<Pair<String, Integer>> dirs = new ArrayList<>();
		if(props.isDirectory()){
			int depth = spec.optInt("depth", 10);
			dirs.add(new Pair<>(path,depth));
			// TODO might have a list of files/directories to extract!
		}
		else{
			// single file
			files.add(path);
		}
		Future<ExtractionStatistics> futureResult = mm.startAutoMetadataExtraction(files, dirs);
		reply.put("status", "OK");
		reply.put("asyncExtraction", futureResult!=null);
		reply.put("taskHref", makeExtractionMonitoringTask(futureResult, path));
		return reply;
	}

	private String makeExtractionMonitoringTask(Future<ExtractionStatistics> f, String path) throws Exception {
		Home taskHome = kernel.getHome(UAS.TASK);
		if (taskHome == null) {
			throw new IllegalStateException("Task service is not deployed.");
		}
		InitParameters init = new InitParameters();
		init.parentUUID = resourceID;
		String base = RESTUtils.makeHref(kernel, "core/storages", getResource().getUniqueID());
		init.parentServiceName = base+"/files"+path;
		String uid = taskHome.createResource(init);
		new ExtractionWatcher(f, uid, kernel).run();
		return kernel.getContainerProperties().getContainerURL()+"/rest/core/tasks/"+uid;
	}

	private String makeFedSearchMonitoringTask(Future<FederatedSearchResultCollection> f) throws Exception {
		Home taskHome = kernel.getHome(UAS.TASK);
		if (taskHome == null) {
			throw new IllegalStateException("Task service is not deployed.");
		}
		InitParameters init = new InitParameters();
		init.parentUUID = null;
		init.parentServiceName = kernel.getContainerProperties().getContainerURL()+"/rest/core/storages";
		String uid = taskHome.createResource(init);
		new FederatedMetadataSearchWatcher(f, uid, kernel).run();
		return kernel.getContainerProperties().getContainerURL()+"/rest/core/tasks/"+uid;
	}

}