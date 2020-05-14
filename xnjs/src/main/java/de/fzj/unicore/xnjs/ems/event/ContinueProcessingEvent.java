package de.fzj.unicore.xnjs.ems.event;

import de.fzj.unicore.xnjs.ems.Action;

/**
 * an action was waiting for an event, and can now continue processing
 * 
 * @author schuller
 */
public class ContinueProcessingEvent implements XnjsEvent {

	private final String actionID;
	
	
	public ContinueProcessingEvent(String actionID){
		this.actionID=actionID;
	}
	
	public String getActionID() {
		return actionID;
	}

	public void callback(Action action){
		
	}
	
}
