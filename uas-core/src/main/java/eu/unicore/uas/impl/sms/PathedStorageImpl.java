package eu.unicore.uas.impl.sms;

import java.io.File;

import org.apache.logging.log4j.Logger;

import eu.unicore.services.InitParameters;
import eu.unicore.uas.util.LogUtil;
import eu.unicore.xnjs.ems.ExecutionException;
import eu.unicore.xnjs.io.XnjsFileWithACL;
import eu.unicore.xnjs.tsi.TSI;

/**
 * a storage where the path is resolved once per request. This allows
 * having environment variables in the path specification, for
 * example /work/$PROJECT/files
 * 
 * @author schuller
 */
public class PathedStorageImpl extends SMSBaseImpl {

	private static final Logger logger = LogUtil.getLogger(LogUtil.DATA, PathedStorageImpl.class);

	protected String storageRoot;

	@Override
	public void activate(){
		super.activate();
		// make sure the storageRoot is resolved once per request
		storageRoot=null;
	}

	@Override
	public synchronized String getStorageRoot() {
		if(storageRoot==null){
			try{
				resolveRootDir();
			}
			catch(ExecutionException e){
				String workdir = getModel().workdir;
				LogUtil.logException("Could not resolve location: "+workdir,e,logger);
				return workdir;
			}
		}
		return storageRoot;
	}

	private void resolveRootDir()throws ExecutionException{
		TSI tsi=getXNJSFacade().getTSI(getClient(),null);
		storageRoot=tsi.resolve(getModel().workdir);
		if(tsi.isLocal()){
			//make sure it is absolute
			storageRoot=new File(storageRoot).getAbsolutePath();
		}
		//check if the resolved dir is just "/", and fail in this case
		if(getSeparator().equals(storageRoot)){
			throw new ExecutionException("Variable path was resolved to '/', this is not allowed");
		}
	}

	@Override
	public void initialise(InitParameters initobjs) throws Exception {
		super.initialise(initobjs);
		SMSModel m = getModel();
		StorageInitParameters init = (StorageInitParameters)initobjs;
		String workdir=m.storageDescription.getPathSpec();
		if(workdir==null) {
			workdir = getDefaultWorkdir();
		}
		if(!workdir.endsWith(getSeparator()))workdir+=getSeparator();
		if(init.appendUniqueID){
			workdir=workdir+getUniqueID()+getSeparator();
		}
		m.workdir = workdir;
		//try to resolve once to detect problems early
		if(!init.skipResolve){
			resolveRootDir();
			if(init.appendUniqueID){
				String parent = new File(storageRoot).getParent();
				createParent(parent);
			}
		}
		if(storageRoot!=null){
			logger.info("Storage init in <{}>, resolved as <{}>", workdir, storageRoot);
			if(m.storageDescription.isCheckExistence()){
				checkDirExists();
			}
		}
		else{
			logger.info("Storage init in <{}>", workdir);
		}
	}

	protected String getDefaultWorkdir() {
		throw new IllegalArgumentException("Work directory cannot be null.");
	}

	private void createParent(String dir) throws ExecutionException {
		TSI tsi=getXNJSFacade().getTSI(getClient(),null);
		tsi.setUmask("0002");
		tsi.setStorageRoot(dir);
		tsi.mkdir("/");
	}

	private void checkDirExists()throws ExecutionException{
		TSI tsi=getXNJSFacade().getTSI(getClient(),null);
		//some sanity checks
		XnjsFileWithACL xnjsFile=tsi.getProperties(storageRoot);
		if(xnjsFile==null){
			throw new ExecutionException("Directory '"+storageRoot+"' does not exist.");
		}
		if(!xnjsFile.isDirectory()){
			throw new ExecutionException("Path '"+storageRoot+"' is not a directory.");
		}
	}
}
