package eu.unicore.xnjs.ems;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.Logger;

import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.ems.event.ContinueProcessingEvent;
import eu.unicore.xnjs.ems.event.XnjsEvent;
import eu.unicore.xnjs.persistence.IActionStore;
import eu.unicore.xnjs.util.LogUtil;

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
		logger.debug("Job runner thread {} starting", n);
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
	 *  On every iteration, the Jobrunner will
	 *  <ul>
	 *  <li> get an action from the Manager</li>
	 *  <li> get a processing chain for it</li>
	 *  <li> call process() on the first Processor of the chain</li>
	 *  <li> return the action to the Manager</li>
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
		}catch(Exception ie){
			logger.info("Worker {} stopped.", getName());
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
		XnjsEvent event = null;
		try{
			LogUtil.fillLogContext(a);
			Processor p = xnjs.createProcessor(a.getType());
			logger.trace("Processing Action <{}> in status {}", a.getUUID(), ActionStatus.toString(a.getStatus()));
			p.process(a);
			logger.trace("New status for Action <{}>: {}", a.getUUID(), ActionStatus.toString(a.getStatus()));
			event = checkNotify(a, status);
			mgr.doneProcessing(a);
			if(event!=null)mgr.handleEvent(event);
		}catch(ProcessingException pe){
			try{
				event = checkNotify(a, status);
				mgr.errorProcessing(a, pe);
				if(event!=null)mgr.handleEvent(event);
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

	private XnjsEvent checkNotify(Action a, int oldStatus) {
		int newStatus = a.getStatus();
		XnjsEvent event = null;
		if(newStatus!=oldStatus){
			try {
				if(changeListener!=null) {
					changeListener.stateChanged(a);
				}
			}catch(Exception ex){
				logger.warn("Internal error during state change notification.", ex);
			}
			if(newStatus==ActionStatus.DONE){
				if(a.getParentActionID()!=null){
					try{
						event = new ContinueProcessingEvent(a.getParentActionID());
					}
					catch(Exception ex){
						logger.error("Error sending notification", ex);
					}
				}
			}
		}
		return event;
	}

}
