package eu.unicore.uas.jclouds.s3;

import java.util.Map;

import eu.unicore.uas.impl.sms.DefaultStorageInfoProvider;
import eu.unicore.uas.impl.sms.StorageDescription;
import eu.unicore.services.Kernel;

public class S3InfoProvider extends DefaultStorageInfoProvider {

	public S3InfoProvider(Kernel kernel){
		super(kernel);
	}

	@Override
	public Map<String,String> getUserParameterInfo(StorageDescription storageDesc){
		Map<String,String> res = super.getUserParameterInfo(storageDesc);
		res.put("accessKey", "access key");
		res.put("secretKey", "secret key");
		res.put("endpoint", "S3 endpoint to access");
		res.put("provider", "JClouds provider to use");
		res.put("validate", "Validate s3 server cert");
		return res;
	}
}
