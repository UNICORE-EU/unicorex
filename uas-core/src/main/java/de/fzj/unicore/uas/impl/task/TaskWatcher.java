package de.fzj.unicore.uas.impl.task;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import eu.unicore.services.Kernel;

/**
 * monitors a running task and update result if it is done
 *
 * @author schuller
 */
public abstract class TaskWatcher<T>  implements Runnable{

	protected final String taskID;

	protected final Future<T>future;

	protected final Kernel kernel;
	
	protected int errCount=0;
	
	public TaskWatcher(Future<T>future, String taskID, Kernel kernel){
		this.taskID=taskID;
		this.future=future;
		this.kernel=kernel;
	}
	
	public void run(){
		if(future.isDone()){
			Map<String, String> result = new HashMap<>();
			try{
				T stats = future.get();
				result = createResult(stats);
			}
			catch(Exception ex){}
			try{
				TaskImpl.putResult(kernel, taskID, result, "OK", 0);
			}catch(Exception ex){
				if(errCount<3){
					errCount++;
					reschedule();
				}
				else{
					throw new RuntimeException("Cannot store result, giving up, sorry.");
				}
			}
		}
		else{
			reschedule();
		}
	}
	
	protected void reschedule(){
		kernel.getContainerProperties().getThreadingServices().getScheduledExecutorService().
		schedule(this, 5000, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * create the result map
	 */
	protected abstract Map<String, String> createResult(T taskResult);
	
}
