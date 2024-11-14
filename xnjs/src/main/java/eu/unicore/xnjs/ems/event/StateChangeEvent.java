package eu.unicore.xnjs.ems.event;

import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.ems.Action;
import eu.unicore.xnjs.ems.ActionStateChangeListener;

/**
 * Notify that the action status has changed
 *
 * @author schuller
 */
public class StateChangeEvent implements CallbackEvent {

	private final String actionID;

	private final ActionStateChangeListener listener;

	public StateChangeEvent(String actionID, ActionStateChangeListener listener){
		this.actionID = actionID;
		this.listener = listener;
	}

	public String getActionID() {
		return actionID;
	}

	@Override
	public void callback(final Action action, final XNJS xnjs) {
		listener.stateChanged(action);
	}
}