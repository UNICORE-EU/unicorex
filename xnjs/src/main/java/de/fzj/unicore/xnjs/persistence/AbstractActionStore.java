/*********************************************************************************
 * Copyright (c) 2006 Forschungszentrum Juelich GmbH 
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


package de.fzj.unicore.xnjs.persistence;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.persist.PersistenceException;
import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.ActionStatus;
import de.fzj.unicore.xnjs.util.LogUtil;

/**
 * Stores actions in some storage back-end.
 * 
 * @author schuller
 */
public abstract class AbstractActionStore implements IActionStore{

	protected static final Logger logger=LogUtil.getLogger(LogUtil.PERSISTENCE,AbstractActionStore.class);	

	/**
	 * interval (seconds) in which to refill the work queue from the database
	 * default: 10
	 */
	public static final String QUEUE_REFILL_INTERVAL="xnjs.queue.refill.delay";

	private static final AtomicInteger idGenerator=new AtomicInteger(0);

	protected String id = String.valueOf(idGenerator.incrementAndGet());

	protected String name;

	protected final Map<String,Integer>states=new ConcurrentHashMap<String,Integer>();
	
	public void setName(String name){
		this.name=name;
	}
	
	public String getName(){
		return name;
	}
	
	/**
	 * removes all actions from the persistence DB
	 *
	 * @throws PersistenceException
	 * @throws TimeoutException
	 */
	public void doCleanup()throws PersistenceException, TimeoutException{
		for(String key: getUniqueIDs()){
			Action a=get(key);
			if(a!=null)remove(a);
		}
	}

	public Action get(String key) throws PersistenceException{
		return doGet(key);
	}

	public Action getForUpdate(String key) throws TimeoutException, PersistenceException{
		Action a=doGetForUpdate(key);
		if(a!=null){
			states.put(key, a.getStatus());
			a.setWaiting(false);
			if(logger.isDebugEnabled())logger.debug("GET FOR UPDATE " + key);
		}
		return a;
	}

	/**
	 * Attempts to lock and get an ACTIVE action. 
	 * Returns <code>null</code> immediately if the action cannot be locked
	 * 
	 * @param id
	 * 
	 * @throws PersistenceException
	 * @throws TimeoutException
	 */
	protected abstract Action tryGetForUpdate(String id)throws PersistenceException,TimeoutException;

	public int getTotalActionsInStore(){
		try{
			return size();
		}catch(PersistenceException pe){
			return -1;
		}
	}

	public String printStorageOverview(){
		return toString();
	}

	public void put(String key, Action value)throws PersistenceException{
		doStore(value);
		if(logger.isDebugEnabled())logger.debug("STORE " + value.getUUID());
		states.put(key,value.getStatus());
	}

	public void remove(Action a)throws PersistenceException{
		states.remove(a.getUUID());
		doRemove(a);
	}

	public abstract int size() throws PersistenceException;

	public int size(int status) throws PersistenceException{
		int i=0;
		for(Integer s: states.values()){
			if(s.intValue()==status)i++;
		}
		return i;
	}

	/**
	 * get the unique IDs of active actions (i.e. where status is not DONE)
	 * @throws PersistenceException
	 */
	public abstract Collection<String> getActiveUniqueIDs() throws PersistenceException;

	/**
	 * store a DAO in the backend store. this method is responsible for checking the
	 * "dirty" status
	 */
	protected abstract void doStore(Action action) throws PersistenceException;

	/**
	 * get a DAO from the backend store
	 */
	protected abstract Action doGet(String id) throws PersistenceException;

	/**
	 * get a DAO from the backend store, aquiring a write lock
	 */
	protected abstract Action doGetForUpdate(String id) throws PersistenceException, TimeoutException;

	/**
	 * delete a DAO in the backend store
	 */
	protected abstract void doRemove(Action a) throws PersistenceException;

	public String printDiagnostics()throws PersistenceException{
		StringBuilder sb=new StringBuilder();
		long start=System.currentTimeMillis();
		String newline=System.getProperty("line.separator");
		sb.append("DIAGONSTIC INFO storage <"+name+"."+id+">"+newline);
		sb.append(newline);
		sb.append("Entries in database: "+getUniqueIDs().size()+newline);
		sb.append("DONE: "+size(ActionStatus.DONE)+newline);
		sb.append("RUNNING: "+size(ActionStatus.RUNNING)+newline);
		sb.append("READY: "+size(ActionStatus.READY)+newline);
		sb.append("PENDING: "+size(ActionStatus.PENDING)+newline);
		sb.append("QUEUED: "+size(ActionStatus.QUEUED)+newline);
		sb.append("PREPROCESSING: "+size(ActionStatus.PREPROCESSING)+newline);
		sb.append("POSTPROCESSING: "+size(ActionStatus.POSTPROCESSING)+newline);
		long time=System.currentTimeMillis()-start;
		sb.append("Implementation: "+getClass().getName()+newline);
		sb.append("Time to generate diagnostic info: "+time+" ms."+newline);
		return sb.toString();
	}

}
