package de.fzj.unicore.uas.rest;

import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.impl.tss.TSFModel;
import de.fzj.unicore.uas.impl.tss.TargetSystemFactoryImpl;
import de.fzj.unicore.uas.json.JSONUtil;
import de.fzj.unicore.uas.util.UnitParser;
import eu.unicore.security.AuthorisationException;
import eu.unicore.security.Client;
import eu.unicore.services.Home;
import eu.unicore.services.Kernel;
import eu.unicore.services.rest.Link;
import eu.unicore.services.rest.USEResource;
import eu.unicore.services.rest.impl.ServicesBase;
import eu.unicore.services.security.util.AuthZAttributeStore;
import eu.unicore.util.Log;
import eu.unicore.xnjs.ems.BudgetInfo;
import eu.unicore.xnjs.idb.ApplicationInfo;
import eu.unicore.xnjs.idb.IDB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST interface to site factories (TSF instances)
 *
 * @author schuller
 */
@Path("/factories")
@USEResource(home=UAS.TSF)
public class SiteFactories extends ServicesBase {

	protected String getResourcesName(){
		return "factories";
	}

	@Override
	protected Map<String,Object>getProperties() throws Exception {
		TargetSystemFactoryImpl tsf = getResource();
		Map<String,Object> props = super.getProperties();
		TSFModel model = getModel();

		props.put("supportsReservation", String.valueOf(model.getSupportsReservation()));

		IDB idb = tsf.getXNJSFacade().getIDB();
		Map<String,Object> resources = IDBContentRendering.asMap(idb.getPartitions());
		props.put("resources", resources);

		Client client = AuthZAttributeStore.getClient();
		List<String> apps = new ArrayList<>();
		for(ApplicationInfo app: tsf.getXNJSFacade().getDefinedApplications(client)){
			apps.add(app.getName()+IDBContentRendering.appSeparator+app.getVersion());
		}
		props.put("applications", apps);

		Map<String,String> textInfo = new HashMap<>();
		textInfo.putAll(idb.getTextInfoProperties());
		props.put("otherInfo", textInfo);
		try {
			List<BudgetInfo> budget = tsf.getXNJSFacade().getComputeTimeBudget(client);
			props.put("remainingComputeTime", IDBContentRendering.budgetToMap(budget));
		}catch(Exception ex) {
			logger.debug(Log.createFaultMessage("Error getting compute budget for "+client.getDistinguishedName(), ex));
			props.put("remainingComputeTime", new HashMap<>());
		}
		return props;
	}

	@Override
	public TSFModel getModel(){
		return (TSFModel)model;
	}

	@Override
	public TargetSystemFactoryImpl getResource(){
		return (TargetSystemFactoryImpl)resource;
	}

	/**
	 * create a new TSS via the target system factory service
	 * 
	 * @param jsonString - settings for the new TSS
	 */
	@POST
	@Path("/{uniqueID}")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response createTSS(String jsonString) throws Exception {
		try{
			TargetSystemFactoryImpl tsf = getResource();
			String id = createTSS(tsf,jsonString);
			String location = getBaseURL()+"/sites/"+id;
			return Response.created(new URI(location)).build();
		}catch(Exception ex){
			return handleError("Could not create TSS", ex, logger);
		}
	}

	@Override
	protected void updateLinks() {
		super.updateLinks();
		links.add(new Link("sites",getBaseURL()+"/sites"));
		links.add(new Link("applications",getBaseURL()+"/"+getResourcesName()+"/"+resource.getUniqueID()+"/applications"));
	}

	@Path("/{uniqueID}/applications")
	public Applications getApplicationResource() {
		String appURL = getBaseURL()+"/"+getResourcesName()+"/"+resourceID+"/applications";
		return new Applications(kernel, getModel().getXnjsReference(), appURL);
	}

	public static String createTSS(TargetSystemFactoryImpl tsf, String jsonString) throws Exception {
		JSONObject json = new JSONObject(jsonString);
		Calendar tt = null;
		String timeSpec = json.optString("terminationTime",null);
		if(timeSpec!=null){
			tt = Calendar.getInstance();
			tt.setTime(UnitParser.extractDateTime(timeSpec));
		}
		Map<String,String>parameters = JSONUtil.asMap(json.optJSONObject("parameters"));
		return tsf.createTargetSystem(tt,parameters);
	}

	// returns the ID of the first available TSF instance (usually there will be just one!)
	public static synchronized String findTSF(Kernel kernel) throws Exception {
		Home home = kernel.getHome(UAS.TSF);
		if(home == null) {
			throw new IllegalStateException("TargetSystemFactory service is not available at this site!");
		}
		Client client = AuthZAttributeStore.getClient();
		List<String> tsfs = home.getAccessibleResources(client);
		if(tsfs == null|| tsfs.size() == 0){
			throw new AuthorisationException("There are no accessible targetsystem factories for: " +client+
					" Please check your security setup!");
		}
		return tsfs.get(0);
	}

	@Override
	public boolean usesKernelMessaging() {
		return true;
	}
}
