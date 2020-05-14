package de.fzj.unicore.uas.jclouds.swift;

import java.util.Map;

import de.fzj.unicore.uas.impl.sms.SMSBaseImpl;
import de.fzj.unicore.uas.impl.sms.StorageDescription;
import de.fzj.unicore.uas.impl.sms.StorageInitParameters;
import de.fzj.unicore.wsrflite.InitParameters;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.io.IStorageAdapter;
import eu.unicore.security.XACMLAttribute;

public class SwiftStorageImpl extends SMSBaseImpl {

	public static final String SWIFT_ATTRIBUTE_PREFIX = "swift.";
	
	@Override
	public void initialise(InitParameters initobjs)
			throws Exception {
		if(model == null){
			model = new SwiftModel();
		}
		SwiftModel model = getModel();
		StorageInitParameters init = (StorageInitParameters)initobjs;
		StorageDescription sd = init.storageDescription;
		sd.setEnableTrigger(false);
		sd.setDisableMetadata(true);
		
		Map<String,String> props = sd.getAdditionalProperties();
		
		boolean allowUserDefinedEndpoint = Boolean.parseBoolean(getSetting("allowUserdefinedEndpoint", props));
		if(!allowUserDefinedEndpoint){
			if(init.userParameters.get("endpoint")!=null){
				throw new IllegalArgumentException("You are not allowed to set the Swift endpoint.");
			}
		}
		
		props.putAll(init.userParameters);
		model.setUsername(getSetting("username",props));
		model.setPassword(getSetting("password",props));
		model.setEndpoint(getSetting("endpoint",props));
		model.setRegion(getSetting("region",props));
		
		super.initialise(initobjs);

		String workdir = sd.getPathSpec();
		if(workdir==null)workdir="/";
		if(!workdir.endsWith(getSeparator()))workdir+=getSeparator();
		if(init.appendUniqueID){
			workdir=workdir+getUniqueID();
		}
		model.setWorkdir(workdir);
	}

	public SwiftModel getModel(){
		return (SwiftModel)model;
	}
	
	@Override
	protected String getStorageRoot() throws ExecutionException {
		return getModel().getWorkdir();
	}
	
	@Override
	public IStorageAdapter getStorageAdapter()throws Exception{
		IStorageAdapter sms = getStorageAdapterFactory().createStorageAdapter(this);
		sms.setStorageRoot(getStorageRoot());
		return sms;
	}

	@Override
	protected SwiftStorageAdapterFactory getStorageAdapterFactory() {
		return new SwiftStorageAdapterFactory();
	}

	@Override
	protected String getSeparator(){
		return "/";
	}
	

	// retrieve a value from the current user context, if not set,
	// use the one from the incoming properties
	public String getSetting(String key, Map<String,String> props){
		String res = getUserAttributeValue(SWIFT_ATTRIBUTE_PREFIX, key);
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
