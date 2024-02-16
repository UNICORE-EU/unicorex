package eu.unicore.xnjs.ems.event;

import org.json.JSONObject;

import eu.unicore.xnjs.ems.Action;

/**
 * Invoked when the XNJS wants to notify the client about
 * an interesting status change on the backend
 * 
 * @author schuller
 */
public interface INotificationSender {

	/**
	 * @param msg - message to send
	 * @param action
	 * @throws Exception
	 */
	public void send(JSONObject msg, final Action action) throws Exception;

}
