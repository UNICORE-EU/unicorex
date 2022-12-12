package de.fzj.unicore.uas.xnjs;

import java.io.IOException;

import de.fzj.unicore.uas.impl.BaseResourceImpl;
import de.fzj.unicore.xnjs.io.IStorageAdapter;

public class TSIStorageAdapterFactory implements StorageAdapterFactory {

	public IStorageAdapter createStorageAdapter(BaseResourceImpl parent)throws IOException{
		try{
			return parent.getXNJSFacade().getTSI(parent.getClient());
		}catch(Exception e){
			throw new IOException(e);
		}
	}

}
