package de.fzj.unicore.uas.cdmi;

import java.util.Map;

import org.ggf.schemas.jsdl.x2005.x11.jsdl.FileSystemDocument;

import de.fzj.unicore.uas.impl.sms.DefaultStorageInfoProvider;
import de.fzj.unicore.uas.impl.sms.StorageDescription;
import de.fzj.unicore.xnjs.io.IStorageAdapter;
import eu.unicore.security.Client;
import eu.unicore.services.Kernel;

public class CDMIInfoProvider extends DefaultStorageInfoProvider {

	
	public CDMIInfoProvider(Kernel kernel) {
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
		res.put("username", "user name");
		res.put("password", "password");
		res.put("endpoint", "CDMI endpoint to access");
		res.put("keystoneEndpoint", "Keystone auth endpoint (if unset, basic auth will be used)");
		return res;
	}
	
}
