package eu.unicore.xnjs.ems;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.Logger;

import eu.unicore.util.Log;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.ems.event.ContinueProcessingEvent;
import eu.unicore.xnjs.ems.event.StateChangeEvent;
import eu.unicore.xnjs.ems.event.XnjsEvent;
import eu.unicore.xnjs.persistence.IActionStore;
import eu.unicore.xnjs.util.LogUtil;

/**
 * The main worker class that takes active actions from a queue
 * and initiates processing, depending on the action type<br/>
 * 
 * @author schuller
 */
public class ActionRunner extends Thread {

	private static final Logger logger=LogUtil.getLogger(LogUtil.XNJS,ActionRunner.class);

	private final XNJS xnjs;

	private final InternalManager mgr;

	private final ActionStateChangeListener changeListener;

	private final BlockingQueue<String> transfer = new ArrayBlockingQueue<>(1);

	private final IActionStore jobs;

	private final Dispatcher dispatcher;

	volatile boolean isInterrupted=false;

	private static final AtomicInteger count = new AtomicInteger(0);

	public ActionRunner(XNJS xnjs, Dispatcher dispatcher) {
		super();
		this.xnjs = xnjs;
		this.dispatcher = dispatcher;
		this.mgr = xnjs.get(InternalManager.class);
		this.changeListener = xnjs.get(ActionStateChangeListener.class, true);
		this.jobs = xnjs.getActionStore("JOBS");
		int n = count.incrementAndGet();
		super.setName("XNJS-"+xnjs.getID()+"-JobRunner-"+n);
		logger.debug("Action runner thread {} starting", n);
	}

	public synchronized void interrupt() {
		if(isInterrupted)return;
		isInterrupted=true;
		logger.debug("{} stopping", getName());
		count.decrementAndGet();
		super.interrupt();
	}

	public void process(String actionID){
		transfer.offer(actionID);
	}
	
	/**
	 *  runs actions<br>
	 *  On every iteration,
	 *  <ul>
	 *  <li> get an action from the Manager</li>
	 *  <li> get the processing chain for it</li>
	 *  <li> call process() on the first Processor of the chain</li>
	 *  <li> store and trigger the next processing iteration</li>
	 *   </ul> 
	 */
	public void run() {
		logger.info("Worker {} starting.", getName());
		try{
			while(!isInterrupted){
				while(mgr.isPaused())Thread.sleep(300);
				Action a=null;
				String id = transfer.poll(100, TimeUnit.MILLISECONDS);
				if(id != null){
					logger.debug("Processing {}", id);
					try{
						a = jobs.getForUpdate(id);
					}catch(TimeoutException te) {
						// not an issue - try again later
						dispatcher.process(id);
					}catch(Exception eex){
						logger.warn("Can't get action for processing",eex);
					}
					if(a!=null){
						doProcess(a);
					}
					dispatcher.notifyAvailable(this);
				}
			}
		}catch(Exception ie){
			logger.info("Worker {} stopped.", getName());
			return;
		}
	}

	private final List<XnjsEvent> events = new ArrayList<>();

	/**
	 * if the action is not delayed, this will invoke the processing chain
	 * for the action, put it back into persistence, and trigger the next
	 * iteration, based on the action state
	 */
	private void doProcess(Action a){
		int status = a.getStatus();
		events.clear();
		try{
			if(isEligible(a)) {
				LogUtil.fillLogContext(a);
				Processor p = xnjs.createProcessor(a.getType());
				logger.trace("Processing Action <{}> in status {}", a.getUUID(), ActionStatus.toString(a.getStatus()));
				p.process(a);
				logger.trace("New status for Action <{}>: {}", a.getUUID(), ActionStatus.toString(a.getStatus()));
				events.addAll(checkNotify(a, status));
			}
			else {
				sendToSleep(a);
			}
		}catch(Throwable pe){
			// set status first to make sure we trigger notifications
			a.setStatus(ActionStatus.DONE);
			a.getResult().setStatusCode(ActionResult.NOT_SUCCESSFUL);
			a.getResult().setErrorMessage(Log.createFaultMessage("Processing failed", pe));
			events.addAll(checkNotify(a, status));
			if(a.getParentActionID()!=null){
				events.add(new ContinueProcessingEvent(a.getParentActionID()));
			}
		}
		finally{
			finishProcessing(a);
			if(events.size()>0) {
				for(XnjsEvent event: events) {
					mgr.handleEvent(event);
				}
			}
			LogUtil.clearLogContext();
		}
	}

	private boolean isEligible(Action a){
		return a.getNotBefore()<System.currentTimeMillis();
	}

	private void sendToSleep(Action a){
		a.setWaiting(true);
		long delay = a.getNotBefore()-System.currentTimeMillis();
		if(delay<=0)delay = 5000;
		mgr.scheduleEvent(new ContinueProcessingEvent(a.getUUID()), delay, TimeUnit.MILLISECONDS);
	}

	private List<XnjsEvent> checkNotify(Action a, int oldStatus) {
		int newStatus = a.getStatus();
		List<XnjsEvent> events = new ArrayList<>();
		if(newStatus!=oldStatus){
			if(changeListener!=null) {
				events.add(new StateChangeEvent(a.getUUID(), newStatus, changeListener));
			}
			if(newStatus==ActionStatus.DONE && a.getParentActionID()!=null){
				events.add(new ContinueProcessingEvent(a.getParentActionID()));
			}
		}
		return events;
	}

	private void finishProcessing(Action a){
		try{
			String id = a.getUUID();
			if(a.getStatus()== ActionStatus.DESTROYED) {
				jobs.remove(a);
				logger.debug("[{}] Action is destroyed.", id);
			}
			else {
				if(a.getStatus()==ActionStatus.DONE){
					a.setWaiting(false);
					jobs.put(id, a);
				}
				else {
					jobs.put(id, a);
					if(!a.isWaiting()){
						dispatcher.process(id);
					}
				}
			}
		}
		catch(Exception e){
			logger.error("Persistence problem",e);
		}
	}

}
