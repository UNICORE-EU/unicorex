package de.fzj.unicore.uas.rest;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.impl.tss.TargetSystemHomeImpl;
import eu.emi.security.authn.x509.X509Credential;
import eu.emi.security.authn.x509.impl.CertificateUtils;
import eu.emi.security.authn.x509.impl.CertificateUtils.Encoding;
import eu.unicore.security.AuthenticationException;
import eu.unicore.services.rest.Link;
import eu.unicore.services.rest.impl.ApplicationBaseResource;
import eu.unicore.services.rest.jwt.JWTServerProperties;
import eu.unicore.services.rest.security.AuthNHandler;
import eu.unicore.services.rest.security.jwt.JWTUtils;
import eu.unicore.services.security.util.AuthZAttributeStore;
import eu.unicore.util.Log;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Parent resource for the core services, has links to the actual resources 
 * and shows some useful info about the server and the current client
 * 
 * @author schuller
 */
@Path("/")
public class Base extends ApplicationBaseResource {

	private final static Logger logger = Log.getLogger(Log.SERVICES, Base.class);
	
	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	public Response listAll(@QueryParam("fields")String fields) throws Exception {
		try {
			parsePropertySpec(fields);
			return Response.ok().entity(getJSON().toString()).build();
		}catch(Exception ex) {
			return handleError("", ex, logger);
		}
	}
	
	@GET
	@Path("/certificate")
	@Produces(MediaType.TEXT_PLAIN)
	public Response getCert() throws Exception {
		try {
			String pem = "n/a";
			if(kernel.getContainerSecurityConfiguration().getCredential()!=null) {
				ByteArrayOutputStream os = new ByteArrayOutputStream();
				CertificateUtils.saveCertificate(os, 
						kernel.getContainerSecurityConfiguration().getCredential().getCertificate(), 
						Encoding.PEM);
				pem = os.toString("UTF-8");
			}
			return Response.ok().entity(pem).build();
		}
		catch(Exception ex) {
			return handleError("", ex, logger);
		}
	}
	
	@GET
	@Path("/token")
	@Produces(MediaType.TEXT_PLAIN)
	public Response getToken(@QueryParam("lifetime")String lifetimeParam,
			@QueryParam("renewable")String renewable,
			@QueryParam("limited")String limited)
			throws Exception {
		try {
			String method = (String)AuthZAttributeStore.getTokens().getContext().get(AuthNHandler.USER_AUTHN_METHOD);
			if("ETD".equals(method)) {
				if(!(Boolean)AuthZAttributeStore.getTokens().getContext().get(AuthNHandler.ETD_RENEWABLE)) {
					throw new AuthenticationException("Cannot create token when authenticating with a non-renewable token!");
				}
			}
			JWTServerProperties jwtProps = new JWTServerProperties(kernel.getContainerProperties().getRawProperties());
			String user = AuthZAttributeStore.getClient().getDistinguishedName();
			X509Credential issuerCred =  kernel.getContainerSecurityConfiguration().getCredential();
			long lifetime = lifetimeParam!=null? Long.valueOf(lifetimeParam): jwtProps.getTokenValidity();
			Map<String,String> claims = new HashMap<>();
			claims.put("etd", "true");
			if(Boolean.parseBoolean(renewable)) {
				claims.put("renewable", "true");
			}
			if(Boolean.parseBoolean(limited)) {
				claims.put("aud", issuerCred.getSubjectName());
			}
			String token = JWTUtils.createJWTToken(user, lifetime,
					issuerCred.getSubjectName(), issuerCred.getKey(),
					claims);
			return Response.ok().entity(token).build();
		}
		catch(Exception ex) {
			return handleError("Error creating token", ex, logger);
		}
	}

	@GET
	@Path("/")
	@Produces(MediaType.TEXT_HTML)
	public String listAllHtml() throws Exception {
		return getHTML();
	}

	final static String[] resources = new String[]{
			"factories","sites","jobs",
			"reservations",
			"storages","storagefactories",
			"transfers","client-server-transfers", 
			"tasks"
	};

	final static String[] serviceNames = new String[]{
			UAS.TSF, UAS.TSS, UAS.JMS,
			UAS.RESERVATIONS,
			UAS.SMS, UAS.SMF, 
			UAS.SERVER_FTS, UAS.CLIENT_FTS,
			UAS.TASK,
	};

	@Override
	protected Map<String, Object> renderServerProperties() throws Exception {
		Map<String,Object>props = super.renderServerProperties();
		try{
			TargetSystemHomeImpl tsf = (TargetSystemHomeImpl)kernel.getHome(UAS.TSS);
			boolean enabled = tsf!=null && tsf.isJobSubmissionEnabled();
			String msg = tsf==null? "No job submission at this site." 
					: tsf.getHighMessage();
			Map<String,Object> js = new HashMap<>();
			props.put("jobSubmission", js);
			js.put("enabled", enabled);
			js.put("message", msg);
		}catch(Exception ex){}
		return props;
	}

	@Override
	protected void updateLinks() {
		for(int i = 0; i<resources.length; i++){
			String r = resources[i];
			String sName = serviceNames[i];
			if(sName!=null) {
				if(kernel.getHome(sName)==null) {
					continue;
				};
			}
			links.add(new Link(r, getBaseURL()+"/"+r));
		}
	}

}
