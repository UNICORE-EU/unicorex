package eu.unicore.uas.trigger.xnjs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import eu.unicore.uas.util.LogUtil;
import eu.unicore.util.Log;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.ems.ActionStatus;
import eu.unicore.xnjs.ems.ProcessingException;
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

	public static final String actionType="DIRECTORY_SCAN";

	public static final String LAST_RUN_TIME="LAST_RUN_TIME";
	
	private IStorageAdapter storage;

	public TriggerProcessor(XNJS xnjs) {
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
	protected void handleRunning() throws ProcessingException {
		long thisRun = System.currentTimeMillis();
		ScanSettings sad=getJob();
		try{
			RuleFactory rf=new RuleFactory(getStorageAdapter(), sad.storageUID);
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
					RuleSet rules=rf.getRules(sad.baseDirectory);
					XnjsFile[]files=findFiles(sad.baseDirectory);
					if(files.length>0){
						TriggerRunner tr=new TriggerRunner(files, rules, getStorageAdapter(), action.getClient(), xnjs);
						getKernel().getContainerProperties().getThreadingServices().getExecutorService().submit(tr);
						logger.debug("Executing trigger run on <{}> files.", files.length);	
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
				sleep(120, TimeUnit.SECONDS);
			}
		}catch(ResourceUnknownException rue){
			// storage is gone for some reason, quit
			setToDoneAndFailed(Log.createFaultMessage("", rue));
		}catch(Exception ex){
			// do not quit processing, it might be a transient error
			Log.logException("Error during trigger processing on storage "+sad.storageUID,ex,logger);
			sleep(120, TimeUnit.SECONDS);
		}
	}

	protected XnjsFile[] findFiles(String baseDir)throws Exception{
		return findFiles(baseDir, 0);
	}

	// TODO limit on files
	/**
	 * @param baseDir - current base relative to the storage root
	 * @param depth - current depth relative to the start of the scan
	 * @return array of files to process
	 * @throws Exception
	 */
	protected XnjsFile[] findFiles(String baseDir, int depth)throws Exception{
		ScanSettings settings=getJob();
		if(settings.maxDepth<=depth)return new XnjsFile[0];

		long lastRun = getLastRun();
		long graceMillis = 500*settings.gracePeriod;
		
		if(logger.isDebugEnabled()){
			logger.debug("Last run "+lastRun);
		}
		List<XnjsFile>files = new ArrayList<>();
		
		boolean includeCurrentDir=matches(baseDir,settings);
		logger.debug("Include files in {}: {}", baseDir, includeCurrentDir);
		IStorageAdapter storage=getStorageAdapter();
		XnjsFile[]xFiles=storage.ls(baseDir);
		for(XnjsFile xf: xFiles){
			if(xf.isDirectory() && matches(xf.getPath(),settings)){
				files.addAll(Arrays.asList(findFiles(xf.getPath(),depth+1)));
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

	protected long getLastRun(){
		Long l = action.getProcessingContext().getAs(LAST_RUN_TIME, Long.class);
		return l!=null? l.longValue() : 0;
	}


	protected void updateLastRunTime(long time){
		action.getProcessingContext().put(LAST_RUN_TIME, time);
	}

	protected IStorageAdapter getStorageAdapter() throws Exception{
		if(storage==null){
			String smsID = getJob().storageUID;
			// this seems to be a nasty way to set the correct client,
			// but as XNJS worker threads do not rely on thread-local Client
			// it is no problem. To be sure, we store the client and reset it
			Client oldClient = AuthZAttributeStore.getClient();
			try{
				AuthZAttributeStore.setClient(action.getClient());
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

	private Map<String, Pattern>patterns=new HashMap<String, Pattern>();

	private Pattern getPattern(String expr){
		Pattern p=patterns.get(expr);
		if(p==null){
			p=Pattern.compile(expr);
			patterns.put(expr, p);
		}
		return p;
	}

}
