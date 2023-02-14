package de.fzj.unicore.xnjs.ems.event;

import java.util.List;

import org.json.JSONObject;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.Action;
import eu.unicore.util.Log;

/**
 * change event if a "raw" (i.e. low-level) BSS status change is detected
 *
 * @author schuller
 */
public class BssStatusChangeEvent implements CallbackEvent {

	private final String actionID;

	private final String newBssStatus;

	public BssStatusChangeEvent(String actionID, String newBssStatus){
		this.actionID = actionID;
		this.newBssStatus = newBssStatus;
	}

	public String getActionID() {
		return actionID;
	}

	@Override
	public void callback(final Action action, final XNJS xnjs) {
		if(action==null
				|| action.getNotificationURLs()==null 
				|| action.getNotificationURLs().isEmpty())return;
		List<String>triggers = action.getNotifyBSSStates();
		if(triggers.contains(newBssStatus)) {
			INotificationSender notificationSender = xnjs.get(INotificationSender.class);
			try {
				JSONObject msg = new JSONObject();
				msg.put("bssStatus", newBssStatus);
				notificationSender.send(msg, action);
			}catch(Exception ex) {
				action.addLogTrace(Log.createFaultMessage("Could not send notification(s)", ex));
			}
		}
	}

}