package eu.unicore.uas.trigger.xnjs;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;

import eu.unicore.security.Client;
import eu.unicore.security.Xlogin;
import eu.unicore.uas.trigger.RuleSet;
import eu.unicore.uas.trigger.impl.RuleFactory;
import eu.unicore.uas.trigger.impl.TriggerRunner;
import eu.unicore.uas.trigger.impl.TriggerStatistics;
import eu.unicore.uas.util.LogUtil;
import eu.unicore.util.Log;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.ems.ActionStatus;
import eu.unicore.xnjs.ems.ExecutionException;
import eu.unicore.xnjs.io.IStorageAdapter;
import eu.unicore.xnjs.io.XnjsFile;

/**
 * handles periodic directory scans on a shared storage, and invokes 
 * trigger processing if applicable.
 * 
 * @author schuller
 */
public class SharedTriggerProcessor extends TriggerProcessor {

	private static final Logger logger = LogUtil.getLogger(LogUtil.TRIGGER, SharedTriggerProcessor.class);

	public static final String actionType="SHARED_DIRECTORY_SCAN";

	public SharedTriggerProcessor(XNJS xnjs) {
		super(xnjs);
	}

	/**
	 * scans the base directory for applicable files and initiates a trigger run 
	 */
	@Override
	protected void handleRunning() throws ExecutionException {
		try{
			long thisRun = System.currentTimeMillis();
			ScanSettings sad = updateSettings();
			if(sad.enabled){
				try{
					List<XnjsFile>directories = findDirectories(sad, sad.baseDirectory, action.getClient());
					for(XnjsFile dir: directories){
						processDirectory(dir, sad.storageUID);
					}
					updateLastRunTime(thisRun-1000*sad.gracePeriod);		
				}catch(Exception ex){
					Log.logException("Error setting up trigger run", ex, logger);
				}
			}
			if(sad.updateInterval>0){
				sleep(sad.updateInterval, TimeUnit.SECONDS);
			}
			else{
				action.setStatus(ActionStatus.DONE);
				action.addLogTrace("Status set to DONE.");
			}
		}catch(Exception ex){
			throw ExecutionException.wrapped(ex);
		}
	}
	
	protected void processDirectory(XnjsFile directory, String storageUID){
		try{
			String dir = directory.getPath();
			Client client = new Client();
			Xlogin xlogin = new Xlogin(new String[]{directory.getOwner()});
			client.setXlogin(xlogin);
			logger.debug("Running trigger on <{}> using uid <{}>", directory, directory.getOwner());	
			IStorageAdapter storage = getStorageAdapter(client);
			RuleFactory rf=new RuleFactory(storage, storageUID);
			ScanSettings settings=rf.parseSettings(dir);
			if(settings == null){
				logger.debug("No trigger settings for <{}>", dir);
				return;
			}
			RuleSet rules = rf.getRules(dir);
			List<XnjsFile> files=findFiles(settings, dir,client);
			Set<String> ids = getSubmittedActionIDs();
			if(files.size()>0){
				TriggerRunner tr=new TriggerRunner(files, rules, storage, client, xnjs, TriggerProcessor.logDirectory);
				logger.debug("Executing trigger run on <{}> files.", files.size());	
				TriggerStatistics ts = tr.call();
				ids.addAll(ts.getActionsLaunched());
			}
			updateActionIDs(ids);
		}catch(Exception ex){
			Log.logException("Error running trigger on <"+directory.getPath()+">", ex, logger);
		}
	}

	/**
	 * @param settings
	 * @param baseDir - current base relative to the storage root
	 * @param client
	 * @return array of directories
	 * @throws Exception
	 */
	protected List<XnjsFile> findDirectories(ScanSettings settings, String baseDir, 
			Client client)throws Exception {
		List<XnjsFile>result = new ArrayList<>();
		IStorageAdapter storage = getStorageAdapter(client);
		XnjsFile[]xFiles=storage.ls(baseDir);
		for(XnjsFile xf: xFiles){
			if(xf.isDirectory() && matches(xf.getPath(),settings)){
				result.add(xf);
			}
		}
		return result;
	}

}
