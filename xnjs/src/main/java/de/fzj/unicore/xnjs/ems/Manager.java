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
 

package de.fzj.unicore.xnjs.ems;

import eu.unicore.security.Client;

/**
 * The frontend EMS interface<br>
 * It serves as the liason to the front-controller 
 * that can add, pause, etc actions<br>
 * 
 * @author schuller
 */
public interface Manager {

	public void start() throws Exception;
	
	public void stop();
	
	/**
	 * retrieve an action by id.
	 * @param id
	 * @return Action or null if not found
	 */
	public Action getAction(String id)throws ExecutionException;
	
	/**
	 * add a new job into the EMS on behalf of the client
	 * @param job
	 * @param client
	 * @return job id for querying
	 * 
	 * @throws ExecutionException
	 */
	public Object add(Action job, Client client) throws ExecutionException;

	/** 
	 * run an Action that is ready
	 * 
	 * @param id the ID of the action to run
	 * @param client
	 * @return status 
	 * @throws ExecutionException
	 */
	public Object run(String id, Client client) throws ExecutionException;

	/**
	 * get the status of the Action identified by id
	 * 
	 * @see ActionStatus
	 * 
	 * @param id Which action
	 * @param client Who wants the status?
	 * @return A status object
	 * @throws ExecutionException
	 */
	public Integer getStatus(String id, Client client) throws ExecutionException;
	
	/**
	 * pause a running Action
	 * 
	 * @param id Which action
	 * @param client Who
	 * @return A status object
	 * @throws ExecutionException
	 */
	public Object pause(String id, Client client) throws ExecutionException;
	
	/** 
	 * resume an Action that has been paused
	 * 
	 * @param id Which action
	 * @param client Who
	 * @return A status object
	 * @throws ExecutionException
	 */
	public Object resume(String id, Client client) throws ExecutionException;
	
	/** 
	 * abort a running/paused action
	 * 
	 * @param id Which action
	 * @param client Who
	 * @return A status object
	 * @throws ExecutionException
	 */
	public Object abort(String id, Client client) throws ExecutionException;

	
	/** 
	 * restart a done action (without any stage-ins!): status will be set back to "PENDING"
	 * 
	 * @param id Which action
	 * @param client Who
	 * @return A status object
	 * @throws ExecutionException
	 */
	public Object restart(String id, Client client) throws ExecutionException;

	/**
	 * destroys an action and cleans up 
	 * 
	 * @param job -  the id of the job to destroy
	 * @param client - the client
	 * 
	 * @throws ExecutionException
	 */
	public void destroy(String job, Client client) throws ExecutionException;

	
	/**
	 * list all jobs the client may see
	 *  
	 * @param client
	 * @return list of job UUIDs
	 * @throws ExecutionException
	 */
	public String[] list(Client client) throws ExecutionException;
}
