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
import java.util.Collection;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.event.CallbackEvent;
import de.fzj.unicore.xnjs.ems.event.ContinueProcessingEvent;
import de.fzj.unicore.xnjs.ems.event.StartJobEvent;
import de.fzj.unicore.xnjs.ems.event.SubActionDoneEvent;
import de.fzj.unicore.xnjs.ems.event.XnjsEvent;
import de.fzj.unicore.xnjs.idb.ApplicationInfo;
import de.fzj.unicore.xnjs.persistence.IActionStore;
import de.fzj.unicore.xnjs.tsi.IExecutionSystemInformation;
import de.fzj.unicore.xnjs.util.ErrorCode;
import de.fzj.unicore.xnjs.util.LogUtil;
import eu.unicore.security.Client;
import eu.unicore.util.Log;

/**
 * Default implementation of the ems manager interfaces<br>
 * 
 * This class implements the ems front end, and starts up workers
 * that actually do the work. 
 *
 * @author schuller
 */
@Singleton
public class BasicManager implements Manager, InternalManager {

	private static final Logger logger=LogUtil.getLogger(LogUtil.XNJS,BasicManager.class); 

	//job store
	private IActionStore jobs;

	private Dispatcher dispatcher;

	private volatile boolean isAcceptingNewActions = false;

	private volatile boolean isPaused = false;

	private volatile boolean started = false;

	private final AtomicLong storeOperations=new AtomicLong(0);

	private final XNJS xnjs;

	private final IExecutionContextManager ecm;

	@Inject
	public BasicManager(XNJS xnjs, IExecutionContextManager ecm){
		this.xnjs = xnjs;
		this.ecm = ecm;
	}

	@Override
	public Object add(Action action, Client client) throws Exception {
		if(isAcceptingNewActions==false){
			throw new ExecutionException(ErrorCode.ERR_XNJS_DISABLED,"XNJS does not accept new actions.");
		}
		action.addLogTrace("Created with type '"+action.getType()+"'");
		if(client!=null){
			action.setClient(client);
			action.addLogTrace("Client: "+client);
		}
		ecm.getContext(action);
		jobs.put(action.getUUID(),action);
		if(!action.isWaiting()){
			dispatcher.process(action.getUUID());
		}
		return action.getUUID();
	}

	@Override
	public String[] list(Client client) throws Exception {
		Collection<String>ids = jobs.getUniqueIDs();
		return (String[])ids.toArray(new String [ids.size()]);
	}

	@Override
	public synchronized void start() throws Exception {
		if(started)return;
		jobs = xnjs.getActionStore("JOBS");
		assert jobs!=null;
		IExecutionSystemInformation ies = xnjs.get(IExecutionSystemInformation.class, true);
		if(ies!=null)ies.initialise(jobs);
		dispatcher = new Dispatcher(xnjs);
		dispatcher.start();
		startAcceptingNewActions();
		started = true;
	}

	@Override
	public synchronized void stop() {
		stopAcceptingNewActions();
		dispatcher.interrupt();
	}

	@Override
	public Integer getStatus(String id, Client client) throws Exception {
		Action a=jobs.get(id);
		if(a==null)throw new ExecutionException(ErrorCode.ERR_NO_SUCH_ACTION, "No such action: "+id);
		return a.getStatus();
	}

	@Override
	public Action getAction(String id) throws Exception{
		return jobs.get(id);
	}

	public Action getActionForUpdate(String id)throws TimeoutException,Exception{
		return jobs.getForUpdate(id);
	}

	/**
	 * called from the workers when a processing iteration has finished without error
	 */
	@Override
	public void doneProcessing(Action a){
		if(a!=null){
			try{
				storeOperations.incrementAndGet();
				String id=a.getUUID();
				if(a.getStatus()==ActionStatus.DONE){
					a.setWaiting(false);
					jobs.put(id, a);
				}
				else if(a.getStatus()!=ActionStatus.DESTROYED){
					jobs.put(id, a);
					if(!a.isWaiting()){
						dispatcher.process(id);
					}
				}
				else {
					jobs.remove(a);
					logger.debug("[{}] Action is destroyed.", id);
				}
			}
			catch(TimeoutException te){
				logger.error("Internal Error: can't remove job <"+a.getUUID()+">");
			}
			catch(Exception e){
				logger.error("Persistence problem",e);
			}
		}
		else{
			logger.warn("Internal Error: doneProcessing() called with non-existent action?");
		}

	}
	/**
	 * processing iteration has produced an error
	 */
	@Override
	public void errorProcessing(Action a, Throwable t){
		if(a!=null){
			a.addLogTrace("Processing failed, aborting");
			a.addLogTrace(t.getMessage());
			a.setStatus(ActionStatus.DONE);
			a.getResult().setStatusCode(ActionResult.NOT_SUCCESSFUL);
			a.getResult().setErrorMessage(Log.createFaultMessage("Processing failed", t));
			try{
				jobs.put(a.getUUID(),a);
			}catch(Exception pe){
				LogUtil.logException("Persistence problem", pe, logger);
			}
			if(a.getParentActionID()!=null){
				String parent=a.getParentActionID();
				try{
					handleEvent(new SubActionDoneEvent(parent));
				}catch(Exception ex){
					LogUtil.logException("Error sending notification", ex, logger);
				}
			}
		}
		else{
			logger.error("Internal Error: errorProcessing() called with non-existent action?");
		}
	}

	@Override
	public Object pause(String id, Client client) throws Exception {
		Action a = null;
		try{
			a=getActionForUpdate(id);
			if(ActionStatus.canPause(a.getStatus()))
			{
				a.setTransitionalStatus(ActionStatus.TRANSITION_PAUSING);
				return "Action will be paused";
			}
			else{
				throw new ExecutionException(ErrorCode.ERR_OPERATION_NOT_POSSIBLE,"Cannot pause the action.");
			}
		}finally{
			if(a!=null) {
				jobs.put(id,a);
				dispatcher.process(id);
			}
		}
	}

	@Override
	public Object resume(String id, Client client) throws Exception {
		Action a=null;
		try{
			a=getActionForUpdate(id);
			if(ActionStatus.canResume(a.getStatus())){
				a.setTransitionalStatus(ActionStatus.TRANSITION_RESUMING);
				return "Action will be resumed";
			}
			else{
				throw new ExecutionException(ErrorCode.ERR_OPERATION_NOT_POSSIBLE,"Cannot resume the action.");
			}
		}finally{
			if(a!=null){
				jobs.put(id,a);	
				dispatcher.process(id);
			}
		}
	}

	@Override
	public Object abort(String id, Client client) throws Exception {
		Action a=null;
		try{
			a=getActionForUpdate(id);
			if(a==null) return null;
			if(a.getStatus()==ActionStatus.DONE) return "Action is done.";
			if(!ActionStatus.canAbort(a.getStatus())){
				throw new ExecutionException(ErrorCode.ERR_OPERATION_NOT_POSSIBLE,"Cannot abort the action.");
			}
			else{
				a.addLogTrace("Got 'abort' request.");
				a.setTransitionalStatus(ActionStatus.TRANSITION_ABORTING);
				return "Action will be aborted";
			}
		}finally{
			if(a!=null){
				jobs.put(id,a);
				dispatcher.process(id);
			}
		}
	}

	@Override
	public Object run(String id, Client client) throws Exception {
		Action a=getAction(id);
		if(a==null) {
			throw new ExecutionException(ErrorCode.ERR_NO_SUCH_ACTION,"Action with id="+id+" could not be found.");
		}
		//check status: must be "READY" or before
		int s=a.getStatus();
		if(!ActionStatus.canRun(s)){
			return null;
		}
		//handle start as an async event
		handleEvent(new StartJobEvent(id));
		return ActionStatus.PENDING;
	}

	@Override
	public Object restart(String id, Client client) throws Exception {
		Action a = getAction(id);
		if(a==null) {
			throw new ExecutionException(ErrorCode.ERR_NO_SUCH_ACTION,"Action with id="+id+" could not be found.");
		}
		// check status: must be "DONE"
		int s=a.getStatus();
		if(!ActionStatus.canRestart(s)){
			return null;
		}
		logger.info("Initiating restart for <{}>", id);
		// required for move back into active Jobs - a bit dangerous
		jobs.remove(a);
		// re-set state so the JobRunner will submit it
		a.setStatus(ActionStatus.PENDING);
		a.setTransitionalStatus(ActionStatus.TRANSITION_RESTARTING);
		jobs.put(id,a);
		dispatcher.process(id);
		return ActionStatus.PENDING;
	}

	@Override
	public String addSubAction(Serializable jobDescription, String type, Action parentAction, boolean notify) 
			throws Exception{
		String parentUUID=parentAction.getUUID();
		Action soa=new Action();
		soa.setType(type);
		soa.setParentActionID(parentUUID);
		soa.setRootActionID(parentAction.getRootActionID());
		soa.setClient(parentAction.getClient());
		soa.setAjd(jobDescription);
		soa.setProcessingContext(parentAction.getProcessingContext());
		ecm.createChildContext(parentAction,soa);
		ApplicationInfo childAppInfo=new ApplicationInfo();
		soa.setApplicationInfo(childAppInfo);
		addInternalAction(soa);
		return soa.getUUID();
	}

	@Override
	public Object addInternalAction(Action a) throws Exception{
		a.addLogTrace("Created with type '"+a.getType()+"'");
		a.addLogTrace("Client: "+a.getClient());
		a.setInternal(true);
		String actionID = a.getUUID();
		logger.debug("Adding internal action <{}> of type <{}>", actionID, a.getType());
		jobs.put(actionID, a);
		dispatcher.process(actionID);
		return a.getUUID();
	}

	@Override
	public boolean isActionDone(String actionID) throws Exception {
		Action a=jobs.get(actionID);
		if(a==null)throw new ExecutionException(ErrorCode.ERR_NO_SUCH_ACTION,"No such action.");
		return a.getStatus()==ActionStatus.DONE;
	}

	@Override
	public void destroy(String actionID, Client client) throws Exception {
		Action a=null;
		try{
			a=jobs.getForUpdate(actionID);
			a.setTransitionalStatus(ActionStatus.TRANSITION_REMOVING);
			logger.debug("Destroying {}", actionID);
		}
		finally{
			if(a!=null){
				jobs.put(actionID, a);
				dispatcher.process(actionID);
			}
		}
	}

	@Override
	public int getAllJobs(){
		try {
			return jobs.size();
		} catch (Exception e) {
			LogUtil.logException("Error getting number of actions", e, logger);
			return -1;
		}
	}

	@Override
	public int getDoneJobs(){
		try {
			return jobs.size(ActionStatus.DONE);
		} catch (Exception e) {
			return -1;
		}
	}	

	public int getTotalJobsOnSystem(){
		try {
			return xnjs.get(IExecutionSystemInformation.class).getTotalNumberOfJobs();
		} catch (Exception e) {
			logger.warn("Could not get number of jobs on the system.",e);
			return -1;
		}
	}	

	@Override
	public boolean getIsAcceptingNewActions(){
		return isAcceptingNewActions;
	}

	@Override
	public void stopAcceptingNewActions(){
		isAcceptingNewActions=false;
	}

	@Override
	public void startAcceptingNewActions(){
		isAcceptingNewActions=true;
	}

	public boolean isPaused() {
		return isPaused;
	}

	public void pauseProcessing() {
		isPaused=true;
	}

	public void resumeProcessing() {
		isPaused=false;
	}

	public IActionStore getActionStore(){
		return jobs;
	}

	@Override
	public void handleEvent(final XnjsEvent event) throws ExecutionException {
		final String actionID=event.getActionID();
		Runnable r=new Runnable(){
			public void run(){
				if(xnjs.isStopped())return;
				if(event instanceof CallbackEvent) {
					Action a=null;
					try{
						a = getActionForUpdate(actionID);
						if(a!=null){
							((CallbackEvent)event).callback(a, xnjs);
							if(event instanceof ContinueProcessingEvent) {
								dispatcher.process(actionID);
							}
						}
					}catch(Exception ex){
						// timeout is not really an error, so do not log it as such
						if( !(ex instanceof TimeoutException)){	
							LogUtil.logException("Error processing callback for action  <"+actionID+">", ex, logger);
						}
						xnjs.getScheduledExecutor().schedule(this, 5000, TimeUnit.MILLISECONDS);
					}
					finally{
						if(a!=null) {
							try {
								jobs.put(actionID, a);
							} catch (Exception e) {
								throw new RuntimeException(e);
							}
						}
					}
				}
				else if(event instanceof ContinueProcessingEvent) {
					dispatcher.process(actionID);
				}
			}
		};
		xnjs.getScheduledExecutor().schedule(r, 200, TimeUnit.MILLISECONDS);

	}

	@Override
	public void scheduleEvent(final XnjsEvent event, int time, TimeUnit units) throws RejectedExecutionException{
		Runnable r=new Runnable(){
			public void run(){
				try{
					handleEvent(event);
				}catch(Exception ex){
					logger.error("Error processing event", ex);
				}
			}
		};
		xnjs.getScheduledExecutor().schedule(r,time,units);
	}

}
