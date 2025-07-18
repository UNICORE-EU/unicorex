package eu.unicore.uas.rest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import eu.unicore.security.OperationType;
import eu.unicore.security.SEIOperationType;
import eu.unicore.services.Home;
import eu.unicore.services.InitParameters;
import eu.unicore.services.Kernel;
import eu.unicore.services.rest.Link;
import eu.unicore.services.rest.PagingHelper;
import eu.unicore.services.rest.RESTUtils;
import eu.unicore.services.rest.impl.RESTRendererBase;
import eu.unicore.uas.UAS;
import eu.unicore.uas.impl.sms.SMSBaseImpl;
import eu.unicore.uas.impl.sms.SMSUtils;
import eu.unicore.uas.json.JSONUtil;
import eu.unicore.uas.metadata.ExtractionStatistics;
import eu.unicore.uas.metadata.ExtractionWatcher;
import eu.unicore.uas.metadata.MetadataManager;
import eu.unicore.util.ConcurrentAccess;
import eu.unicore.util.Log;
import eu.unicore.util.Pair;
import eu.unicore.xnjs.ems.ExecutionException;
import eu.unicore.xnjs.io.IStorageAdapter;
import eu.unicore.xnjs.io.XnjsFile;
import eu.unicore.xnjs.io.XnjsFileWithACL;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.Variant.VariantListBuilder;

/**
 * REST files sub-resource
 * 
 * @author schuller
 */
public class Files extends RESTRendererBase {

	private static final Logger logger = Log.getLogger("unicore.rest", Files.class);

	final Kernel kernel;
	final SMSBaseImpl sms;

	public Files(Kernel kernel, SMSBaseImpl sms, String baseURL){
		this.sms = sms;
		this.kernel = kernel;
		this.baseURL = baseURL;
	}


	/**
	 * handle special case: empty path is the root dir
	 */
	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	@ConcurrentAccess(allow=true)
	public Response list(
			@QueryParam("offset") @DefaultValue("0") int offset, 
			@QueryParam("num") @DefaultValue("50000") int num) 
					throws Exception {
		return list("/", 0, SMSBaseImpl.MAX_LS_RESULTS);
	}

	/**
	 * get directory listings or file properties in JSON format
	 */
	@GET
	@Path("/{path:.*}")
	@Produces(MediaType.APPLICATION_JSON)
	@ConcurrentAccess(allow=true)
	public Response list(@PathParam("path")String path, @QueryParam("offset") @DefaultValue("0") int offset, 
			@QueryParam("num") @DefaultValue("50000") int num) throws Exception {
		try{
			this.num = num;
			this.offset = offset;
			if(path== null || path.isEmpty())path="/";
			path = sms.makeSMSLocal(path);
			resourceID = path;
			JSONObject o = getJSON();

			boolean isDirectory = o.optJSONObject("content")!=null;
			if(isDirectory){
				// add links to next/prev chunks
				int actualNum = o.getJSONObject("content").length();
				String base = getBaseURL();
				PagingHelper ph = new PagingHelper(base, base, path);
				Collection<Link> links = ph.getLinks(offset, actualNum, offset+actualNum+500);
				for(Link l: links){
					o.getJSONObject("_links").put(l.getRelation(), renderJSONLink(l));
				}
			}
			ResponseBuilder rb = Response.ok(o.toString(),MediaType.APPLICATION_JSON);
			if(!isDirectory){
				// let us tell the client that we support byte range for reading from a file
				rb.header("Accept-Ranges","bytes");
			}
			return rb.build();
		}catch(Exception ex){
			return handleError("Error listing '"+path+"'", ex, logger);
		}
	}

	/**
	 * handle special case: empty path is the root dir
	 */
	@GET
	@Path("/")
	@Produces(MediaType.TEXT_HTML)
	@ConcurrentAccess(allow=true)
	public String listHTML() throws Exception {
		return listHTML("/");
	}

	/**
	 * get directory listings or file properties in HTML format
	 */
	@GET
	@Path("/{path:.*}")
	@Produces(MediaType.TEXT_HTML)
	@ConcurrentAccess(allow=true)
	public String listHTML(@PathParam("path") String path) throws Exception {
		if(path== null || path.isEmpty())path="/";
		resourceID = path;
		return getHTML();
	}

	/**
	 * modify file properties / permissions
	 */
	@PUT
	@Path("/{path:.*}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@SEIOperationType(OperationType.write)
	public Response modify(@PathParam("path")String path, String jsonString) throws Exception {

		try{
			if(path== null || path.isEmpty())path="/";
			path = sms.makeSMSLocal(path);

			JSONObject json = new JSONObject(jsonString);
			IStorageAdapter tsi=sms.getStorageAdapter();

			XnjsFileWithACL props = sms.getProperties(path);
			if(props == null){
				throw new WebApplicationException(404);
			}
			JSONObject reply = new JSONObject();
			String permissions = json.optString("permissions", null);
			if(permissions==null)permissions = json.optString("unixPermissions", null);
			if(permissions!=null){
				try{
					tsi.chmod2(path, SMSUtils.getChangePermissions(permissions), false);
					reply.put("permissions","OK");
				}catch(Exception e) {
					reply.put("permissions","FAILED: "+Log.getDetailMessage(e));
				}
			}

			String group = json.optString("group", null);
			if(group!=null){
				try {
					tsi.chgrp(path, group, false);
					reply.put("group","OK");
				}catch(Exception e) {
					reply.put("group","FAILED: "+Log.getDetailMessage(e));
				}
			}

			JSONObject jMeta = json.optJSONObject("metadata");
			if(jMeta!=null){
				MetadataManager mm = sms.getMetadataManager();
				String metadataMsg = "OK";
				if(mm!=null){
					Map<String,String> md = JSONUtil.asMap(jMeta);
					try {
						if(md.isEmpty()) {
							mm.removeMetadata(path);
						}
						else{
							mm.createMetadata(path, md);
						}
					}catch(Exception e) {
						metadataMsg = "FAILED: "+Log.getDetailMessage(e);
					}
				}
				else {
					metadataMsg = "Metadata is not supported for this storage.";
				}
				reply.put("metadata", metadataMsg);
			}

			return Response.ok(reply.toString(),MediaType.APPLICATION_JSON).build();
		}catch(Exception ex){
			return handleError("",ex,logger);
		}
	}

	/**
	 * handle actions.
	 * 
	 * <li>extract: extract metadata starting at the current directory</li>
	 * 
	 */
	@POST
	@Path("/actions/{action}/{path:.*}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response handleAction(@PathParam("path")String path, @PathParam("action")String action, String jsonString) throws Exception {
		if("extract".equals(action)) {
			return startMetadataExtraction(path, jsonString);
		}
		else {
			throw new WebApplicationException("Action '"+action+"' not available.", 404);
		}
	}
	
	protected Response startMetadataExtraction(String path, String jsonString) throws Exception {
		try{
			if(path== null || path.isEmpty())path="/";
			if(!path.startsWith("/"))path="/"+path;
			XnjsFileWithACL props = sms.getProperties(path);
			if(props == null){
				throw new WebApplicationException(404);
			}

			MetadataManager mm = sms.getMetadataManager();
			if(mm == null)throw new WebApplicationException(404);

			JSONObject reply = new JSONObject();

			JSONObject json = new JSONObject(jsonString);
			List<String>files = new ArrayList<>(); 
			List<Pair<String, Integer>> dirs = new ArrayList<>();
			if(props.isDirectory()){
				int depth = json.optInt("depth", 10);
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
			String taskHref = makeMonitoringTask(futureResult, path);
			if(taskHref!=null)reply.put("taskHref", taskHref);

			return Response.ok(reply.toString(),MediaType.APPLICATION_JSON).build();
		}catch(Exception ex){
			return handleError("Error setting up metadata extraction", ex, logger);
		}
	}


	/**
	 * get file content
	 */
	@GET
	@Path("/{path:.*}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@ConcurrentAccess(allow=true)
	public Response download(@PathParam("path")String path, @HeaderParam("range")String range) 
			throws Exception {
		InputStream is = null;
		try{
			if(path==null || path.isEmpty())path="/";
			path=sms.makeSMSLocal(path);
			XnjsFileWithACL props = sms.getProperties(path);
			if(props == null){
				return Response.status(404).build();
			}
			boolean isDirectory = props.isDirectory();
			if(isDirectory){
				return Response.notAcceptable(
						VariantListBuilder.newInstance().mediaTypes(MediaType.APPLICATION_JSON_TYPE).build())
						.build();
			}
			String mt = MediaType.APPLICATION_OCTET_STREAM;
			// figure out media type
			if(sms.getMetadataManager()!=null){
				Map<String,String>meta = sms.getMetadataManager().getMetadataByName(path);
				mt = meta.getOrDefault("Content-Type", mt);
				mt = meta.getOrDefault("content-type", mt);
			}
			is = sms.getStorageAdapter().getInputStream(path);
			boolean wantRange = range!=null;
			if(wantRange){
				long size = props.getSize();
				try{
					is = makeRangedStream(range, is, size);
				}
				catch(Exception ex){
					IOUtils.closeQuietly(is);
					return handleError(416,"Range header '"+range+"' cannot be parsed", ex, logger);
				}
				return Response.status(Status.PARTIAL_CONTENT).entity(is).type(mt).build();
			}
			else{
				return Response.ok().entity(is).type(mt).build();	
			}
		}catch(Exception ex){
			IOUtils.closeQuietly(is);
			return handleError("Error downloading from '"+path+"'", ex, logger);
		}
	}

	private InputStream makeRangedStream(String rangeHeader, InputStream source, long fileSize) throws IOException {
		String[] rangeSpec = rangeHeader.split("=");
		if(!"bytes".equals(rangeSpec[0]))throw new IllegalArgumentException();
		String range = rangeSpec[1].trim();

		long offset = 0;
		long length = -1;
		String[] tok = range.split("-");
		boolean wantLastlastPart = range.startsWith("-"); // e.g. "Range: bytes=-100" for the last 100 bytes
		if(wantLastlastPart) {
			offset = fileSize - Long.parseLong(tok[1]);
		}
		else {
			offset = Long.parseLong(tok[0]);
			if(tok.length>1){
				long last = Long.parseLong(tok[1]);
				if(last<offset)throw new IllegalArgumentException();
				length = last+1-offset;
			}
		}
		if(offset<0)throw new IllegalArgumentException();
		while(offset>0){
			offset -= source.skip(offset);
		}
		return length<0 ? source : 
			BoundedInputStream.builder().setInputStream(source).setMaxCount(length).get();
	}

	/**
	 * upload file content
	 */
	@PUT
	@Path("/{path:.*}")
	@SEIOperationType(OperationType.write)
	public Response upload(@PathParam("path")String path, InputStream content, @QueryParam("size") Long size, 
			@HeaderParam("Content-Type") String mediaType) 
					throws Exception {

		try{
			if(path==null || path.isEmpty())path="/";
			path = sms.makeSMSLocal(path);
			createParentDirectories(path);
			long length = size!=null? size: -1;

			try(OutputStream os = sms.getStorageAdapter().getOutputStream(path,false,length)){
				IOUtils.copy(content, os);
			}
			// store media type in metadata
			if(mediaType!=null && sms.getMetadataManager()!=null){
				try{
					Map<String,String>meta = sms.getMetadataManager().getMetadataByName(path);
					meta.put("Content-Type", mediaType);
					sms.getMetadataManager().updateMetadata(path, meta);
				}
				catch(Exception ex){}
			}
			return Response.noContent().build();
		}catch(Exception ex){
			return handleError("Error uploading to '"+path+"'", ex, logger);
		}
	}

	/**
	 * delete a file
	 */
	@DELETE
	@Path("/{path:.*}")
	@SEIOperationType(OperationType.write)
	public Response delete(@PathParam("path")String path) 
			throws Exception {

		if(path == null || path.isEmpty())path="/";
		try{
			sms.doDelete(path);
			return Response.noContent().build();
		}
		catch(Exception e){
			return handleError("Cannot delete <"+path+">", e, logger);
		}
	}


	/**
	 * create a new directory
	 */
	@POST
	@Path("/{path:.*}")
	public Response mkdir(@PathParam("path")String path) 
			throws Exception {

		if(path == null || path.isEmpty())path="/";
		try{
			sms.mkdir(path);
			String location = RESTUtils.makeHref(kernel, "core/storages", sms.getUniqueID()+"/files/"+path);
			return Response.created(new URI(location)).build();
		}
		catch(Exception e){
			return handleError("Cannot create directory <"+path+">", e, logger);
		}
	}

	int offset = 0;
	int num = SMSBaseImpl.MAX_LS_RESULTS;

	@Override
	protected Map<String, Object> getProperties() throws Exception {
		if(resourceID == null)throw new IllegalStateException();

		XnjsFileWithACL props = sms.getProperties(resourceID);
		if(props == null){
			throw new WebApplicationException(404);
		}
		Map<String,Object> o = toProperties(props);
		if(props.isDirectory()){
			Map<String,Object> childrenInfo = new HashMap<>();
			for(XnjsFile f: sms.getListing(resourceID, offset, num, false)) {
				String p = f.isDirectory() ? f.getPath()+"/" : f.getPath();
				childrenInfo.put(p, getJSONObject(toProperties(f)));
			}
			o.put("content", childrenInfo); 
		}
		Map<String,String> meta = new HashMap<>();
		MetadataManager mm = sms.getMetadataManager();
		if(mm!=null){
			meta.putAll(mm.getMetadataByName(resourceID));
		}
		if(props.getMetadata()!=null) {
			try {
				JSONObject xnjsMeta = new JSONObject(props.getMetadata());
				meta.putAll(JSONUtil.asMap(xnjsMeta));
			}catch(JSONException je){
				meta.put("backend-metadata", props.getMetadata());
			}
		}
		o.put("metadata",meta);
		// TODO other properties, ACLs and the like
		return o;
	}

	private Map<String,Object>toProperties(XnjsFile props){
		Map<String,Object> o = new HashMap<>();
		o.put("isDirectory", props.isDirectory());
		if(props.getLastModified()!=null){
			o.put("lastAccessed", getISODateFormatter().format(props.getLastModified().getTime()));
		}
		o.put("size", props.getSize());
		o.put("owner", props.getOwner());
		o.put("group", props.getGroup());
		o.put("permissions", props.getUNIXPermissions()!=null ? props.getUNIXPermissions(): props.getPermissions());
		return o;
	}

	@Override
	protected void updateLinks() {
		links.add(new Link("parentStorage",RESTUtils.makeHref(kernel, "core/storages", sms.getUniqueID()),
				"Parent Storage"));
		if(!sms.getModel().getStorageDescription().isDisableMetadata()){
			String base = RESTUtils.makeHref(kernel, "core/storages", sms.getUniqueID());
			String path = "/".equals(resourceID)? "/actions/extract" : "/files/actions/extract/"+resourceID;
			links.add(new Link("action:extract",
					base+FilenameUtils.normalize(path),
					"Extract metadata for this file"));
		}
	}


	/**
	 * creates missing directories
	 */
	protected void createParentDirectories(String target) throws Exception{
		String s = new File(target).getParent();
		if(s==null || "/".equals(s))return;
		IStorageAdapter storage=sms.getStorageAdapter();
		XnjsFile parent=storage.getProperties(s);
		if(parent==null){
			storage.mkdir(s);
		}
		else if(!parent.isDirectory()){
			throw new ExecutionException("Parent <"+s+"> is not a directory");
		}
	}

	protected String makeMonitoringTask(Future<ExtractionStatistics> f, String path) {
		Home taskHome = kernel.getHome(UAS.TASK);
		if (taskHome == null) {
			return null;
		}
		InitParameters init = new InitParameters();
		init.parentUUID = resourceID;
		String base = RESTUtils.makeHref(kernel, "core/storages", sms.getUniqueID());
		init.parentServiceName = base+"/files"+path;
		try {
			String uid = taskHome.createResource(init);
			new ExtractionWatcher(f, uid, kernel).run();
			return kernel.getContainerProperties().getContainerURL()+"/rest/core/tasks/"+uid;
		}catch(Exception ex) {
			Log.logException("Cannot create task instance", ex);
		}
		return null;
	}

}
