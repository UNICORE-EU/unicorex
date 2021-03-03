package de.fzj.unicore.xnjs.ems;

import java.io.Serializable;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.persist.PersistenceException;
import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.XNJSProperties;
import de.fzj.unicore.xnjs.persistence.IActionStore;
import de.fzj.unicore.xnjs.util.LogUtil;

public class Dispatcher extends Thread {

	private static final Logger logger=LogUtil.getLogger(LogUtil.XNJS, Dispatcher.class); 

	private IActionStore jobs;

	private JobRunner[] workers;

	private BlockingQueue<JobRunner> availableRunners;

	protected final BlockingQueue<QueueEntry> workQueue;

	volatile boolean isInterrupted=false;

	private final XNJS xnjs;

	public Dispatcher(XNJS xnjs) throws Exception {
		this.xnjs = xnjs;
		this.jobs = xnjs.getActionStore("JOBS");
		this.workQueue = createQueue();
		this.setName("XNJS-"+xnjs.getID()+"-Dispatcher");
	}

	private BlockingQueue<QueueEntry> createQueue(){
		if(xnjs!=null && xnjs.isClusterEnabled()){
			return xnjs.getCluster().getQueue("XNJS.action_id_queue");
		}
		else{
			return new DelayQueue<QueueEntry>();
		}
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
		}catch(PersistenceException pe){
			LogUtil.logException("Can't read IDs from persistence.", pe, logger);
		}
		catch(RuntimeException re){
			LogUtil.logException("Error refilling work queue.", re, logger);
		}
	}

	private void setup() throws Exception {
		int nWorkers=xnjs.getXNJSProperties().getIntValue(XNJSProperties.XNJSWORKERS);
		workers=new JobRunner[nWorkers];
		availableRunners = new ArrayBlockingQueue<>(nWorkers);
		for(int i=0;i<workers.length;i++){
			workers[i]=new JobRunner(xnjs, this);
			workers[i].start();
			notifyAvailable(workers[i]);
		}
		logger.info("Started {} worker threads.", workers.length);
		refillQueue();
	}

	public void notifyAvailable(JobRunner runner){
		availableRunners.offer(runner);
	}

	public void interrupt() {
		isInterrupted=true;
		logger.debug("{} stopping", getName());
		super.interrupt();
	}

	public void run(){
		logger.info("Starting dispatcher thread");
		try{
			setup();
		}catch(Exception ex){
			logger.error("", ex);
			throw new RuntimeException(ex);
		}
		try{
			while(!isInterrupted){
				// get a free JobRunner
				JobRunner r = availableRunners.take();
				// get the next queue element to process
				QueueEntry q = workQueue.take();
				while(q.getDelay(TimeUnit.MILLISECONDS)>0)Thread.sleep(50);
				r.process(q.getActionID());
			}
		}catch(InterruptedException ie){
			logger.info("{} stopped.", getName());
		}
	}

	public void process(String actionID){
		workQueue.offer(new QueueEntry(actionID));
	}

	public static class QueueEntry implements Delayed, Serializable {

		private static final long serialVersionUID = 1l;

		/**
		 * minimum waiting time (milliseconds) before an Action is processed again
		 */
		public static final int QUEUE_DELAY=50;

		final String actionID;

		final long lastAccessed;

		/**
		 * @param actionID
		 * @param lastAccessed - in millis
		 */
		public QueueEntry(String actionID, long lastAccessed){
			this.actionID=actionID;
			this.lastAccessed=lastAccessed;
		}

		public QueueEntry(String actionID){
			this(actionID,System.currentTimeMillis());
		}

		public long getDelay(TimeUnit unit) {
			return unit.convert(QUEUE_DELAY-System.currentTimeMillis()+lastAccessed, TimeUnit.MILLISECONDS);
		}

		public int compareTo(Delayed o) {
			return (int)(lastAccessed-((QueueEntry)o).lastAccessed);
		}

		public String toString(){
			return actionID+"[remaining:"+getDelay(TimeUnit.MILLISECONDS)+"]";
		}

		public String getActionID(){
			return actionID;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((actionID == null) ? 0 : actionID.hashCode());
			return result;
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
			if (actionID == null) {
				if (other.actionID != null)
					return false;
			} else if (!actionID.equals(other.actionID))
				return false;
			return true;
		}

	}

}

