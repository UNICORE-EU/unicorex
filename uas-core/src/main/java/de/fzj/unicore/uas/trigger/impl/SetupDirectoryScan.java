package de.fzj.unicore.uas.trigger.impl;

import java.util.concurrent.Callable;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.uas.trigger.xnjs.ScanSettings;
import de.fzj.unicore.uas.trigger.xnjs.SharedTriggerProcessor;
import de.fzj.unicore.uas.trigger.xnjs.TriggerProcessor;
import de.fzj.unicore.uas.util.LogUtil;
import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.InternalManager;
import de.fzj.unicore.xnjs.ems.Manager;
import eu.unicore.security.Client;

/**
 * initiates a periodic scan of a directory
 * 
 * @author schuller
 */
public class SetupDirectoryScan implements Callable<String>{

	private static final Logger logger = LogUtil.getLogger(LogUtil.TRIGGER, SetupDirectoryScan.class);

	private final Client client;
	
	private final XNJS xnjs;
	
	private final ScanSettings scanSettings;
	
	/**
	 * 
	 * @param storageID -  unique ID of the storage service
	 * @param baseDirectory - base directory relative to storage root
	 * @param client - the Client
	 * @param xnjs - the XNJS configuration
	 * @param interval - requested update interval
	 * @param includes - directory patterns to include in recursion
	 * @param excludes - directory patterns to exclude in recursion
	 * @param maxDepth - maximum depth for recursion
	 * @param sharedMode - if <code>true</code>, the parent storage is a shared one
	 */
	public SetupDirectoryScan(String storageID, String baseDirectory, Client client, XNJS xnjs, int interval, 
			String[]includes, String[] excludes, int maxDepth, boolean sharedMode){
		this.scanSettings=createSettings(storageID, baseDirectory, interval, includes, excludes, maxDepth, sharedMode);
		this.client=client;
		this.xnjs=xnjs;
	}
	
	/**
	 * 
	 * @param scanSettings
	 * @param client
	 * @param xnjs
	 */
	public SetupDirectoryScan(ScanSettings scanSettings, Client client, XNJS xnjs){
		this.scanSettings=scanSettings;
		this.client=client;
		this.xnjs=xnjs;
	}
	
	public String call() throws Exception{
		if(actionExists()){
			logger.info("Scan of <"+scanSettings.storageUID+"/"+scanSettings.baseDirectory
					+"> is already running for for <"+client.getDistinguishedName()+">");
			return getActionUUID();
		}
		logger.info("Setting up directory scan for <"+client.getDistinguishedName()+">");
		Action a=new Action();
		a.setUUID(getActionUUID());
		String type = scanSettings.sharedStorageMode? SharedTriggerProcessor.actionType : TriggerProcessor.actionType;
		a.setType(type);
		a.setClient(client);
		a.setAjd(scanSettings);
		xnjs.get(Manager.class).add(a, client);
		return a.getUUID();
	}

	private ScanSettings createSettings(String storageID, String baseDirectory, int interval, 
			String[]includes, String[] excludes, int maxDepth, boolean shared){
		ScanSettings ajd=new ScanSettings();
		ajd.storageUID=storageID;
		ajd.baseDirectory=baseDirectory;
		ajd.updateInterval=interval;
		ajd.includes=includes;
		ajd.excludes=excludes;
		ajd.sharedStorageMode = shared;
		if(maxDepth>0)ajd.maxDepth=maxDepth;
		return ajd;
	}
	
	public String getActionUUID(){
		int hash=(scanSettings.baseDirectory+client.getDistinguishedName()).hashCode();
		return scanSettings.storageUID+"-scan"+hash;
	}
	
	private boolean actionExists() throws Exception {
		return xnjs.get(InternalManager.class).getAction(getActionUUID())!=null;
	}
}
