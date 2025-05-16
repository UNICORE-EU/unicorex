package eu.unicore.uas.cdmi;

import java.util.Map;

import eu.unicore.uas.impl.sms.SMSBaseImpl;
import eu.unicore.uas.impl.sms.StorageDescription;
import eu.unicore.uas.impl.sms.StorageInitParameters;
import eu.unicore.xnjs.ems.ExecutionException;
import eu.unicore.xnjs.io.IStorageAdapter;
import eu.unicore.security.XACMLAttribute;
import eu.unicore.services.InitParameters;

public class CDMIStorageImpl extends SMSBaseImpl {

	public static final String CDMI_ATTRIBUTE_PREFIX = "cdmi.";

	@Override
	public void initialise(InitParameters initobjs)
			throws Exception {
		if(model == null){
			model = new CDMIModel();
		}
		StorageInitParameters init = (StorageInitParameters)initobjs;
		CDMIModel model = getModel();
		StorageDescription sd = init.storageDescription;
		sd.setEnableTrigger(false);
		sd.setDisableMetadata(true);
		Map<String,String> props = sd.getAdditionalProperties();
		Map<String,String> userSettings = init.userParameters; 
		boolean allowUserDefinedEndpoint = 
				Boolean.parseBoolean(getSetting("allowUserDefinedEndpoint", props));
		if(!allowUserDefinedEndpoint){
			if(userSettings.get("endpoint")!=null)
				throw new IllegalArgumentException("You are not allowed to set the CDMI endpoint.");
		}
		props.putAll(userSettings);
		model.setUsername(getSetting("username",props));
		model.setPassword(getSetting("password",props));
		model.setEndpoint(getSetting("endpoint",props));
		model.setTokenEndpoint(getSetting("tokenEndpoint",props));
		super.initialise(initobjs);
		String workdir = sd.getPathSpec();
		if(workdir==null)workdir="/";
		if(!workdir.endsWith(getSeparator()))workdir+=getSeparator();
		if(init.appendUniqueID){
			workdir=workdir+getUniqueID();
		}
		model.setWorkdir(workdir);
	}

	@Override
	public CDMIModel getModel(){
		return (CDMIModel)model;
	}

	@Override
	public boolean isProtocolAllowed(String protocol) {
		return "BFT".equalsIgnoreCase(protocol);
	}

	@Override
	public String getStorageRoot() throws ExecutionException {
		return getModel().getWorkdir();
	}

	@Override
	public IStorageAdapter getStorageAdapter()throws Exception{
		IStorageAdapter sms = getStorageAdapterFactory().createStorageAdapter(this);
		sms.setStorageRoot(getStorageRoot());
		return sms;
	}

	@Override
	protected CDMIStorageAdapterFactory getStorageAdapterFactory() {
		return new CDMIStorageAdapterFactory();
	}

	@Override
	protected String getSeparator(){
		return "/";
	}

	// retrieve a value from the current user context, if not set,
	// use the one from the incoming properties
	public String getSetting(String key, Map<String,String> props){
		String res = getUserAttributeValue(CDMI_ATTRIBUTE_PREFIX, key);
		return res!=null ? res : props.get(key);
	}

	private String getUserAttributeValue(String prefix,String key){
		if(prefix!=null)key = prefix+key; 
		try{
			for(XACMLAttribute attr: getClient().getSubjectAttributes().getXacmlAttributes()){
				if(key.equals(attr.getName())){
					return attr.getValue();
				}
			}
		}
		catch(Exception e){}
		return null;
	}
}
