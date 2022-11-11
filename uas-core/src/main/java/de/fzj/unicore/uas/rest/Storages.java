package de.fzj.unicore.uas.rest;

import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import de.fzj.unicore.uas.SMSProperties;
import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.fts.FileTransferImpl;
import de.fzj.unicore.uas.fts.FileTransferModel;
import de.fzj.unicore.uas.impl.sms.SMSBaseImpl;
import de.fzj.unicore.uas.impl.sms.SMSModel;
import de.fzj.unicore.uas.impl.sms.StorageFactoryImpl;
import de.fzj.unicore.uas.json.JSONUtil;
import de.fzj.unicore.uas.metadata.MetadataManager;
import de.fzj.unicore.uas.metadata.SearchResult;
import de.fzj.unicore.uas.util.LogUtil;
import de.fzj.unicore.xnjs.io.IStorageAdapter;
import de.fzj.unicore.xnjs.io.XnjsStorageInfo;
import eu.unicore.security.Client;
import eu.unicore.services.Home;
import eu.unicore.services.exceptions.InvalidModificationException;
import eu.unicore.services.rest.Link;
import eu.unicore.services.rest.USEResource;
import eu.unicore.services.rest.impl.ServicesBase;
import eu.unicore.services.security.util.AuthZAttributeStore;
import eu.unicore.util.ConcurrentAccess;
import eu.unicore.util.Log;

/**
 * REST interface to storages
 *
 * @author schuller
 */
@Path("/storages")
@USEResource(home=UAS.SMS)
public class Storages extends ServicesBase {

	private static final Logger logger=Log.getLogger(LogUtil.SERVICES,Storages.class);

	protected String getResourcesName(){
		return "storages";
	}

	@Override
	protected Map<String,Object>getProperties() throws Exception {
		Map<String,Object> props = super.getProperties();
		SMSModel model = getModel();
		IStorageAdapter sip = getResource().getStorageAdapter();
		
		props.put("protocols", getJSONObject(getResource().getAvailableProtocols()));
		props.put("umask", model.getUmask());
		props.put("mountPoint", sip.getStorageRoot());
		props.put("description", model.getStorageDescription().getDescription());
		props.put("filesystemDescription", sip.getFileSystemIdentifier());
		props.put("metadataSupported", !model.getStorageDescription().isDisableMetadata());
		try {
			XnjsStorageInfo info = sip.getAvailableDiskSpace(model.getWorkdir());
			props.put("freeSpace", info.getFreeSpace());
			props.put("usableSpace", info.getUsableSpace());
		}catch(Exception ex) {
			logger.error("Problem getting free space", ex);
			props.put("freeSpace", "-1");
			props.put("usableSpace", "-1");
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
	 * using a JSON transfer definition (localPath, source/target, protocol, extra params?)
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
			Map<String,String>extraParameters = JSONUtil.asMap(json.optJSONObject("extraParameters"));
			String id = getResource().transferFile(source, target, isExport, extraParameters);
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
				String id = StorageFactories.createSMS(smf,jsonString);
				return Response.created(new URI(getBaseURL()+"/storages/"+id)).build();
			}
		}catch(Exception ex){
			return handleError("Error creating storage", ex, logger);
		}
	}

	/**
	 * handle the named action
	 * <ul>
	 * <li>rename: with parameters 'from' and 'to'</li>
	 * <li>copy: with parameters 'from' and 'to'</li>
	 * </ul>
	 */
	protected void doHandleAction(String name, JSONObject o) throws Exception {
		if("rename".equals(name)){
			String source = o.optString("from", null);
			String target= o.optString("to", null);
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
		else{
			throw new WebApplicationException("Action '"+name+"' not available.", 404);
		}
	}

	@Override
	protected boolean doSetProperty(String name, String value) throws Exception {
		if("umask".equals(name)){
			if (!SMSProperties.umaskPattern.matcher(value).matches())
				throw new InvalidModificationException("Specified umask must be an octal number from 0 to 777.");
			getModel().setUmask(value);
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
		Client client = AuthZAttributeStore.getClient();
		return home.getAccessibleResources(client).get(0);
	}

	@Override
	protected void updateLinks() {
		super.updateLinks();
		String base = getBaseURL()+"/storages/"+resource.getUniqueID();
		links.add(new Link("files", base+"/files", "Files"));
		if(!getModel().getStorageDescription().isDisableMetadata()){
			links.add(new Link("metadata-search", base+"/search", "Search in metadata"));
			links.add(new Link("metadata-extract", base+"/files/actions/extract", "Extract metadata"));
		}
		links.add(new Link("action:copy", base+"/actions/copy", "Copy file 'from' to file 'to'."));
		links.add(new Link("action:rename", base+"/actions/rename","Rename file 'from' to file 'to'."));
		String smfID = getModel().getParentUID();
		if(smfID != null){
			links.add(new Link("factory",getBaseURL()+"/storagefactories/"+smfID, "Storage Factory"));
		}
	}
}
