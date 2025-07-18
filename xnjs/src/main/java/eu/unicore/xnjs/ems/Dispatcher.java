package eu.unicore.xnjs.ems;

import java.io.Serializable;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;

import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.XNJSProperties;
import eu.unicore.xnjs.persistence.IActionStore;
import eu.unicore.xnjs.util.LogUtil;

public class Dispatcher extends Thread {

	private static final Logger logger=LogUtil.getLogger(LogUtil.XNJS, Dispatcher.class); 

	private final IActionStore jobs;

	private ActionRunner[] workers;

	private BlockingQueue<ActionRunner> availableRunners;

	protected final BlockingQueue<QueueEntry> workQueue;

	volatile boolean isInterrupted = false;

	private final XNJS xnjs;

	public Dispatcher(XNJS xnjs) throws Exception {
		this.xnjs = xnjs;
		this.jobs = xnjs.getActionStore("JOBS");
		this.workQueue = new DelayQueue<>();
		this.setName("XNJS-"+xnjs.getID()+"-Dispatcher");
	}

	/**
	 * fill queue from database content
	 */
	public void refillQueue(){
		try{
			for(String i: jobs.getActiveUniqueIDs()){
				QueueEntry q = new QueueEntry(i);
				if(!workQueue.contains(q)){
					workQueue.add(q);
				}
			}
		}
		catch(Exception re){
			LogUtil.logException("Error refilling work queue.", re, logger);
		}
	}

	private void setup() {
		int nWorkers=xnjs.getXNJSProperties().getIntValue(XNJSProperties.XNJSWORKERS);
		workers=new ActionRunner[nWorkers];
		availableRunners = new ArrayBlockingQueue<>(nWorkers);
		for(int i=0;i<workers.length;i++){
			workers[i] = new ActionRunner(xnjs, this);
			workers[i].start();
			notifyAvailable(workers[i]);
		}
		logger.info("Started {} worker threads.", workers.length);
		refillQueue();
	}

	public void notifyAvailable(ActionRunner runner){
		availableRunners.offer(runner);
	}

	public synchronized void interrupt() {
		if(isInterrupted)return;
		isInterrupted = true;
		logger.debug("{} stopping", getName());
		super.interrupt();
	}

	public void run(){
		logger.info("Starting dispatcher thread");
		try{
			setup();
		}catch(Exception ex){
			throw new RuntimeException(ex);
		}
		try{
			while(!isInterrupted){
				ActionRunner r = availableRunners.take();
				QueueEntry q = workQueue.take();
				while(q.getDelay(TimeUnit.MILLISECONDS)>0)Thread.sleep(50);
				r.process(q.getActionID());
			}
		}catch(Exception ie){}
		logger.info("{} stopped.", getName());
	}

	public void process(String actionID){
		workQueue.offer(new QueueEntry(actionID));
	}

	public static class QueueEntry implements Delayed, Serializable {

		private static final long serialVersionUID = 1l;

		/**
		 * minimum waiting time (milliseconds) before the Action is processed again
		 */
		public static final int QUEUE_DELAY=50;

		final String actionID;

		final long lastAccessed;

		public QueueEntry(String actionID){
			this.actionID = actionID;
			this.lastAccessed = System.currentTimeMillis();
		}

		public long getDelay(TimeUnit unit) {
			return unit.convert(QUEUE_DELAY-System.currentTimeMillis()+lastAccessed, TimeUnit.MILLISECONDS);
		}

		public int compareTo(Delayed o) {
			return (int)(lastAccessed-((QueueEntry)o).lastAccessed);
		}

		public String getActionID(){
			return actionID;
		}

		@Override
		public int hashCode() {
			return actionID.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			QueueEntry other = (QueueEntry) obj;
			return this.actionID.equals(other.actionID);
		}
	}
}