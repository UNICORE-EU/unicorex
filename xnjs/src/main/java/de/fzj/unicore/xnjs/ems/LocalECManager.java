package de.fzj.unicore.xnjs.ems;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.xnjs.XNJSProperties;
import de.fzj.unicore.xnjs.tsi.BatchMode;
import de.fzj.unicore.xnjs.tsi.TSI;
import de.fzj.unicore.xnjs.tsi.TSIFactory;
import de.fzj.unicore.xnjs.tsi.remote.RemoteTSI;
import de.fzj.unicore.xnjs.util.LogUtil;

/**
 * default IExecutionContextManager implementation
 * 
 * @author schuller
 */
@Singleton
public class LocalECManager implements IExecutionContextManager {
	
	private static final Logger logger=LogUtil.getLogger(LogUtil.JOBS,LocalECManager.class);
	
	private final TSIFactory tsiFactory;
	
	private final XNJSProperties properties;
	
	@Inject
	public LocalECManager(TSIFactory tsiFactory, XNJSProperties properties){
		this.tsiFactory = tsiFactory;
		this.properties = properties;
	}

	@Override
	public ExecutionContext getContext(Action action) throws ExecutionException{
		ExecutionContext ec = action.getExecutionContext();
		if(ec==null){
			TSI targetSystem = tsiFactory.createTSI(action.getClient());
			String uspaces = properties.getValue(XNJSProperties.FILESPACE);
			String baseDirectory = targetSystem.resolve(uspaces);
			if(targetSystem.isLocal()){
				baseDirectory = new File(baseDirectory).getAbsolutePath();
			}
			targetSystem.setStorageRoot(baseDirectory);
			
			if(targetSystem instanceof BatchMode) {
				((BatchMode)targetSystem).startBatch();
			}
			// create base first with configured umask
			String baseUmask = properties.getValue(XNJSProperties.FILESPACE_UMASK);
			targetSystem.setUmask(baseUmask);
			targetSystem.mkdir("/");
			
			String wd = action.getUUID();
			String uspace = baseDirectory+targetSystem.getFileSeparator()+wd+targetSystem.getFileSeparator();
			if(targetSystem.getProperties(wd)==null){
				logger.info("Creating {}", uspaceInfo(action));
				targetSystem.setUmask(action.getUmask());
				targetSystem.mkdir(wd);
			}
			else{
				logger.info("Re-connecting to {}", uspaceInfo(action));
			}
			if(targetSystem instanceof BatchMode) {
				String res = ((BatchMode)targetSystem).commitBatch();
				if (res!=null) {
					res = res.replaceFirst("TSI_OK", "").trim().replace("\n", " - ");
				}
				if(targetSystem instanceof RemoteTSI) {
					RemoteTSI rTSI = (RemoteTSI)targetSystem;
					String node = rTSI.getLastUsedTSIHost();
					rTSI.assertIsDirectory(wd,
							"Could not create job working directory <%s> on TSI <%s>! TSI reply: %s", uspace, node, res);
				}
			}
			ec = new ExecutionContext();
			initContext(ec, uspace, false, null, action.getUmask());
			action.setExecutionContext(ec);
		}
		return ec;
	}
	

	private void initContext(ExecutionContext ec, String wd, boolean isChild, String childUID, String umask){
		ec.setWorkingDirectory(wd);
		if(umask==null) {
			umask = properties.getValue(XNJSProperties.DEFAULT_UMASK);
		}
		ec.setUmask(umask);
		//set some default names for the out/err files
		if(isChild){
			ec.setStdout("stdout-"+childUID); 
			ec.setStderr("stderr-"+childUID);
		}
	}
	 
	@Override
	public ExecutionContext createChildContext(Action parentAction, Action childAction) throws ExecutionException {
		ExecutionContext pc=getContext(parentAction);
		if(pc==null) throw new IllegalStateException("Cannot create child context, parent context does not exist");
		ExecutionContext childEc = new ExecutionContext();
		String wd=pc.getWorkingDirectory();
		String cwd=wd;
		if(parentAction.getApplicationInfo()!=null){
			//copy environment
			childEc.getEnvironment().putAll(parentAction.getApplicationInfo().getEnvironment());
		}
		initContext(childEc, cwd, true, childAction.getUUID(), parentAction.getUmask());
		childAction.setExecutionContext(childEc);
		return childEc;
	}
	
	@Override
	public void destroyUSpace(Action action) throws ExecutionException{
		logger.info("Destroying {}", uspaceInfo(action));
		TSI targetSystem = tsiFactory.createTSI(action.getClient());
		try{
			String wd=action.getExecutionContext().getWorkingDirectory();
			targetSystem.rmdir(wd);
		}catch(Exception e){
			throw new ExecutionException(e);
		}
	}
	
	/**
	 * create a working directory for the given action, if it does not yet exist. 
	 * The working directory is created in the given base directory, and is named
	 * using the action's uuid.
	 * 
	 * @param action - the action
	 * @param baseDirectory - the base directory
	 * @throws ExecutionException
	 */
	@Override
	public String createUSpace(Action action, String baseDirectory) throws ExecutionException{
		TSI targetSystem = tsiFactory.createTSI(action.getClient());
		if(targetSystem.isLocal()){
			baseDirectory=new File(baseDirectory).getAbsolutePath();
		}
		targetSystem.setStorageRoot(baseDirectory);
		String wd=action.getUUID();
		String uspace=baseDirectory+targetSystem.getFileSeparator()+wd+targetSystem.getFileSeparator();
		if(targetSystem.getProperties(wd)==null){
			logger.info("Creating "+uspaceInfo(action));
			targetSystem.setUmask(action.getUmask());
			targetSystem.mkdir(wd);
		}
		else{
			logger.info("Re-connecting to "+uspaceInfo(action));
		}
		return uspace;
	}

	private String uspaceInfo(Action action){
		return "uspace <"+action.getUUID()+"> for <" 
				+(action.getClient()!=null?action.getClient().getDistinguishedName():"n/a")+">";
	}
}
