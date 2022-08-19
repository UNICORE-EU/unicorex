package de.fzj.unicore.xnjs.ems.event;

import de.fzj.unicore.xnjs.ems.Action;

/**
 * an event within the XNJS
 *  
 * @author schuller
 */
public interface XnjsEvent {

	/**
	 * the action ID this event refers to
	 */
	public String getActionID();
	
	/**
	 * allows to perform some operation on the action when
	 * the event is handled
	 * 
	 * @param action - the action (never null)
	 */
	public void callback(final Action action);
}
