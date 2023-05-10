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
 

package de.fzj.unicore.xnjs.ems.processors;

import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.ActionResult;
import de.fzj.unicore.xnjs.ems.ActionStatus;
import de.fzj.unicore.xnjs.ems.ProcessingException;
import de.fzj.unicore.xnjs.ems.Processor;
import de.fzj.unicore.xnjs.ems.event.ContinueProcessingEvent;
import de.fzj.unicore.xnjs.util.LogUtil;

/**
 * default Processor with empty handle*() methods, useful when
 * creating your own processors
 * 
 * @author schuller
 */
public class DefaultProcessor extends Processor {
	
	protected static final Logger logger=LogUtil.getLogger(LogUtil.XNJS,DefaultProcessor.class);
	
	protected final static String KEY_FS_CHECKED_FLAG="FILE_SYSTEM_CHECKED";
	
	protected final static String KEY_FIRST_STAGEOUT_FAILURE="FIRST_STAGEOUT_FAILURE";
	
	protected final static String KEY_STATUS_ON_UNPAUSE="__STATUS_ON_UNPAUSE";
	
	/**
	 * By default give the (network) file system 10 seconds to show files 
	 */
	public static final int DEFAULT_FS_TIMEOUT = 10;
	
	public DefaultProcessor(XNJS xnjs){
		super(xnjs);
	}

	protected void handleAborting()throws ProcessingException{
		action.setStatus(ActionStatus.DONE);
		action.setResult(new ActionResult(ActionResult.USER_ABORTED, "Job aborted."));
	}
	
	protected void handlePausing()throws ProcessingException{
		Integer oldStatus = action.getStatus();
		action.getProcessingContext().put(KEY_STATUS_ON_UNPAUSE, oldStatus);
		action.setStatus(ActionStatus.PAUSED);
	}
	
	protected void handlePaused()throws ProcessingException{
		sleep(5000);
	}
	
	protected void handleResuming()throws ProcessingException{
		Integer oldStatus = action.getProcessingContext().getAs(KEY_STATUS_ON_UNPAUSE, Integer.class);
		action.setStatus(oldStatus);
	}

	/**
	 * utility method which sets the action status to DONE and the result
	 * to NOT_SUCCESSFUL with the reason given
	 * @param reason
	 */
	protected void setToDoneAndFailed(String reason){
		action.setStatus(ActionStatus.DONE);
		action.setResult(new ActionResult(ActionResult.NOT_SUCCESSFUL,reason));
	}

	/**
	 * send the action to sleep for the specified time in millis, by
	 * setting the "waiting" flag and scheduling a "continue" event
	 * @param millis
	 */
	protected void sleep(int millis){
		action.setWaiting(true);
		manager.scheduleEvent(new ContinueProcessingEvent(action.getUUID()), millis, TimeUnit.MILLISECONDS);
	}

	/**
	 * store the current system time as a Long object under the given key
	 * @param key
	 */
	protected void storeTimeStamp(String key){
		action.getProcessingContext().put(key, Long.valueOf(System.currentTimeMillis()));
		action.setDirty();
	}

	/**
	 * get the Long time value stored under the given key
	 * @param key
	 */
	protected Long getTimeStamp(String key){
		return (Long)action.getProcessingContext().get(key);
	}

	/**
	 * allow to set action for unit testing
	 * @param a - the action
	 */
	public void setAction(Action a){
		this.action=a;
	}

}
