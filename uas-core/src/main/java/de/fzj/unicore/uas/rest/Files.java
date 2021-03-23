package de.fzj.unicore.uas.rest;

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

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Variant.VariantListBuilder;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import de.fzj.unicore.uas.impl.sms.SMSBaseImpl;
import de.fzj.unicore.uas.impl.sms.SMSUtils;
import de.fzj.unicore.uas.json.JSONUtil;
import de.fzj.unicore.uas.metadata.MetadataManager;
import de.fzj.unicore.uas.util.Pair;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.io.IStorageAdapter;
import de.fzj.unicore.xnjs.io.XnjsFile;
import de.fzj.unicore.xnjs.io.XnjsFileWithACL;
import eu.unicore.services.rest.Link;
import eu.unicore.services.rest.PagingHelper;
import eu.unicore.services.rest.RESTUtils;
import eu.unicore.services.rest.impl.RESTRendererBase;
import eu.unicore.util.ConcurrentAccess;
import eu.unicore.util.Log;

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
			String permissions = json.optString("unixPermissions", null);
			if(permissions!=null){
				try{
					tsi.chmod2(path, SMSUtils.getChangePermissions(permissions), false);
					reply.put("unixPermissions","OK");
				}catch(Exception e) {
					reply.put("unixPermissions","FAILED: "+Log.getDetailMessage(e));
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
					try {
						mm.createMetadata(path, JSONUtil.asMap(jMeta));
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
	@Path("/{path:.*}/actions/{action}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response handleAction(@PathParam("path")String path, @PathParam("action")String action, String jsonString) throws Exception {

		if(!"extract".equals(action))throw new WebApplicationException("Action '"+action+"' not available.", 404);

		try{
			if(path== null || path.isEmpty())path="/";
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
				dirs.add(new Pair<String,Integer>(path,depth));
				// TODO might have a list of files/directories to extract!
			}
			else{
				// single file
				files.add(path);
			}
			mm.startAutoMetadataExtraction(files, dirs);
			reply.put("status", "OK");

			// TODO once we have the REST version of the Task resource, 
			// we can add a link to it!

			return Response.ok(reply.toString(),MediaType.APPLICATION_JSON).build();
		}catch(Exception ex){
			return handleError("Error handling action '"+action+"'", ex, logger);
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
				try{
					is = makeRangedStream(range, is);
				}
				catch(Exception ex){
					de.fzj.unicore.xnjs.util.IOUtils.closeQuietly(is);
					return handleError(400,"Range header '"+range+"' cannot be parsed", ex, logger);
				}
				return Response.status(Status.PARTIAL_CONTENT).entity(is).type(mt).build();
			}
			else{
				return Response.ok().entity(is).type(mt).build();	
			}
			
		}catch(Exception ex){
			de.fzj.unicore.xnjs.util.IOUtils.closeQuietly(is);
			return handleError("Error downloading from '"+path+"'", ex, logger);
		}
	}

	private InputStream makeRangedStream(String rangeHeader, InputStream source) throws IOException {
		String[] rangeSpec = rangeHeader.split("=");
		if(!"bytes".equals(rangeSpec[0]))throw new IllegalArgumentException();
		String range = rangeSpec[1];
		
		long offset = 0;
		long length = -1;
		String[] tok = range.split("-");
		offset = Long.parseLong(tok[0]);
		if(offset<0)throw new IllegalArgumentException();
		if(tok.length>1){
			long last = Long.parseLong(tok[1]);
			if(last<offset)throw new IllegalArgumentException();
			length = last+1-offset;
		}
		while(offset>0){
			offset -= source.skip(offset);
		}
		return length>=0? new BoundedInputStream(source, length) : source;
	}
	
	/**
	 * upload file content
	 */
	@PUT
	@Path("/{path:.*}")
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
		MetadataManager mm = sms.getMetadataManager();
		if(mm!=null){
			Map<String,String> meta = mm.getMetadataByName(resourceID);
			o.put("metadata",meta);
		}
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
		if(sms.getModel().getMetadataServiceID()!=null){
			String base = RESTUtils.makeHref(kernel, "core/storages", sms.getUniqueID());
			links.add(new Link("action:extract",base+"/files/"+resourceID+"/actions/extract",
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


}
