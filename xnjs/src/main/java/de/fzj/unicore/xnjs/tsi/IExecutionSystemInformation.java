/*********************************************************************************
 * Copyright (c) 2006-2010 Forschungszentrum Juelich GmbH 
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

import java.util.List;
import java.util.Map;

import de.fzj.unicore.xnjs.ems.BudgetInfo;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.persistence.IActionStore;
import eu.unicore.security.Client;

/**
 * runtime information about the execution system
 *  
 * @author schuller
 */
public interface IExecutionSystemInformation {

	/**
	 * the (best guess for) the total number of jobs
	 * on the (batch) system, i.e. including those not
	 * controlled by UNICORE. This includes queued and 
	 * running jobs
	 */
	public int getTotalNumberOfJobs();
	
	/**
	 * the (best guess for) the total number of running jobs
	 * on the system, i.e. including those not
	 * controlled by UNICORE
	 */
	public int getNumberOfRunningJobs();
	
	
	/**
	 * the (best guess for) the total number of queued jobs
	 * on the system, i.e. including those not
	 * controlled by UNICORE
	 */
	public int getNumberOfQueuedJobs();
	
	/**
	 * get the number of jobs in the queues on the system. 
	 * This can be <code>null</code> if the system isn't 
	 * running a batch system. The set of queues need not be complete,
	 * and the jobs can be RUNNING or QUEUED
	 * 
	 * @return queue fill information or <code>null</code> if the system 
	 * does not provide this information or is not batch-queue based
	 */
	public Map<String,Integer>getQueueFill();
	
	/**
	 * get the remaining compute time budget for the given user
	 * 
	 * @return compute budget (one per project) or empty list if not applicable 
	 */
	public List<BudgetInfo> getComputeTimeBudget(Client client) throws ExecutionException;
	
	/**
	 * get (measured) mean time (seconds) for jobs in the queue(s)
	 */
	public long getMeanTimeQueued();

	/**
	 * on startup, initialize required data structures
	 * @param jobStore
	 */
	public default void initialise(IActionStore jobStore) throws Exception {}
}
