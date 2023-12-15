package de.fzj.unicore.xnjs.util;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.io.XnjsFile;
import de.fzj.unicore.xnjs.tsi.TSI;
import eu.unicore.security.Client;

/**
 * get periodical updates on the properties of a 
 * file on the target system
 * 
 * @author schuller
 */
public class FileMonitor implements Runnable {

	private static final Logger log=LogUtil.getLogger(LogUtil.XNJS,FileMonitor.class);
	
	private final Client client;
	private final String workingDirectory;
	private final String target;
	
	private final long updateInterval;
	private final TimeUnit timeUnit;
	
	private XnjsFile info;
	private volatile boolean interrupt;
	private final Set<Observer<XnjsFile>>observers = new HashSet<>();
	
	private final XNJS configuration;
	
	public FileMonitor(String workingDirectory, String target, Client client, XNJS config){
		this(workingDirectory, target,client,config,5,TimeUnit.SECONDS);
	}

	/**
	 * @param workingDirectory
	 * @param target  - file relative to working directory
	 * @param client
	 * @param config
	 * @param updateInterval
	 * @param timeUnit
	 */
	public FileMonitor(String workingDirectory, String target, Client client, XNJS config, long updateInterval, TimeUnit timeUnit){
		this.workingDirectory=workingDirectory;
		this.target=target;
		this.client=client;
		this.updateInterval=updateInterval;
		this.timeUnit=timeUnit;
		this.configuration=config;
		run();
		reschedule();
	}

	private void reschedule(){
		configuration.getScheduledExecutor().schedule(this, updateInterval, timeUnit);
	}
	
	public synchronized void run(){
		try {
			if(interrupt)return;
			TSI tsi=configuration.getTargetSystemInterface(client);
			tsi.setStorageRoot(workingDirectory);
			XnjsFile newInfo=tsi.getProperties(target);
			//check if file was modified
			if(newInfo!=null){
				if(info==null || newInfo.getLastModified().compareTo(info.getLastModified())>=0){
					info=newInfo;
					notifyObservers();
				}				
			}
			else{
				log.debug("File <{}> not found in working directory <{}>", target, workingDirectory);
				info=null;	
			}
			reschedule();
		} catch (ExecutionException e) {
			log.error(e);
		}
	}
	
	public void dispose(){
		interrupt=true;
	}

	/**
	 * returns an {@link XnjsFile} or null if the target does not exist
	 */
	public synchronized XnjsFile getInfo(){
		return info;
	}
	
	public void registerObserver(Observer<XnjsFile>obs){
		observers.add(obs);
	}
	
	private void notifyObservers(){
		for(Observer<XnjsFile>obs: observers){
			obs.update(info);
		}
	}
	
}
