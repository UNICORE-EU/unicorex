package eu.unicore.uas.jclouds.s3;

import java.util.Map;

import eu.unicore.uas.SMSProperties;
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
		res.remove(SMSProperties.ENABLE_TRIGGER);
		res.remove(SMSProperties.DISABLE_METADATA);
		res.put("accessKey", "access key");
		res.put("secretKey", "secret key");
		res.put("bucket", "bucket to access (or create)");
		res.put("validate", "Validate s3 server cert");
		boolean allowUser = Boolean.parseBoolean(storageDesc.getAdditionalProperties()
				.get("allowUserDefinedEndpoint"));
		if(allowUser){
			res.put("endpoint", "S3 endpoint to access");
			res.put("region", "S3/AWS region identifier");
			res.put("provider", "JClouds provider to use");
		}
		return res;
	}
}
