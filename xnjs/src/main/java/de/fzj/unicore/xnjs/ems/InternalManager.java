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

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import de.fzj.unicore.xnjs.ems.event.EventHandler;
import de.fzj.unicore.xnjs.ems.event.XnjsEvent;


/**
 * this interface is used within the EMS to manage running actions
 * 
 * @author schuller
 */
public interface InternalManager extends EventHandler {
	
	/**
	 * Get an Action for processing<br>
	 * 
	 * An action must explicitly "returned" to the manager using 
	 *   doneProcessing() if everything went well, or
	 *   errorProcessing() if errors occured
	 * <br>
	 * This method will not return the same action twice, if no call to
	 * doneProcessing() or errorProcessing() has been made.
	 *
	 * @throws ExecutionException
	 * @throws {@link InterruptedException}
	 */
	public Action getNextActionForProcessing() throws InterruptedException, ExecutionException;
	
	/**
	 * notify manager that processing is done
	 */
	public void doneProcessing(Action a);
	
	/**
	 * notify manager that an error occured during processing
	 */
	public void errorProcessing(Action a, Throwable t);

	/**
	 * retrieve an action by id.
	 * @param id
	 * @return Action or null if not found
	 */
	public Action getAction(String id)throws ExecutionException;
	
	/**
	 * construct a sub action and start processing it
	 * 
	 * @param jobDescription - the job description for the subaction
	 * @param type - the sub action type
	 * @param parentAction - the parent action
	 * @param notifyDone - whether to notify the parent when the subaction is done
	 * @return the unique id of the new action
	 */
	public String addSubAction(Serializable jobDescription, String type, Action parentAction, boolean notifyDone)throws ExecutionException;
	
	/**
	 * add an internal action
	 */
	public Object addInternalAction(Action a) throws ExecutionException;
	
	/**
	 * has the specified action finished (i.e. is the status = DONE)
	 * @param actionID
	 * @throws ExecutionException when the action is not found
	 */
	public boolean isActionDone(String actionID) throws ExecutionException;
	
	public int getAllJobs();
	
	public int getDoneJobs();
	
	public boolean getIsAcceptingNewActions();
	
	public void startAcceptingNewActions();
	
	public void stopAcceptingNewActions();
	
	public boolean isPaused();
	
	public void pauseProcessing();
	
	public void resumeProcessing();
	
	/**
	 * process the given event after the defined delay
	 * 
	 * @param event - the event to process
	 * @param delay - delay 
	 * @param unit - delay units
	 */
	public void scheduleEvent(final XnjsEvent event, int delay, TimeUnit unit);
}
