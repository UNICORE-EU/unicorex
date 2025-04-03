package eu.unicore.uas.impl.sms;

import java.io.File;

import eu.unicore.services.InitParameters;
import eu.unicore.xnjs.ems.ExecutionException;
import eu.unicore.xnjs.io.XnjsFileWithACL;
import eu.unicore.xnjs.tsi.TSI;

/**
 * A storage serving files from a fixed path, such as "/work" 
 *
 * @author schuller
 */
public class FixedStorageImpl extends SMSBaseImpl {

	@Override
	public void initialise(InitParameters initobjs)throws Exception{
		super.initialise(initobjs);
		SMSModel m = getModel();
		String workdir = m.storageDescription.getPathSpec();
		if(workdir==null)throw new IllegalArgumentException("Work directory cannot be null.");
		TSI tsi=getXNJSFacade().getTSI(getClient(),null);
		if(tsi!=null && tsi.isLocal()){
			workdir=new File(workdir).getAbsolutePath();
		}
		StorageInitParameters init = (StorageInitParameters)initobjs;
		if(!workdir.endsWith(getSeparator()))workdir+=getSeparator();
		if(init.appendUniqueID){
			workdir=workdir+getUniqueID();
		}
		m.workdir=workdir;
		if(m.storageDescription.isCheckExistence() 
				&& !init.skipResolve){
			checkWorkdirExists();
		}
	}

	private void checkWorkdirExists()throws ExecutionException{
		TSI tsi=getXNJSFacade().getTSI(getClient(),null);
		//some sanity checks
		String workdir=getModel().workdir;
		XnjsFileWithACL xnjsFile=tsi.getProperties(workdir);
		if(xnjsFile==null){
			throw new ExecutionException("Directory '"+workdir+"' does not exist.");
		}
		if(!xnjsFile.isDirectory()){
			throw new ExecutionException("Path '"+workdir+"' is not a directory.");
		}
	}

	@Override
	public String getStorageRoot() {
		return getModel().workdir;
	}
}
