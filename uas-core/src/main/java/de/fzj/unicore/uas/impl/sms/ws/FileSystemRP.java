package de.fzj.unicore.uas.impl.sms.ws;

import org.ggf.schemas.jsdl.x2005.x11.jsdl.FileSystemDocument;

import de.fzj.unicore.uas.impl.sms.DefaultStorageInfoProvider;
import de.fzj.unicore.uas.impl.sms.SMSBaseImpl;
import de.fzj.unicore.uas.impl.sms.StorageDescription;
import de.fzj.unicore.uas.impl.sms.StorageInfoProvider;
import de.fzj.unicore.wsrflite.impl.ResourceImpl;
import de.fzj.unicore.wsrflite.xmlbeans.renderers.ValueRenderer;

public class FileSystemRP extends ValueRenderer{
	
	public FileSystemRP(ResourceImpl parent){
		super(parent, FileSystemDocument.type.getDocumentElementName());
	}
	
	@Override
	protected FileSystemDocument getValue() throws Exception {
		SMSBaseImpl sms=(SMSBaseImpl)parent;
		StorageDescription desc = sms.getModel().getStorageDescription();
		Class<? extends StorageInfoProvider>spClazz = desc.getInfoProviderClass();
		// for backwards compatibility: class may be null
		if(spClazz == null)spClazz = DefaultStorageInfoProvider.class;
		StorageInfoProvider sp = parent.getKernel().load(spClazz);
		FileSystemDocument fs = sp.getInformation(desc, sms.getClient(), sms.getStorageAdapter());
		if(fs.getFileSystem()!=null){
			if(fs.getFileSystem().getName()==null){
				fs.getFileSystem().setName(sms.getModel().getFsname());
			}
			if(fs.getFileSystem().getMountPoint()==null){
				fs.getFileSystem().setMountPoint(sms.getModel().getWorkdir());
			}
		}
		return fs;
	}

}
