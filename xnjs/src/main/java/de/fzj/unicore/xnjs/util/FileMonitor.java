/*********************************************************************************
 * Copyright (c) 2008 Forschungszentrum Juelich GmbH 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * (1) Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the disclaimer at the end. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * 
 * (2) Neither the name of Forschungszentrum Juelich GmbH nor the names of its 
 * contributors may be used to endorse or promote products derived from this 
 * software without specific prior written permission.
 * 
 * DISCLAIMER
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *********************************************************************************/

package de.fzj.unicore.xnjs.util;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

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
	private final Set<Observer<XnjsFile>>observers=new HashSet<Observer<XnjsFile>>();
	
	private final XNJS configuration;
	
	public FileMonitor(String workingDirectory, String target, Client client, XNJS config){
		this(workingDirectory, target,client,config,5,TimeUnit.SECONDS);
	}
	
	private void init(){
		run();
		configuration.getScheduledExecutor().scheduleWithFixedDelay(this, updateInterval, updateInterval, timeUnit);
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
		init();
	}
	
	public synchronized void run(){
		try {
			if(interrupt)throw new RuntimeException();
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
				log.debug("File not found: "+target+ " in working directory "+workingDirectory);
				info=null;	
			}
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
