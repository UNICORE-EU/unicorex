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

import de.fzj.unicore.xnjs.XNJS;

/**
 * Action processing<br>
 * Processors may be chained to produce a more complex processor 
 * 
 * @author schuller
 */
public abstract class Processor {

	protected final XNJS xnjs;

	protected InternalManager manager;

	protected Action action;
	
	protected Processor next;
	
	public Processor(XNJS xnjs){
		this.xnjs = xnjs;
		this.manager=xnjs.get(InternalManager.class);
	}
	
	public void setNext(Processor p){
		next=p;
	}

	public String toString(){
		String nextName="";
		if(next!=null){nextName="->"+next.toString();}
		return this.getClass().getName()+"@"+hashCode()+nextName;
	}

	/**
	 * process the action: based on the action status, 
	 * the various handle*() methods are called
	 */
	public final void process(Action a) throws Exception{
		this.action=a;
		begin();
		if(a.getTransitionalStatus()!=ActionStatus.TRANSITION_NONE){
			try{
				processTransition(action);
			}
			finally{
				a.setTransitionalStatus(ActionStatus.TRANSITION_NONE);
			}
		}

		switch(action.getStatus()){
		
		case ActionStatus.DONE:
			break;
		
		case ActionStatus.CREATED:
			handleCreated();
			break;

		case ActionStatus.PREPROCESSING:
			handlePreProcessing();
			break;

		case ActionStatus.READY:
			handleReady();
			break;

		case ActionStatus.PENDING:
			handlePending();
			break;

		case ActionStatus.QUEUED:
			handleQueued();
			break;

		case ActionStatus.RUNNING:
			handleRunning();
			break;

		case ActionStatus.POSTPROCESSING:
			handlePostProcessing();
			break;

		case ActionStatus.PAUSED:
			handlePaused();
			break;

		default:
		}

		done();
		if(next!=null)
		{
			next.process(action);
		}
	}

	final void processTransition(Action a) throws Exception{
		this.action=a;
		
		switch(action.getTransitionalStatus()){
		
		case ActionStatus.TRANSITION_ABORTING:
			handleAborting();
			break;
		case ActionStatus.TRANSITION_PAUSING:
			handlePausing();
			break;
		case ActionStatus.TRANSITION_RESUMING:
			handleResuming();
			break;
		case ActionStatus.TRANSITION_REMOVING:
			handleRemoving();
			break;
		case ActionStatus.TRANSITION_RESTARTING:
			handleRestarting();
			break;
		}
		
		if(next!=null)
		{
			next.processTransition(action);
		}
	}
	
	/**
	 * executed at the very beginning of process()
	 */
	protected void begin() throws Exception {}

	/**
	 * executed at the very end of process()
	 */
	protected void done() throws Exception {}

	/** handle state "CREATED"
	 */
	protected void handleCreated() throws Exception {}

	/** handle state "PreProcessing"
	 */
	protected void handlePreProcessing() throws Exception {}
	/**
	 * handle "READY" state
	 */
	protected void handleReady() throws Exception {}

	/**
	 * handle "PENDING" state
	 */
	protected void handlePending() throws Exception {}

	/**
	 * handle "QUEUED" state
	 */
	protected void handleQueued() throws Exception {}

	/**
	 * handle "RUNNING" state
	 */
	protected void handleRunning() throws Exception {}

	/**
	 * handle "POSTPROCESSING" state
	 */
	protected void handlePostProcessing() throws Exception {}

	/**
	 * handle "PAUSED" state
	 */
	protected void handlePaused() throws Exception {}
 
	/**
	 * handle transitional "aborting" state
	 */
	protected void handleAborting() throws Exception {}

	/**
	 * handle transitional "pausing" state
	 */
	protected void handlePausing() throws Exception {}

	/**
	 * handle transitional "resuming" state
	 */
	protected void handleResuming() throws Exception {}

	/**
	 * handle transitional "removing" state
	 */
	protected void handleRemoving() throws Exception {}
	
	/**
	 * handle transitional "restarting" state
	 */
	protected void handleRestarting() throws Exception {}
}
