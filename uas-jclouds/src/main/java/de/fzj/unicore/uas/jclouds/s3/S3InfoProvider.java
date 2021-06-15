package de.fzj.unicore.uas.jclouds.s3;

import java.util.Map;

import org.ggf.schemas.jsdl.x2005.x11.jsdl.FileSystemDocument;

import de.fzj.unicore.uas.impl.sms.DefaultStorageInfoProvider;
import de.fzj.unicore.uas.impl.sms.StorageDescription;
import de.fzj.unicore.xnjs.io.IStorageAdapter;
import eu.unicore.security.Client;
import eu.unicore.services.Kernel;

public class S3InfoProvider extends DefaultStorageInfoProvider {

	public S3InfoProvider(Kernel kernel){
		super(kernel);
	}
	
	@Override
	public FileSystemDocument getInformation(StorageDescription storageDesc,
			Client client, IStorageAdapter storage) {
		FileSystemDocument fsd = FileSystemDocument.Factory.newInstance();
		fsd.addNewFileSystem();
		fsd.getFileSystem().setDescription(storageDesc.getDescription());
		if(storage!=null)fsd.getFileSystem().setDescription(storage.getFileSystemIdentifier());
		fsd.getFileSystem().setName(storageDesc.getName());
		return fsd;
	}

	@Override
	public Map<String,String> getUserParameterInfo(StorageDescription storageDesc){
		Map<String,String> res = super.getUserParameterInfo(storageDesc);
		res.put("accessKey", "access key");
		res.put("secretKey", "secret key");
		res.put("endpoint", "S3 endpoint to access");
		res.put("provider", "JClouds provider to use");
		return res;
	}
	
}
