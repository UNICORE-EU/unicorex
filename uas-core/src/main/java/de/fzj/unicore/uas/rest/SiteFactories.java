package de.fzj.unicore.uas.rest;

import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONObject;

import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.impl.tss.TSFModel;
import de.fzj.unicore.uas.impl.tss.TargetSystemFactoryImpl;
import de.fzj.unicore.uas.json.JSONUtil;
import de.fzj.unicore.wsrflite.security.util.AuthZAttributeStore;
import de.fzj.unicore.wsrflite.utils.UnitParser;
import de.fzj.unicore.xnjs.ems.BudgetInfo;
import de.fzj.unicore.xnjs.idb.ApplicationInfo;
import de.fzj.unicore.xnjs.idb.IDB;
import eu.unicore.security.Client;
import eu.unicore.services.rest.Link;
import eu.unicore.services.rest.USEResource;
import eu.unicore.services.rest.impl.ServicesBase;

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
		List<String> apps = new ArrayList<String>();
		for(ApplicationInfo app: tsf.getXNJSFacade().getDefinedApplications(client)){
			apps.add(app.getName()+IDBContentRendering.appSeparator+app.getVersion());
		}
		props.put("applications", apps);

		Map<String,String> textInfo = new HashMap<String, String>();
		textInfo.putAll(idb.getTextInfoProperties());
		props.put("otherInfo", textInfo);

		List<BudgetInfo> budget = tsf.getXNJSFacade().getComputeTimeBudget(client);
		props.put("remainingComputeTime", IDBContentRendering.budgetToMap(budget));
		
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

}
