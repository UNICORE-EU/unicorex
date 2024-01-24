package eu.unicore.xnjs.ems.event;

import eu.unicore.xnjs.ems.ExecutionException;

/**
 * handle events
 
 * @author schuller
 */
public interface EventHandler {
	
	/**
	 * handle an event
	 */
	public void handleEvent(XnjsEvent event) throws ExecutionException;
	
}
