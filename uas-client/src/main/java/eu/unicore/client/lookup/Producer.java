package eu.unicore.client.lookup;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public interface Producer<T> extends Runnable {

	/**
	 * setup the producer
	 * 
	 * @param target - the target queue for results
	 * @param runCount - a counter to allow the supervising {@link Lister} to track whether the
	 * producer is still running. It must be <b>decremented</b> when the run() method finishes
	 */
	public void init(BlockingQueue<T>target, AtomicInteger runCount);
	
}
