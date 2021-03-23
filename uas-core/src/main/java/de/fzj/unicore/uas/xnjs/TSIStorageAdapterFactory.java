package de.fzj.unicore.uas.xnjs;

import java.io.IOException;

import de.fzj.unicore.uas.impl.BaseResourceImpl;
import de.fzj.unicore.xnjs.io.IStorageAdapter;
import eu.unicore.security.Client;

public class TSIStorageAdapterFactory implements StorageAdapterFactory {

	public IStorageAdapter createStorageAdapter(BaseResourceImpl parent)throws IOException{
		try{
			Client client=parent.getClient();
			return parent.getXNJSFacade().getTargetSystemInterface(client);
		}catch(Exception e){
			throw new IOException(e);
		}
	}

}
