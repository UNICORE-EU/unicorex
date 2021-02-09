package de.fzj.unicore.uas.jclouds.s3;

import java.util.Map;

import de.fzj.unicore.uas.impl.sms.SMSBaseImpl;
import de.fzj.unicore.uas.impl.sms.StorageDescription;
import de.fzj.unicore.uas.impl.sms.StorageInitParameters;
import de.fzj.unicore.wsrflite.InitParameters;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.io.IStorageAdapter;
import eu.unicore.security.XACMLAttribute;

public class S3StorageImpl extends SMSBaseImpl {

	public static final String S3_ATTRIBUTE_PREFIX = "s3.";
	
	@Override
	public void initialise(InitParameters initobjs)
			throws Exception {
		if(model == null){
			model = new S3Model();
		}
		S3Model model = getModel();
		StorageInitParameters init = (StorageInitParameters)initobjs;
		StorageDescription sd = init.storageDescription;
		sd.setEnableTrigger(false);
		sd.setDisableMetadata(true);
		
		Map<String,String> props = sd.getAdditionalProperties();
		
		boolean allowUserDefinedEndpoint = Boolean.parseBoolean(getSetting("allowUserDefinedEndpoint", props));
		if(!allowUserDefinedEndpoint){
			if(init.userParameters.get("endpoint")!=null || init.userParameters.get("provider")!=null){
				throw new IllegalArgumentException("You are not allowed to set S3 endpoint/provider.");
			}
		}
		
		props.putAll(init.userParameters);
		model.setAccessKey(getSetting("accessKey",props));
		model.setSecretKey(getSetting("secretKey",props));
		model.setEndpoint(getSetting("endpoint",props));
		model.setProvider(getSetting("provider",props));
		
		super.initialise(initobjs);

		String workdir = sd.getPathSpec();
		if(workdir==null)workdir="/";
		if(!workdir.endsWith(getSeparator()))workdir+=getSeparator();
		if(init.appendUniqueID){
			workdir=workdir+getUniqueID();
		}
		model.setWorkdir(workdir);
	}

	public S3Model getModel(){
		return (S3Model)model;
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
	protected S3StorageAdapterFactory getStorageAdapterFactory() {
		return new S3StorageAdapterFactory();
	}

	@Override
	protected String getSeparator(){
		return "/";
	}
	

	// retrieve a value from the current user context, if not set,
	// use the one from the incoming properties
	public String getSetting(String key, Map<String,String> props){
		String res = getUserAttributeValue(S3_ATTRIBUTE_PREFIX, key);
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
