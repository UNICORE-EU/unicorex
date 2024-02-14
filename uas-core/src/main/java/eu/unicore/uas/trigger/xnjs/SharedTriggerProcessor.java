package eu.unicore.uas.trigger.xnjs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Logger;

import eu.unicore.security.Client;
import eu.unicore.security.Xlogin;
import eu.unicore.services.Home;
import eu.unicore.services.Kernel;
import eu.unicore.services.security.util.AuthZAttributeStore;
import eu.unicore.uas.UAS;
import eu.unicore.uas.impl.sms.SMSBaseImpl;
import eu.unicore.uas.trigger.RuleSet;
import eu.unicore.uas.trigger.impl.RuleFactory;
import eu.unicore.uas.trigger.impl.TriggerRunner;
import eu.unicore.uas.trigger.impl.TriggerStatistics;
import eu.unicore.uas.util.LogUtil;
import eu.unicore.util.Log;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.ems.ActionStatus;
import eu.unicore.xnjs.ems.ProcessingException;
import eu.unicore.xnjs.ems.processors.DefaultProcessor;
import eu.unicore.xnjs.io.IStorageAdapter;
import eu.unicore.xnjs.io.XnjsFile;

/**
 * handles periodic directory scans on a shared storage, and invokes 
 * trigger processing if applicable.
 * 
 * @author schuller
 */
public class SharedTriggerProcessor extends DefaultProcessor {

	private static final Logger logger = LogUtil.getLogger(LogUtil.TRIGGER, SharedTriggerProcessor.class);

	public static final String actionType="SHARED_DIRECTORY_SCAN";

	private IStorageAdapter storage;

	public SharedTriggerProcessor(XNJS xnjs) {
		super(xnjs);
	}

	@Override
	protected void handleCreated(){
		ScanSettings sad=getJob();
		int update=sad.updateInterval;
		if(update>0)sleep(update, TimeUnit.SECONDS);
		action.addLogTrace("Created with update interval <"+update+">.");
		action.setStatus(ActionStatus.RUNNING);
		action.addLogTrace("Status set to RUNNING.");
	}

	/**
	 * scans the base directory for applicable files and initiates a trigger run 
	 */
	@Override
	protected void handleRunning() throws ProcessingException{
		try{
			long thisRun = System.currentTimeMillis();
			ScanSettings sad=getJob();
			RuleFactory rf=new RuleFactory(getStorageAdapter(action.getClient()), sad.storageUID);
			// check if we must update our settings
			ScanSettings newSettings=rf.parseSettings(sad.baseDirectory);
			if(newSettings!=null){
				sad.includes=newSettings.includes;
				sad.excludes=newSettings.excludes;
				sad.enabled=newSettings.enabled;
				if(sad.updateInterval!=newSettings.updateInterval){
					sad.updateInterval=newSettings.updateInterval;
					logger.debug("Update interval for directory scan <{}> changed to {}",
							action.getUUID(), sad.updateInterval);
				}
				action.setDirty();
			}
			if(sad.enabled){
				try{
					XnjsFile[]directories=findDirectories(sad,sad.baseDirectory,action.getClient());
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
			throw new ProcessingException(ex);
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
			RuleSet rules=rf.getRules(dir);
			XnjsFile[]files=findFiles(settings, dir,client);
			Set<String> ids = getSubmittedActionIDs();
			if(files.length>0){
				TriggerRunner tr=new TriggerRunner(files, rules, storage, client, xnjs, TriggerProcessor.logDirectory);
				logger.debug("Executing trigger run on <{}> files.", files.length);	
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
	 * @param depth - current depth relative to the start of the scan
	 * @param client
	 * @return array of files to process
	 * @throws Exception
	 */
	protected XnjsFile[] findFiles(ScanSettings settings, String baseDir, 
			int depth, Client client)throws Exception{
		if(settings.maxDepth<=depth)return new XnjsFile[0];

		long lastRun = getLastRun();
		long graceMillis = 500*settings.gracePeriod;
		
		List<XnjsFile>files = new ArrayList<>();
		
		boolean includeCurrentDir=matches(baseDir,settings);
		logger.debug("Include files in {}: {}", baseDir, includeCurrentDir);
		IStorageAdapter storage=getStorageAdapter(client);
		XnjsFile[]xFiles=storage.ls(baseDir);
		for(XnjsFile xf: xFiles){
			if(xf.isDirectory() && matches(xf.getPath(),settings)){
				files.addAll(Arrays.asList(findFiles(settings, xf.getPath(),depth+1,client)));
			}
			else{
				// files in the directory are included if
				// - the current dir is included in the match
				// - the file is newer than our last run?
				// - the file has not been modified for a certain grace period
				long lastMod=xf.getLastModified().getTimeInMillis();
				if(includeCurrentDir 
						&& lastMod >= lastRun 
						&& lastMod+graceMillis < System.currentTimeMillis())
				{
					logger.debug("Adding: {}", xf.getPath());
					files.add(xf);
				}
				else{
					if(includeCurrentDir){
						logger.debug("Skipping : {} lastModified {}", xf.getPath(), lastMod);
					}
				}
			}
		}

		return files.toArray(new XnjsFile[files.size()]);
	}
	
	protected XnjsFile[] findFiles(ScanSettings settings, String baseDir, Client client)throws Exception{
		return findFiles(settings, baseDir, 0, client);
	}

	// TODO limit on files
	/**
	 * @param settings
	 * @param baseDir - current base relative to the storage root
	 * @param client
	 * @return array of directories
	 * @throws Exception
	 */
	protected XnjsFile[] findDirectories(ScanSettings settings, String baseDir, 
			Client client)throws Exception {
		List<XnjsFile>result = new ArrayList<>();
		IStorageAdapter storage=getStorageAdapter(client);
		XnjsFile[]xFiles=storage.ls(baseDir);
		for(XnjsFile xf: xFiles){
			if(xf.isDirectory() && matches(xf.getPath(),settings)){
				result.add(xf);
			}
		}
		return result.toArray(new XnjsFile[result.size()]);
	}

	protected long getLastRun(){
		Long l=(Long)action.getProcessingContext().get(TriggerProcessor.LAST_RUN_TIME);
		return l!=null? l.longValue() : 0;
	}


	protected void updateLastRunTime(long time){
		action.getProcessingContext().put(TriggerProcessor.LAST_RUN_TIME, time);
	}

	protected void updateActionIDs(Set<String> ids){
		for(Iterator<String> i= ids.iterator(); i.hasNext(); ) {
			try{ 
				if(manager.isActionDone(i.next()))i.remove();
			}catch(Exception ex) {
				i.remove();
			}
		}
		action.getProcessingContext().put(TriggerProcessor.ACTION_IDS, ids);
	}

	@SuppressWarnings("unchecked")
	protected Set<String> getSubmittedActionIDs(){
		Set<String> ids = (Set<String>)action.getProcessingContext().getAs(TriggerProcessor.ACTION_IDS, Set.class);
		return ids!=null? ids : new HashSet<>();
	}

	protected IStorageAdapter getStorageAdapter(Client client) throws Exception{
		if(storage==null){
			String smsID = getJob().storageUID;
			// this seems to be a nasty way to set the correct client,
			// but as XNJS worker threads do not rely on thread-local Client
			// it is no problem. To be sure, we store the client and reset it
			Client oldClient = AuthZAttributeStore.getClient();
			try{
				AuthZAttributeStore.setClient(client);
				Home h=getKernel().getHome(UAS.SMS);
				SMSBaseImpl sms = (SMSBaseImpl)h.get(smsID);
				storage = sms.getStorageAdapter();
			}finally{
				AuthZAttributeStore.setClient(oldClient);
			}
		}
		return storage;
	}

	protected Kernel getKernel(){
		return xnjs.get(Kernel.class);
	}

	protected ScanSettings getJob(){
		return (ScanSettings)action.getAjd();
	}

	/**
	 * return true if the given path should be included in the scan
	 * @param path - the path to check
	 * @param fileSet
	 */
	protected boolean matches(String path, ScanSettings fileSet){
		boolean included=isIncluded(path, fileSet);
		boolean excluded=isExcluded(path, fileSet);
		return included && !excluded;
	}

	protected boolean isIncluded(String path, ScanSettings fileSet){
		boolean res=false;
		// if includes are given, check if it is in the includes
		if(fileSet.includes!=null && fileSet.includes.length>0){
			for(String include: fileSet.includes){
				res=res || match(path,include);
			}
		}
		//else nothing is included
		return res;
	}

	protected boolean isExcluded(String path, ScanSettings fileSet){
		if(fileSet.excludes!=null && fileSet.excludes.length>0){
			for(String exclude: fileSet.excludes){
				if(match(path,exclude))return true;
			}
		}
		return false; 
	}

	private boolean match(String path, String expr){
		Pattern p = getPattern(expr);
		boolean res=p.matcher(path).find();
		return res;
	}

	private Map<String, Pattern>patterns=new HashMap<>();

	private Pattern getPattern(String expr){
		Pattern p=patterns.get(expr);
		if(p==null){
			p=Pattern.compile(expr);
			patterns.put(expr, p);
		}
		return p;
	}

}
