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

	private final BlockingQueue<T> queue;

	private final List<Producer<T>>producers = new ArrayList<>();

	private final ExecutorService executor;

	private final AtomicInteger runCounter = new AtomicInteger(0);

	protected AddressFilter addressFilter = new AcceptAllFilter();

	private long timeout = 10;

	public Lister(ExecutorService executor){
		this(Integer.MAX_VALUE, executor);
	}

	/**
	 * @param maxQueueSize - maximum number of results to keep in memory
	 */
	public Lister(int maxQueueSize, ExecutorService executor){
		this.queue = new LinkedBlockingQueue<T>(maxQueueSize);
		this.executor = executor;
	}

	public void setAddressFilter(AddressFilter filter){
		this.addressFilter=filter;
	}

	public AddressFilter getAddressFilter(){
		return addressFilter;
	}
	
	/**
	 * set the timeout (seconds) that is used the iterator next() method. If no results become available
	 * in that time, the next() method will return null.
	 * 
	 * @param timeout
	 */
	public void setTimeout(long timeout){
		this.timeout = timeout;
	}

	public void addProducer(Producer<T> producer){
		if(isRunning())throw new IllegalStateException();
		producers.add(producer);
	}

	/**
	 * Get an iterator to access the lookup results. If not already done using 
	 * the run() method, the lookup will be started. 
	 */
	public Iterator<T> iterator(){
		if(!isRunning()){
			run();
		}
		return new Iterator<T>() {

			@Override
			public boolean hasNext() {
				return runCounter.get()>0 || queue.size()>0;
			}

			@Override
			public T next() {
				try{
					int i=0;
					// poll/wait for 50 millis, and exit if no more producers
					// are running or the global timeout is reached
					long time_out = 20*timeout;
					while(i<time_out && runCounter.get()>0) {
						T res = queue.poll(50, TimeUnit.MILLISECONDS);
						if(res!=null)return res;
						i++;
					}
					return null;
				}catch(InterruptedException it){
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
		return runCounter.get()>0;
	}

	/**
	 * Start producing results. If no executor was supplied, this method will
	 * block until all producers have finished. Otherwise, use the isRunning() method
	 * to check whether the background tasks have finished.
	 */
	public void run(){
		if(isRunning())throw new IllegalStateException();
		if(producers.size()>0){
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
