package eu.unicore.uas.rest;

import java.net.URI;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import eu.unicore.services.rest.USEResource;
import eu.unicore.services.rest.impl.ServicesBase;
import eu.unicore.services.restclient.utils.UnitParser;
import eu.unicore.uas.UAS;
import eu.unicore.uas.UASProperties;
import eu.unicore.uas.impl.sms.SMFModel;
import eu.unicore.uas.impl.sms.StorageDescription;
import eu.unicore.uas.impl.sms.StorageFactoryImpl;
import eu.unicore.uas.json.JSONUtil;
import eu.unicore.uas.util.LogUtil;
import eu.unicore.util.Log;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

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
		Map<String,Object> props = new HashMap<>();
		Map<String, StorageDescription> factoriesDesc = 
				kernel.getAttribute(UASProperties.class).getStorageFactories();
		for(Map.Entry<String, StorageDescription>e: factoriesDesc.entrySet()){
			props.put(e.getKey(), convert(e.getValue()));
		}
		return props;
	}
	
	protected Map<String,Object> convert(StorageDescription sd){
		Map<String,Object> props = new HashMap<>();
		props.put("description", sd.getDescription());
		try{
			props.put("parameters", getParameterInfo(sd));
		}catch(Exception ex){
			Log.logException("Error retrieving parameter info", ex, logger);
		}
		return props;
	}

	protected Map<String,String> getParameterInfo(StorageDescription sd) throws Exception {
		return kernel.load(sd.getInfoProviderClass()).getUserParameterInfo(sd);
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

	@Override
	public boolean usesKernelMessaging() {
		return true;
	}
}
