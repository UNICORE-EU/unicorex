package eu.unicore.uas.cdmi;

import java.util.Map;

import eu.unicore.uas.impl.sms.DefaultStorageInfoProvider;
import eu.unicore.uas.impl.sms.StorageDescription;
import eu.unicore.services.Kernel;

public class CDMIInfoProvider extends DefaultStorageInfoProvider {

	
	public CDMIInfoProvider(Kernel kernel) {
		super(kernel);
	}

	@Override
	public Map<String,String> getUserParameterInfo(StorageDescription storageDesc){
		Map<String,String> res = super.getUserParameterInfo(storageDesc);
		res.put("username", "user name");
		res.put("password", "password");
		res.put("endpoint", "CDMI endpoint to access");
		res.put("keystoneEndpoint", "Keystone auth endpoint (if unset, basic auth will be used)");
		return res;
	}
	
}
