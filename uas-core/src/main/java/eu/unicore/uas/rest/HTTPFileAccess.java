package eu.unicore.uas.rest;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;

import eu.unicore.security.Client;
import eu.unicore.services.Kernel;
import eu.unicore.services.exceptions.ResourceUnknownException;
import eu.unicore.services.rest.impl.RESTRendererBase;
import eu.unicore.services.security.util.AuthZAttributeStore;
import eu.unicore.uas.UAS;
import eu.unicore.uas.fts.http.HttpFileTransferImpl;
import eu.unicore.uas.fts.http.HttpFileTransferModel;
import eu.unicore.uas.fts.http.UResource;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.Response.Status;

/**
 * Provides file access for the HTTP filetransfer protocol
 * 
 * @author schuller
 */
@Path("/")
public class HTTPFileAccess {

	// workaround for TODO: USERestInvoker should honor KernelInjectable
	static Kernel kernel;

	@GET
	@Path("{uniqueID}")
	public Response download(@PathParam("uniqueID") String uniqueID, @HeaderParam("range")String rangeHeader) {
		Response r = null;
		try{
			final UResource resource = getResorce(uniqueID);
			final Range range = new Range(rangeHeader, resource.length());
			final String fileName = new File(resource.getName()).getName();
			ResponseBuilder rb = rangeHeader!=null? Response.status(Status.PARTIAL_CONTENT) : Response.ok();
			rb.header("Content-Disposition", "attachment; filename=\""+fileName+"\"");
			if(range.haveRange) {
				resource.setNumberOfBytes(range.length);
				rb.header("Content-Range", String.format("bytes %d-%d/%d",
						range.offset, range.length+1, resource.length()));
			}
			final InputStream in = resource.getInputStream();
			if(range.haveRange) {
				long toSkip = range.offset;
				while(toSkip>0) {
					toSkip -= in.skip(toSkip);
				}
			}
			rb.entity(in);
			return rb.build();
		}catch(ResourceUnknownException rue) {
			r = RESTRendererBase.createErrorResponse(404, "Not found.");
		}catch(Exception ex){
			r = RESTRendererBase.handleError(500, "", ex, null);
		}
		return r;
	}

	@PUT
	@Path("/{uniqueID}")
	public Response upload(InputStream content, @PathParam("uniqueID") String uniqueID) {
		Response r = null;
		try{
			UResource resource = getResorce(uniqueID);
			OutputStream out = resource.getOutputStream();
			IOUtils.copy(content, out);
			r = Response.noContent().build();
		}catch(ResourceUnknownException rue) {
			r = RESTRendererBase.createErrorResponse(404, "Not found.");
		}catch(Exception ex){
			r = RESTRendererBase.handleError(500, "", ex, null);
		}
		return r;
	}


	private UResource getResorce(String id) throws ResourceUnknownException {
		Client old = AuthZAttributeStore.getClient();
		try{
			HttpFileTransferImpl r = (HttpFileTransferImpl)kernel.getHome(UAS.CLIENT_FTS).get(id);
			HttpFileTransferModel m = r.getModel();
			AuthZAttributeStore.setClient(m.getClient());
			UResource u = new UResource(id, r.getPath(), r.getStorageAdapter(), kernel);
			u.setAppend(!m.getOverWrite());
			u.setNumberOfBytes(m.getNumberOfBytes());
			return u;
		}catch(Exception ie){
			throw new RuntimeException(ie);
		}
		finally{
			AuthZAttributeStore.setClient(old);	
		}
	}

	private static class Range {
		private static final String err = "Range header cannot be parsed";

		public long offset;

		public long length;

		public boolean haveRange = false;
	
		public Range(String rangeHeader, long totalLength) {
			offset = 0;
			length = -1;
			if(rangeHeader!=null) {
				haveRange = true;
				String[] rangeSpec = rangeHeader.split("=");
				if(!"bytes".equals(rangeSpec[0]))throw new IllegalArgumentException(err);
				String range = rangeSpec[1];
				String[] tok = range.split("-");
				if(tok.length>2)throw new IllegalArgumentException(err);
				if(tok.length==1){
					// only offset
					offset = Long.parseLong(tok[0]);
					length = totalLength-offset;
				}
				else if(tok[0].length()==0) {
					// want the tail
					length = Long.parseLong(tok[1]);
					offset = totalLength - length;
				}
				else {
					// normal range
					offset = Long.parseLong(tok[0]);
					long last = Long.parseLong(tok[1]);
					if(last<offset)throw new IllegalArgumentException(err);
					length = last+1-offset;
				}
				if(offset<0)throw new IllegalArgumentException(err);
			}
		}
	}
}
