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
	 * @param msg - base message already containing the new "bssStatus"
	 * @param action - NOTE this is already locked with getForUpdate()
	 * @throws Exception
	 */
	public void send(JSONObject msg, final Action action) throws Exception;

}
