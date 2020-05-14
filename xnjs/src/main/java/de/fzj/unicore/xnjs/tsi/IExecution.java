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
 

package de.fzj.unicore.xnjs.tsi;

import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.ActionStatus;
import de.fzj.unicore.xnjs.ems.ExecutionException;

/**
 * The basic interface to the execution system<br>
 * The various methods will change the action, for example set the BSID.
 * These are executed after the client has been authenticated, so there is no more 
 * reference to the client.<br>
 *
 * @author schuller
 */
public interface IExecution {
	
	/**
	 * uspace name of the (optional) file containing progress information.
	 * (this file may be written by the application or through some other means) 
	 */
	public static final String PROGRESS_FILENAME=".UNICORE_PROGRESS_INDICATION";
	public static final String PROGRESS_NOT_FOUND_KEY="UNICORE_PROGRESS_INDICATION_UNAVAILABLE";
	
	/**
	 * submit a job
	 * 
	 * @param job - the action 
	 * @return initial action state after submission - QUEUED or RUNNING, see {@link ActionStatus}
	 */
	public int submit(Action job) throws TSIBusyException,ExecutionException;
	
	/**
	 * abort a job
	 */
	public void abort(Action job) throws ExecutionException;
	
	/**
	 * pause a job
	 */
	public void pause(Action job) throws ExecutionException;
	
	/**
	 * resume a job
	 */
	public void resume(Action job) throws ExecutionException;
	
	/**
	 * checkpoint a job to allow job migration to a different machine
	 */
	public void checkpoint(Action job) throws ExecutionException;
	
	/**
	 * restart a checkpointed job
	 */
	public void restart(Action job) throws ExecutionException;
	
	/**
	 * update the status of the action on the target system
	 */
	public void updateStatus(Action job) throws ExecutionException;
	
	/**
	 * Get batch-system level job details
	 */
	public String getBSSJobDetails(Action job) throws ExecutionException;

	/**
	 * enable/disable (periodic) back-end job status checking
	 */
	public default void toggleStatusUpdates(boolean enable) {}
}
