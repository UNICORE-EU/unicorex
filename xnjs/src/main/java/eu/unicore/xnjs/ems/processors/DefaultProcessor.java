package eu.unicore.xnjs.ems.processors;

import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;

import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.ems.Action;
import eu.unicore.xnjs.ems.ActionResult;
import eu.unicore.xnjs.ems.ActionStatus;
import eu.unicore.xnjs.ems.ProcessingException;
import eu.unicore.xnjs.ems.Processor;
import eu.unicore.xnjs.ems.event.ContinueProcessingEvent;
import eu.unicore.xnjs.util.LogUtil;

/**
 * default Processor with empty handle*() methods, useful when
 * creating your own processors
 * 
 * @author schuller
 */
public class DefaultProcessor extends Processor {
	
	protected static final Logger logger=LogUtil.getLogger(LogUtil.XNJS,DefaultProcessor.class);
	
	protected final static String KEY_FS_CHECKED_FLAG="FILE_SYSTEM_CHECKED";
	
	protected final static String KEY_FIRST_STAGEOUT_FAILURE="FIRST_STAGEOUT_FAILURE";
	
	protected final static String KEY_STATUS_ON_UNPAUSE="__STATUS_ON_UNPAUSE";
	
	/**
	 * By default give the (network) file system 10 seconds to show files 
	 */
	public static final int DEFAULT_FS_TIMEOUT = 10;
	
	public DefaultProcessor(XNJS xnjs){
		super(xnjs);
	}

	protected void handleAborting()throws ProcessingException{
		action.setStatus(ActionStatus.DONE);
		action.setResult(new ActionResult(ActionResult.USER_ABORTED, "Job aborted."));
	}
	
	protected void handlePausing()throws ProcessingException{
		Integer oldStatus = action.getStatus();
		action.getProcessingContext().put(KEY_STATUS_ON_UNPAUSE, oldStatus);
		action.setStatus(ActionStatus.PAUSED);
	}
	
	protected void handlePaused()throws ProcessingException{
		sleep(5, TimeUnit.SECONDS);
	}
	
	protected void handleResuming()throws ProcessingException{
		Integer oldStatus = action.getProcessingContext().getAs(KEY_STATUS_ON_UNPAUSE, Integer.class);
		action.setStatus(oldStatus);
	}

	/**
	 * utility method which sets the action status to DONE and the result
	 * to NOT_SUCCESSFUL with the reason given
	 * @param reason
	 */
	protected void setToDoneAndFailed(String reason){
		action.setStatus(ActionStatus.DONE);
		action.setResult(new ActionResult(ActionResult.NOT_SUCCESSFUL,reason));
	}
	
	/**
	 * send the action to sleep for the specified time, by
	 * setting the "waiting" flag and scheduling a "continue" event
	 * @param amount
	 * @param units
	 */
	protected void sleep(int amount, TimeUnit units){
		action.setWaiting(true);
		manager.scheduleEvent(new ContinueProcessingEvent(action.getUUID()), amount, units);
	}

	@Deprecated
	protected void sleep(int millis){
		sleep(millis, TimeUnit.MILLISECONDS);
	}

	/**
	 * store the current system time as a Long object under the given key
	 * @param key
	 */
	protected void storeTimeStamp(String key){
		action.getProcessingContext().put(key, Long.valueOf(System.currentTimeMillis()));
		action.setDirty();
	}

	/**
	 * get the Long time value stored under the given key
	 * @param key
	 */
	protected Long getTimeStamp(String key){
		return (Long)action.getProcessingContext().get(key);
	}

	/**
	 * allow to set action for unit testing
	 * @param a - the action
	 */
	public void setAction(Action a){
		this.action=a;
	}

}
