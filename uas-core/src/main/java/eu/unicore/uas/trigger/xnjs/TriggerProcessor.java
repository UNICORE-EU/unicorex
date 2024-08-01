package eu.unicore.uas.trigger.xnjs;

import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
import eu.unicore.services.Home;
import eu.unicore.services.Kernel;
import eu.unicore.services.exceptions.ResourceUnknownException;
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
import eu.unicore.xnjs.ems.Action;
import eu.unicore.xnjs.ems.ActionStatus;
import eu.unicore.xnjs.ems.ExecutionException;
import eu.unicore.xnjs.ems.processors.DefaultProcessor;
import eu.unicore.xnjs.io.IStorageAdapter;
import eu.unicore.xnjs.io.XnjsFile;

/**
 * handles periodic directory scans and invokes trigger processing if
 * applicable.
 * 
 * @author schuller
 */
public class TriggerProcessor extends DefaultProcessor {

	private static final Logger logger = LogUtil.getLogger(LogUtil.TRIGGER, TriggerProcessor.class);

	public static final String actionType = "DIRECTORY_SCAN";
	public static final String LAST_RUN_TIME = "LAST_RUN_TIME";
	public static final String LAST_RUN_INFO = "LAST_RUN_INFO";
	public static final String ACTION_IDS = "ACTION_IDS";

	public TriggerProcessor(XNJS xnjs) {
		super(xnjs);
	}

	@Override
	protected void handleCreated(){
		ScanSettings sad=getJob();
		int update=sad.updateInterval;
		if(update>0)sleep(update, TimeUnit.SECONDS);
		action.setStatus(ActionStatus.RUNNING);
		updateActionIDs(new HashSet<>());
		action.addLogTrace("Created, status set to RUNNING.");
	}

	/**
	 * scans the base directory for applicable files and initiates a trigger run 
	 */
	@Override
	protected void handleRunning() throws ExecutionException {
		long thisRun = System.currentTimeMillis();
		IStorageAdapter storage = getStorageAdapter(action.getClient());
		List<String>log = new ArrayList<>();
		try{
			ScanSettings sad = updateSettings();
			if(sad.enabled){
				try{
					RuleFactory rf = new RuleFactory(storage, sad.storageUID);
					RuleSet rules = rf.getRules(sad.baseDirectory);
					List<XnjsFile> files = findFiles(sad, sad.baseDirectory, action.getClient());
					Set<String> ids = getSubmittedActionIDs();
					if(files.size()>0){
						TriggerRunner tr = new TriggerRunner(files, rules, storage,
								action.getClient(), xnjs, logDirectory);
						logger.debug("Executing trigger run on <{}> files.", files.size());
						TriggerStatistics ts = tr.call();
						ids.addAll(ts.getActionsLaunched());
						storeLastRunInfo(ts.toString());
					}
					updateLastRunTime(thisRun-1000*sad.gracePeriod);
					updateActionIDs(ids);
				}catch(Exception ex){
					Log.logException("Error setting up trigger run", ex, logger);
					log.add(Log.createFaultMessage("Error setting up trigger run", ex));
				}
			}
			if(sad.updateInterval>0){
				sleep(sad.updateInterval, TimeUnit.SECONDS);
			}
			else{
				sleep(120, TimeUnit.SECONDS);
			}
		}catch(ResourceUnknownException rue){
			// storage is gone for some reason, quit
			setToDoneAndFailed(Log.createFaultMessage("Parent storage does not exist.", rue));
		}catch(Exception ex){
			// do not quit processing, it might be a transient error
			logger.debug("Error during trigger processing on storage {}: {}",
					getJob().storageUID, ex);
			String msg = Log.createFaultMessage("Error", ex);
			log.add(msg);
			storeLastRunInfo(msg);
			sleep(120, TimeUnit.SECONDS);
		}
		try {
			storeLog(log, logDirectory, storage);
		}catch(Exception ex) {
			logger.debug("Cannot write error log for trigger run on storage {}: {}",
					getJob().storageUID, ex);
		}
	}

	protected ScanSettings updateSettings() throws Exception {
		ScanSettings sad = getJob();
		RuleFactory rf = new RuleFactory(getStorageAdapter(action.getClient()), sad.storageUID);
		ScanSettings newSettings = rf.parseSettings(sad.baseDirectory);
		if(newSettings!=null){
			sad.includes = newSettings.includes;
			sad.excludes = newSettings.excludes;
			sad.enabled = newSettings.enabled;
			if(sad.updateInterval!=newSettings.updateInterval){
				sad.updateInterval=newSettings.updateInterval;
				logger.debug("Update interval for directory scan <{}> changed to {}",
						action.getUUID(), sad.updateInterval);
			}
			action.setDirty();
		}
		return sad;
	}

	protected List<XnjsFile> findFiles(ScanSettings settings, String baseDir, Client client)throws Exception{
		return findFiles(settings, baseDir, 0, client);
	}

	// TODO limit on files
	/**
	 * @param settings
	 * @param baseDir - current base relative to the storage root
	 * @param depth - current depth relative to the start of the scan
	 * @param client
	 * @return array of files to process
	 * @throws Exception
	 */
	protected List<XnjsFile> findFiles(ScanSettings settings, String baseDir, int depth, Client client)throws Exception{
		List<XnjsFile>files = new ArrayList<>();
		if(settings.maxDepth<=depth)return files;

		final long lastRun = getLastRun();
		long graceMillis = 1000 * settings.gracePeriod;
		logger.debug("Last run {}", lastRun);

		boolean includeCurrentDir=matches(baseDir,settings);
		logger.debug("Include files in {}: {}", baseDir, includeCurrentDir);
		IStorageAdapter storage = getStorageAdapter(client);
		XnjsFile[]xFiles = storage.ls(baseDir);
		for(XnjsFile xf: xFiles){
			if(xf.isDirectory() && matches(xf.getPath(),settings)){
				files.addAll(findFiles(settings, xf.getPath(), depth+1, client));
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
					logger.debug("Adding: <{}> lastModified: {}", xf.getPath(), lastMod);
					files.add(xf);
				}
				else{
					if(includeCurrentDir){
						logger.debug("Skipping: <{}> lastModified: {}", xf.getPath(), lastMod);
					}
				}
			}
		}
		return files;
	}

	public static long getLastRun(Action action){
		Long l = action.getProcessingContext().getAs(LAST_RUN_TIME, Long.class);
		return l!=null? l.longValue() : 0;
	}

	protected long getLastRun(){
		return getLastRun(action);
	}

	public static String getLastRunInfo(Action action){
		String i = action.getProcessingContext().getAs(LAST_RUN_INFO, String.class);
		return i!=null? i : "n/a";
	}

	protected void storeLastRunInfo(String info) {
		if(info.length()>128)info=info.substring(0, 128)+"...";
		action.getProcessingContext().put(LAST_RUN_INFO, info);
	}

	@SuppressWarnings("unchecked")
	protected Set<String> getSubmittedActionIDs(){
		return (Set<String>)action.getProcessingContext().getAs(ACTION_IDS, Set.class);
	}

	protected void updateLastRunTime(long time){
		action.getProcessingContext().put(LAST_RUN_TIME, time);
	}

	protected void updateActionIDs(Set<String> ids){
		for(Iterator<String> i= ids.iterator(); i.hasNext(); ) {
			try{ 
				if(manager.isActionDone(i.next()))i.remove();
			}catch(Exception ex) {
				i.remove();
			}
		}
		action.getProcessingContext().put(ACTION_IDS, ids);
	}

	protected IStorageAdapter getStorageAdapter(Client client) throws ExecutionException {
		String smsID = getJob().storageUID;
		// this seems to be a nasty way to set the correct client,
		// but as XNJS worker threads do not rely on thread-local Client
		// it is no problem. To be sure, we store the client and reset it
		Client oldClient = AuthZAttributeStore.getClient();
		try {
			AuthZAttributeStore.setClient(client);
			Home h=getKernel().getHome(UAS.SMS);
			SMSBaseImpl sms = (SMSBaseImpl)h.get(smsID);
			return sms.getStorageAdapter();
		} catch(Exception e) {
			throw ExecutionException.wrapped(e);
		}
		finally{
			AuthZAttributeStore.setClient(oldClient);
		}
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
		return getPattern(expr).matcher(path).find();
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
	
	public static final String logDirectory = ".UNICORE_data_processing";

	private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
	
	protected void storeLog(List<String>log, String directory, IStorageAdapter storage) throws Exception {
		if(log.size()==0)return;
		storage.mkdir(directory);
		String fName = directory+"/"+"error-"+df.format(new Date())+".log";
		try(OutputStreamWriter os = new OutputStreamWriter(storage.getOutputStream(fName))) {
			for(String l: log) {
				os.write(l+"\n");
			}
		}
	}

}
