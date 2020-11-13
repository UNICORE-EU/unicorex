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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.event.ContinueProcessingEvent;
import de.fzj.unicore.xnjs.ems.event.XnjsEvent;
import de.fzj.unicore.xnjs.persistence.IActionStore;
import de.fzj.unicore.xnjs.util.LogUtil;

/**
 * The main worker class that takes jobs from a queue 
 * and initiates processing, depending on the action type<br/>
 * 
 * @author schuller
 */
public class JobRunner extends Thread {

	private static final Logger logger=LogUtil.getLogger(LogUtil.XNJS,JobRunner.class); 

	private final XNJS xnjs;

	private final InternalManager mgr;

	private final ActionStateChangeListener changeListener;

	private final BlockingQueue<String> transfer = new ArrayBlockingQueue<>(1);
	
	private final IActionStore jobs;
	
	private final Dispatcher dispatcher;

	volatile boolean isInterrupted=false;
	
	private static AtomicInteger count = new AtomicInteger(0);

	public JobRunner(XNJS xnjs, Dispatcher dispatcher) throws Exception {
		super();
		this.xnjs = xnjs;
		this.dispatcher = dispatcher;
		this.mgr = xnjs.get(InternalManager.class);
		this.changeListener = xnjs.get(ActionStateChangeListener.class, true);
		this.jobs = xnjs.getActionStore("JOBS");
		int n = count.incrementAndGet();
		super.setName("XNJS-"+xnjs.getID()+"-JobRunner-"+n);
		logger.debug("Job runner thread "+n+" starting");
	}

	public void interrupt() {
		isInterrupted=true;
		logger.debug(getName()+" stopping");
		count.decrementAndGet();
		super.interrupt();
	}

	public void process(String actionID){
		transfer.offer(actionID);
	}
	
	/**
	 *  runs actions<br>
	 *  On every iteration, the Jobrunner will
	 *  <ul>
	 *  <li> get an action from the Manager</li>
	 *  <li> get a processing chain for it</li>
	 *  <li> call process() on the first Processor of the chain</li>
	 *  <li> return the action to the Manager</li>
	 *   </ul> 
	 */
	public void run() {
		logger.info("Worker "+getName()+" starting.");
		try{
			while(!isInterrupted){
				while(mgr.isPaused())Thread.sleep(300);
				Action a=null;
				String id = transfer.poll(100, TimeUnit.MILLISECONDS);
				if(id != null){
					if(logger.isDebugEnabled()){
						logger.debug("Processing "+id);
					}
					try{
						a=jobs.getForUpdate(id);
					}catch(Exception eex){
						logger.warn("Can't get action for processing",eex);
					}
					if(a!=null){
						if(check(a)){
							process(a);
						}
						else{
							sendToSleep(a);
						}
					}
					dispatcher.notifyAvailable(this);
				}
			}
		}catch(InterruptedException ie){
			logger.info(getName()+" stopped.");
			return;
		}
	}

	private boolean check(Action a){
		return a.getNotBefore()-System.currentTimeMillis()<0;
	}

	private void sendToSleep(Action a){
		a.setWaiting(true);
		long delay=a.getNotBefore()-System.currentTimeMillis();
		if(delay<=0)delay=5000;
		mgr.scheduleEvent(new ContinueProcessingEvent(a.getUUID()), (int)delay, TimeUnit.MILLISECONDS);
		mgr.doneProcessing(a);
	}

	//processes the action, and returns it to the manager
	private void process(Action a){
		int status=a.getStatus();
		int newStatus;
		XnjsEvent event = null;
		try{
			LogUtil.fillLogContext(a);
			Processor p = xnjs.createProcessor(a.getType());
			if(logger.isDebugEnabled()){
				logger.debug("Processing Action <"+a.getUUID()+"> in status "+ActionStatus.toString(a.getStatus()));
			}
			p.process(a);
			if(logger.isTraceEnabled()){
				logger.trace("New status for Action <"+a.getUUID()+">: "+ActionStatus.toString(a.getStatus()));
			}
			newStatus=a.getStatus();
			if(newStatus!=status){
				try {
					if(changeListener!=null) {
						changeListener.stateChanged(a);
					}
				}catch(Exception ex2){
					logger.warn("Internal error during state change notification.",ex2);
				}
				if(newStatus==ActionStatus.DONE){
					if(a.getParentActionID()!=null){
						try{
							String parent=a.getParentActionID();
							event = new ContinueProcessingEvent(parent);
						}
						catch(Exception ex){
							logger.error("Error sending notification",ex);
						}
					}
				}
			}
			mgr.doneProcessing(a);
			if(event!=null)mgr.handleEvent(event);
		}catch(ProcessingException pe){
			logger.error("Error during processing action <"+a.getUUID()+">",pe);
			try{
				mgr.errorProcessing(a, pe);
			}catch(Exception ex){
				logger.error("Error during error reporting for action <"+a.getUUID()+">",ex);
			}
		}
		catch(Throwable t){
			logger.error("Severe error during processing action <"+a.getUUID()+">",t);
			try{
				mgr.errorProcessing(a, t);
			}catch(Exception ex){
				logger.error("Error during error reporting for action <"+a.getUUID()+">",ex);
			}
		}
		finally{
			LogUtil.clearLogContext();
		}
	}

}
