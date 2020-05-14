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

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.ActionStatus;
import de.fzj.unicore.xnjs.ems.ProcessingException;

/**
 * logs the total (wall-clock) time used to process an action. 
 * The time is taken from state CREATED to state DONE. <br/>
 * 
 * @author schuller
 */
public class LogProcessingTimeProcessor extends DefaultProcessor {

	static final String startTimeKey=
		LogProcessingTimeProcessor.class.getName()+"_starttime";
	
	public LogProcessingTimeProcessor(XNJS xnjs){
		super(xnjs);
	}
	
	@Override
	protected void begin() throws ProcessingException {
		if(action.getProcessingContext().get(startTimeKey)!=null)return;
		Long startTime=Long.valueOf(System.currentTimeMillis());
		action.getProcessingContext().put(startTimeKey,startTime);
		action.setDirty();
	}
	
	@Override
	protected void done() throws ProcessingException {
		if(action.getStatus()==ActionStatus.DONE){
			String msg=buildLogEntry();
			logger.info(msg);
			action.addLogTrace(msg);
		}
	}

	protected String buildLogEntry(){
		StringBuilder sb=new StringBuilder();
		Long startTime=(Long)action.getProcessingContext().get(startTimeKey);
		long endTime=System.currentTimeMillis();
		long time=endTime-startTime.longValue();
		sb.append("TIMER: Processed [").append(action.getUUID());
		sb.append("] of type [").append(action.getType());
		sb.append("] in ").append(time).append(" ms.");
		return sb.toString();
	}
	
}
