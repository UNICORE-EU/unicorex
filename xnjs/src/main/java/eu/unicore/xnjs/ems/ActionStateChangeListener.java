package eu.unicore.xnjs.ems;

public interface ActionStateChangeListener {

	/**
	 * notify of action state change
	 */
	public void stateChanged(Action action, int newState);
	
}
