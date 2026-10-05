package eu.unicore.xnjs.ems.event;

import java.util.List;

import org.json.JSONObject;

import eu.unicore.util.Log;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.ems.Action;
import eu.unicore.xnjs.ems.ActionStateChangeListener;
import eu.unicore.xnjs.ems.ActionStatus;

public class Events {


	/**
	 * start job was requested
	 */
	public static class StartJobEvent extends ContinueProcessingEvent implements CallbackEvent {

		public StartJobEvent(String actionID) {
			super(actionID);
		}

		@Override
		public void callback(final Action action, final XNJS xnjs) {
			int s = action.getStatus();
			if(ActionStatus.canRun(s)){
				//make it PENDING so the JobRunner will submit it
				if(s==ActionStatus.READY){
					action.setStatus(ActionStatus.PENDING);
				}
				else{
					action.getProcessingContext().put(Action.AUTO_SUBMIT, Boolean.TRUE);
					action.setDirty();
				}
			}
		}

	}
	

	/**
	 * job abort was requested
	 */
	public static class AbortJobEvent extends ContinueProcessingEvent implements CallbackEvent {

		public AbortJobEvent(String actionID) {
			super(actionID);
		}

		@Override
		public void callback(final Action action, final XNJS xnjs) {
			int s = action.getStatus();
			if(ActionStatus.canAbort(s)){
				action.addLogTrace("Got 'abort' request.");
				action.setTransitionalStatus(ActionStatus.TRANSITION_ABORTING);
			}
		}

	}


	/**
	 * Notify that the action status has changed
	 */
	public static class StateChangeEvent implements CallbackEvent {

		private final String actionID;

		private final ActionStateChangeListener listener;

		private final int newState;

		public StateChangeEvent(String actionID, int newState, ActionStateChangeListener listener){
			this.actionID = actionID;
			this.listener = listener;
			this.newState = newState;
		}

		public String getActionID() {
			return actionID;
		}

		@Override
		public void callback(final Action action, final XNJS xnjs) {
			listener.stateChanged(action, newState);
		}
	}


	/**
	 * change event if a "raw" (i.e. low-level) BSS status change is detected
	 */
	public static class BssStatusChangeEvent implements CallbackEvent {

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
}
