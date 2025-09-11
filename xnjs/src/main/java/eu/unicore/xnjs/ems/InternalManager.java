package eu.unicore.xnjs.ems;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import eu.unicore.xnjs.ems.event.EventHandler;
import eu.unicore.xnjs.ems.event.XnjsEvent;


/**
 * this interface is used within the EMS to manage running actions
 * 
 * @author schuller
 */
public interface InternalManager extends EventHandler {

	/**
	 * retrieve an action by id.
	 * @param id
	 * @return Action or null if not found
	 */
	public Action getAction(String id)throws Exception;
	
	/**
	 * construct a sub action and start processing it
	 * 
	 * @param jobDescription - the job description for the subaction
	 * @param type - the sub action type
	 * @param parentAction - the parent action
	 * @param notifyDone - whether to notify the parent when the subaction is done
	 * @return the unique id of the new action
	 */
	public String addSubAction(Serializable jobDescription, String type, Action parentAction, boolean notifyDone)
			throws Exception;
	
	/**
	 * add an internal action
	 */
	public Object addInternalAction(Action a) throws Exception;
	
	/**
	 * has the specified action finished (i.e. is the status = DONE)
	 * @param actionID
	 * @throws ExecutionException when the action is not found
	 */
	public boolean isActionDone(String actionID) throws Exception;
	
	public int getAllJobs();
	
	public int getDoneJobs();
	
	public boolean getIsAcceptingNewActions();
	
	public void startAcceptingNewActions();
	
	public void stopAcceptingNewActions();
	
	public boolean isPaused();
	
	public void pauseProcessing();
	
	public void resumeProcessing();
	
	/**
	 * process the given event after the defined delay
	 * 
	 * @param event - the event to process
	 * @param delay - delay 
	 * @param unit - delay units
	 */
	public void scheduleEvent(final XnjsEvent event, long delay, TimeUnit unit);
}
