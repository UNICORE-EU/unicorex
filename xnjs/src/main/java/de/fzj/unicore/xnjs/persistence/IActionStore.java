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
import java.util.concurrent.TimeoutException;

import de.fzj.unicore.xnjs.ems.Action;
import eu.unicore.persist.PersistenceException;

/**
 * Interface to the action storage<br/>
 * Also maintains the XNJS work queue, i.e. the set of those actions that are currently
 * being processed
 * 
 * @author schuller
 */
public interface IActionStore {
	
	/**
	 * property key, if set to true persistence layer will 
	 * clear everything on startup 
	 */
	public static final String CLEAR_ON_STARTUP="de.fzj.unicore.xnjs.persistence.clearAllOnStartup";
	
	/**
	 * set the ID of this store
	 * 
	 * @param id
	 */
	public void setName(String id);
	
	
	/**
	 * retrieve an action
	 * 
	 * @param uuid - the action's unique ID
	 */
	public Action get(String uuid) throws Exception;
	
	/**
	 * Retrieve an action and aquire the right for 
	 * making persistent changes to it<br/>
	 * 
	 * This method must be matched with a corresponding call to put()
	 * 
	 * @param uuid
	 * 
	 * @return action
	 * @throws TimeoutException
	 * @throws PersistenceException 
	 */
	public Action getForUpdate(String uuid) throws TimeoutException, Exception;
	
	/**
	 * store an action but don't add it to the work queue
	 * @param key
	 * @param value
	 * @throws PersistenceException
	 */
	public void put(String key, Action value) throws Exception;
	
	/**
	 * remove an action
	 * @param action - the {@link Action} to remove
	 */
	public void remove(Action action) throws Exception,TimeoutException;

	/**
	 * get all IDs
	 */
	public Collection<String> getUniqueIDs() throws Exception;
	
	/**
	 * get all IDs where the action status is not DONE
	 */
	public Collection<String> getActiveUniqueIDs() throws Exception;
	
	/**
	 * get the total number of entries
	 */
	public int size() throws Exception;
	
	/**
	 * get the total number of entries with the given status
	 */
	public int size(int actionStatus) throws Exception;
	
	/**
	 * print some information about this store
	 * 
	 * @return String with diagnostic info
	 */
	public String printDiagnostics();
	
	/**
	 * delete everything
	 * @throws Exception 
	 */
	public void removeAll() throws Exception;
}