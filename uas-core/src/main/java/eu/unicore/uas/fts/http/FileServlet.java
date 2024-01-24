package eu.unicore.uas.fts.http;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletHolder;

import eu.unicore.security.Client;
import eu.unicore.services.Kernel;
import eu.unicore.services.exceptions.ResourceUnknownException;
import eu.unicore.services.security.util.AuthZAttributeStore;
import eu.unicore.uas.UAS;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * this servlet exposes files under a "hard to guess" URL 
 * 
 * @author schuller
 * @since 1.0.1
 */
public class FileServlet extends DefaultServlet {

	private static final long serialVersionUID = 1L;

	private final Map<String,Long> transferredBytes=new ConcurrentHashMap<>();

	private final Kernel kernel;

	public FileServlet(Kernel kernel){
		this.kernel=kernel;
	}

	public static synchronized void initialise(Kernel kernel){
		if(kernel.getAttribute(FileServlet.class)!=null)return;
		FileServlet fs=new FileServlet(kernel);
		kernel.setAttribute(FileServlet.class, fs);
		ServletHolder sh=new ServletHolder("fileServlet", fs);
		kernel.getServer().getRootServletContext().addServlet(sh,"/files/*");
	}

	public void cleanup(String id){
		transferredBytes.remove(id);
	}

	public Long getTransferredBytes(String id){
		return transferredBytes.get(id);
	}

	public void setTransferredBytes(String id, Long transferred){
		transferredBytes.put(id,transferred);
	}


	@Override
	public UResource getResource(String pathInContext) {
		return getUResource(new File(pathInContext));
	}

	private UResource getUResource(File base) {
		return createResource(base.getName());
	}

	private UResource createResource(String id) {
		Client old = AuthZAttributeStore.getClient();
		try{
			HttpFileTransferImpl r = (HttpFileTransferImpl)kernel.getHome(UAS.CLIENT_FTS).get(id);
			HttpFileTransferModel m = r.getModel();
			AuthZAttributeStore.setClient(m.getClient());
			UResource u = new UResource(id, r.getPath(), r.getStorageAdapter(), kernel);
			u.setAppend(!m.getOverWrite());
			return u;
		}catch(ResourceUnknownException re){
			return null;
		}catch(Exception ie){
			throw new RuntimeException(ie);
		}
		finally{
			AuthZAttributeStore.setClient(old);	
		}
	}

	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		UResource r = getResource(request.getRequestURI());
		try(OutputStream out = r.getOutputStream()){
			IOUtils.copy(request.getInputStream(), out);
			response.setStatus(HttpURLConnection.HTTP_NO_CONTENT);
		}
	} 


	private static final String multipart="multipart/form-data";

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		UResource r=getResource(request.getRequestURI());
		//check that it is the proper type
		String contentType=request.getHeader("Content-Type");
		if(contentType==null || contentType.indexOf(multipart)==-1){
			throw new ServletException("Invalid content type: only '"+multipart+"' is accepted.");
		}
		//OK just read the body
		try(OutputStream out = r.getOutputStream()){
			IOUtils.copy(request.getInputStream(),out);
			response.setStatus(HttpURLConnection.HTTP_NO_CONTENT);
		}
	} 
	
}
