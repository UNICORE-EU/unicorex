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