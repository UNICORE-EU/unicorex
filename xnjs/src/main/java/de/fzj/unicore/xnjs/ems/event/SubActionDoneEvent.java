package de.fzj.unicore.xnjs.ems.event;


/**
 * an action was waiting for a sub-action to finish can now continue processing
 * 
 * @author schuller
 */
public class SubActionDoneEvent extends ContinueProcessingEvent{

	public SubActionDoneEvent(String actionID) {
		super(actionID);
	}

}
