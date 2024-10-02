package eu.unicore.client.lookup;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.BaseServiceClient;

/**
 * Base class for looking up service endpoints. Client code can use the iterator() 
 * method to retrieve the clients for the endpoints that are found.<br/>
 * 
 * There are two modes of operation: lookup can be in the <em>background</em> 
 * or on the current thread. To run in the background,  provide an {@link ExecutorService} 
 * instance in the constructor. 
 * 
 * <b>
 * NOTE:
 * This class can be used in a for-each loop, but: in background mode 
 * it may be that the current value is null to indicate that no result 
 * has become available in the waiting period. 
 * <b/>
 * @author schuller
 */
public class Lister<T extends BaseServiceClient> implements Iterable<T>{

	private volatile boolean running = false;

	private final BlockingQueue<T> queue;

	private final List<Producer<T>>producers = new ArrayList<>();

	private ExecutorService executor;

	private final AtomicInteger runCounter = new AtomicInteger(0);

	protected AddressFilter addressFilter = new AcceptAllFilter();

	private long timeout = 10;

	private TimeUnit unit = TimeUnit.SECONDS;

	public Lister(){
		this(Integer.MAX_VALUE);
	}

	/**
	 * @param maxQueueSize - maximum number of results to keep in memory
	 */
	public Lister(int maxQueueSize){
		this.queue = new LinkedBlockingQueue<T>(maxQueueSize);
	}

	public void setAddressFilter(AddressFilter filter){
		this.addressFilter=filter;
	}

	public AddressFilter getAddressFilter(){
		return addressFilter;
	}
	
	/**
	 * set the timeout that is used the iterator next() method. If no results become available
	 * in that time, the next() method will return null.
	 * 
	 * @param timeout
	 * @param timeUnit
	 */
	public void setTimeout(long timeout, TimeUnit timeUnit){
		this.timeout = timeout;
		this.unit = timeUnit;
	}

	public void addProducer(Producer<T> producer){
		producers.add(producer);
	}
	
	public void setExecutor(ExecutorService executor){
		this.executor = executor;
	}

	/**
	 * Get an iterator to access the lookup results. If not already done using 
	 * the run() method, the lookup will be started. 
	 */
	public Iterator<T> iterator(){
		if(!running){
			run();
		}
		return new Iterator<T>() {

			@Override
			public boolean hasNext() {
				return running && (runCounter.get()>0 || queue.size()>0);
			}

			@Override
			public T next() {
				try{
					return queue.poll(timeout, unit);
				}catch(InterruptedException it){
					running = false;
					return null;
				}
			}

			@Override
			public void remove() {
				// NOP since there is no underlying collection
			}
		};
	}

	public boolean isRunning(){
		return running;
	}

	/**
	 * Start producing results. If no executor was supplied, this method will
	 * block until all producers have finished. Otherwise, use the isRunning() method
	 * to check whether the background tasks have finished.
	 */
	public void run(){
		if(running==true)throw new IllegalStateException();

		if(producers.size()>0){
			running=true;
			runCounter.set(producers.size());
			for(Producer<T> p: producers){
				p.init(queue, runCounter);
				if(executor!=null){
					executor.execute(p);
				}
				else{
					p.run();
				}
			}
		}
	}

	public static class AcceptAllFilter implements AddressFilter {

		@Override
		public boolean accept(Endpoint epr) {
			return true;
		}

		@Override
		public boolean accept(String uri) {
			return true;
		}

		@Override
		public boolean accept(BaseServiceClient client) {
			return true;
		}
		
	}
	
}
