package de.fzj.unicore.xnjs.ems.event;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.Action;

/**
 * an event requiring a callback
 *  
 * @author schuller
 */
public interface CallbackEvent extends XnjsEvent {
	
	/**
	 * allows to perform some operation on the action when
	 * the event is handled
	 * 
	 * @param action - the action (never null)
	 * @param xnjs - XNJS
	 */
	public void callback(final Action action, final XNJS xnjs);
}
