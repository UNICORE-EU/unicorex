package eu.unicore.uas.jclouds.swift;

import java.util.Map;

import eu.unicore.uas.impl.sms.DefaultStorageInfoProvider;
import eu.unicore.uas.impl.sms.StorageDescription;
import eu.unicore.services.Kernel;

public class SwiftInfoProvider extends DefaultStorageInfoProvider {

	public SwiftInfoProvider(Kernel kernel){
		super(kernel);
	}

	@Override
	public Map<String,String> getUserParameterInfo(StorageDescription storageDesc){
		Map<String,String> res = super.getUserParameterInfo(storageDesc);
		res.put("username", "Openstack user name");
		res.put("password", "Openstack password");
		res.put("endpoint", "Swift endpoint to access");
		res.put("provider", "JClouds provider to use");
		return res;
	}
}
