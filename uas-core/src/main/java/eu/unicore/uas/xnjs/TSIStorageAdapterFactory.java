package eu.unicore.uas.xnjs;

import java.io.IOException;

import eu.unicore.uas.impl.BaseResourceImpl;
import eu.unicore.uas.impl.UmaskSupport;
import eu.unicore.uas.impl.sms.SMSBaseImpl;
import eu.unicore.xnjs.io.IStorageAdapter;
import eu.unicore.xnjs.tsi.TSI;

public class TSIStorageAdapterFactory implements StorageAdapterFactory {

	@Override
	public IStorageAdapter createStorageAdapter(BaseResourceImpl parent)throws IOException{
		try{
			TSI tsi = parent.getXNJSFacade().getTSI(parent.getClient());
			if(parent instanceof UmaskSupport) {
				tsi.setUmask(((UmaskSupport)parent).getUmask());
			}
			if(parent instanceof SMSBaseImpl) {
				tsi.setStorageRoot(((SMSBaseImpl)parent).getStorageRoot());
			}
			return tsi;
		}catch(Exception e){
			throw new IOException(e);
		}
	}

}