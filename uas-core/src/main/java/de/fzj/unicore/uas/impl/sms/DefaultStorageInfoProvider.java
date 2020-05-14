package de.fzj.unicore.uas.impl.sms;

import java.util.HashMap;
import java.util.Map;

import org.ggf.schemas.jsdl.x2005.x11.jsdl.FileSystemDocument;

import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.xnjs.io.IStorageAdapter;
import de.fzj.unicore.xnjs.io.XnjsStorageInfo;
import eu.unicore.security.Client;

/**
 * provides information about the storage backed by a filesystem
 */
public class DefaultStorageInfoProvider implements StorageInfoProvider {

	protected final Kernel kernel;
	
	public DefaultStorageInfoProvider(Kernel kernel){
		this.kernel = kernel;
	}
	
	@Override
	public FileSystemDocument getInformation(StorageDescription factoryDesc, Client client, IStorageAdapter storage) {
		FileSystemDocument fs=FileSystemDocument.Factory.newInstance();
		fs.addNewFileSystem().setDescription(factoryDesc.getDescription());
		fs.getFileSystem().setName(factoryDesc.getName());
		String workdir=factoryDesc.getPathSpec();
		if(storage!=null){
			// may override workdir...
			workdir = storage.getStorageRoot();
		}
		if (workdir != null && storage!=null) {
			fillStorageInfo(storage, workdir, fs);
		}
		return fs;	
	}
	
	/**
	 * add info to the FileSystemDocument 
	 * @param storage
	 * @param workdir
	 * @param fs
	 */
	public void fillStorageInfo(IStorageAdapter storage, String workdir, FileSystemDocument fs){
		String root=".";
		if(workdir!=null){
			root=workdir;
			fs.getFileSystem().setMountPoint(workdir);	
		}
		XnjsStorageInfo info=storage.getAvailableDiskSpace(root);
		
		if(info.getUsableSpace()>-1){
			fs.getFileSystem().addNewDiskSpace().addNewExact().setDoubleValue(info.getUsableSpace());
		}
		else if(info.getFreeSpace()>-1){
			fs.getFileSystem().addNewDiskSpace().addNewExact().setDoubleValue(info.getFreeSpace());
		}
		fs.getFileSystem().setDescription(storage.getFileSystemIdentifier());
	}
	
	@Override
	public Map<String,String> getUserParameterInfo(StorageDescription storageDescription){
		Map<String,String> params = new HashMap<>();
		if(storageDescription!=null && storageDescription.isAllowUserdefinedPath()){
			params.put("path","Root path of the new storage");
		}
		return params;
	}
}
