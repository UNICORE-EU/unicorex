package eu.unicore.uas.xnjs;

import java.io.IOException;

import eu.unicore.uas.impl.BaseResourceImpl;
import eu.unicore.xnjs.io.IStorageAdapter;

public class TSIStorageAdapterFactory implements StorageAdapterFactory {

	@Override
	public IStorageAdapter createStorageAdapter(BaseResourceImpl parent)throws IOException{
		try{
			return parent.getXNJSFacade().getTSI(parent.getClient());
		}catch(Exception e){
			throw new IOException(e);
		}
	}

}