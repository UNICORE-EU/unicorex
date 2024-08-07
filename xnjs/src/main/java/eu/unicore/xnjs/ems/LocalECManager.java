package eu.unicore.xnjs.ems;

import java.io.File;

import org.apache.logging.log4j.Logger;

import eu.unicore.xnjs.XNJSProperties;
import eu.unicore.xnjs.tsi.BatchMode;
import eu.unicore.xnjs.tsi.TSI;
import eu.unicore.xnjs.tsi.TSIFactory;
import eu.unicore.xnjs.tsi.remote.RemoteTSI;
import eu.unicore.xnjs.util.LogUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

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
	public void initialiseContext(Action action) throws ExecutionException{
		initContext(action.getExecutionContext(), null, false, null, action.getUmask());
	}

	/**
	 * Create a working directory for the given action, if it does not yet exist. 
	 * The working directory is created in the configured location
	 * (@see XNJSProperties.FILESPACE)
	 * the working directory is stored in the actions execution context
	 *
	 * @param action - the action
	 *
	 * @throws ExecutionException
	 */
	public String createUSpace(Action action) throws ExecutionException{
		TSI targetSystem = tsiFactory.createTSI(action.getClient());
		String uspaces = properties.getValue(XNJSProperties.FILESPACE);
		String baseDirectory = targetSystem.resolve(uspaces);
		String sep = targetSystem.getFileSeparator();
		if(targetSystem.isLocal()){
			baseDirectory = new File(baseDirectory).getAbsolutePath();
		}
		if(baseDirectory.endsWith(sep)) {
			baseDirectory = baseDirectory.substring(0, baseDirectory.length()-1);
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
		String uspace = baseDirectory + sep + wd + sep;
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
		action.getExecutionContext().setWorkingDirectory(uspace);
		return uspace;
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
		if(baseDirectory.endsWith(targetSystem.getFileSeparator())) {
			baseDirectory = baseDirectory.substring(0, baseDirectory.length()-1);
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

	@Override
	public void initialiseChildContext(Action parentAction, Action childAction) throws ExecutionException {
		ExecutionContext pc = parentAction.getExecutionContext();
		if(pc==null) throw new IllegalStateException("Cannot create child context, parent context does not exist");
		ExecutionContext childEc = childAction.getExecutionContext();
		String wd=pc.getWorkingDirectory();
		String cwd=wd;
		if(parentAction.getApplicationInfo()!=null){
			//copy environment
			childEc.getEnvironment().putAll(parentAction.getApplicationInfo().getEnvironment());
		}
		initContext(childEc, cwd, true, childAction.getUUID(), parentAction.getUmask());
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

	private String uspaceInfo(Action action){
		return "uspace <"+action.getUUID()+"> for <" 
				+(action.getClient()!=null?action.getClient().getDistinguishedName():"n/a")+">";
	}
}
