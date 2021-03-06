package de.fzj.unicore.uas.rest;

import java.net.URI;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.UASProperties;
import de.fzj.unicore.uas.impl.sms.SMFModel;
import de.fzj.unicore.uas.impl.sms.StorageDescription;
import de.fzj.unicore.uas.impl.sms.StorageFactoryImpl;
import de.fzj.unicore.uas.impl.sms.StorageInfoProvider;
import de.fzj.unicore.uas.json.JSONUtil;
import de.fzj.unicore.uas.util.LogUtil;
import de.fzj.unicore.uas.util.UnitParser;
import eu.unicore.services.rest.USEResource;
import eu.unicore.services.rest.impl.ServicesBase;
import eu.unicore.util.Log;

/**
 * REST interface to storage factories
 *
 * @author schuller
 */
@Path("/storagefactories")
@USEResource(home=UAS.SMF)
public class StorageFactories extends ServicesBase {

	private static final Logger logger=Log.getLogger(LogUtil.SERVICES,StorageFactories.class);

	protected String getResourcesName(){
		return "storagefactories";
	}

	@Override
	protected Map<String,Object>getProperties() throws Exception {
		Map<String,Object> props = super.getProperties();
		props.put("storageDescriptions", getStorageDescriptions());
		return props;
	}

	@Override
	public SMFModel getModel(){
		return (SMFModel)model;
	}

	@Override
	public StorageFactoryImpl getResource(){
		return (StorageFactoryImpl)resource;
	}

	protected Map<String,Object> getStorageDescriptions(){
		Map<String,Object> props = new HashMap<String, Object>();
		Map<String, StorageDescription> factoriesDesc = 
				kernel.getAttribute(UASProperties.class).getStorageFactories();
		for(Map.Entry<String, StorageDescription>e: factoriesDesc.entrySet()){
			props.put(e.getKey(), convert(e.getValue()));
		}
		return props;
	}
	
	protected Map<String,Object> convert(StorageDescription sd){
		Map<String,Object> props = new HashMap<String, Object>();
		props.put("description", sd.getDescription());
		try{
			props.put("parameters", getParameterInfo(sd));
		}catch(Exception ex){
			Log.logException("Error retrieving parameter info", ex, logger);
		}
		return props;
	}
	
	protected Map<String,String> getParameterInfo(StorageDescription sd) throws Exception {
		Class<? extends StorageInfoProvider> infoP=sd.getInfoProviderClass();
		return ((StorageInfoProvider)kernel.load(infoP)).getUserParameterInfo(sd);
	}
	
	/**
	 * create a new storage via the storage factory service
	 * using a JSON description (type, name, parameters, termination time, ...) 
	 * for the new storage
	 */
	@POST
	@Path("/{uniqueID}")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response createSMS(String jsonString) throws Exception {
		try{
			StorageFactoryImpl smf = getResource();
			String id = createSMS(smf, jsonString);
			String location = getBaseURL()+"/storages/"+id;
			return Response.created(new URI(location)).build();
		}catch(Exception ex){
			return handleError("Could not create storage", ex, logger);
		}
	}

	@Override
	protected void updateLinks() {
		super.updateLinks();
	}
	
	public static String createSMS(StorageFactoryImpl smf, String jsonString) throws Exception {
		JSONObject json = new JSONObject(jsonString);
		Calendar tt = null;
		String timeSpec = json.optString("terminationTime",null);
		if(timeSpec!=null){
			tt = Calendar.getInstance();
			tt.setTime(UnitParser.extractDateTime(timeSpec));
		}
		String storageBackendType = json.optString("type", null);
		String name = json.optString("name", null);
		Map<String,String>parameters = JSONUtil.asMap(json.optJSONObject("parameters"));
		return smf.createSMS(storageBackendType, name, tt, parameters);
	}
}
