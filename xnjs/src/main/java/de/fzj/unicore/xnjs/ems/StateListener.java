package de.fzj.unicore.xnjs.ems;

/**
 * use to receive notifications when an action enters a given state
 */
public abstract class StateListener implements ActionStateChangeListener {
	
	private Integer status;
	
	/**
	 * construct a StateListener that accepts notifications when the action
	 * enters state 'status'
	 * 
	 * @see de.fzj.unicore.xnjs.ems.ActionStatus
	 * @param status
	 */
	public StateListener(Integer status){
		this.status=status;
	}
	
	public boolean accept(Integer newState) {
		if(status.equals(newState))return true;
		return false;
	}

	public abstract void stateChanged(String id, Integer newState);
	
}
