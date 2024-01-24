package eu.unicore.xnjs.ems.event;

import java.util.List;

import org.json.JSONObject;

import eu.unicore.util.Log;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.ems.Action;

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
		for(String trigger: triggers) {
			if(newBssStatus.matches(trigger)) {
				INotificationSender notificationSender = xnjs.get(INotificationSender.class, true);
				if(notificationSender!=null) {
					try {
						JSONObject msg = new JSONObject();
						msg.put("bssStatus", newBssStatus);
						notificationSender.send(msg, action);
						break;
					}catch(Exception ex) {
						action.addLogTrace(Log.createFaultMessage("Could not send notification(s)", ex));
					}
				}
				else {
					action.addLogTrace("Notification(s) not sent: no notification sender configured.");
				}
			}
		}
	}

}