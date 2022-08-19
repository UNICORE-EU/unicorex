package de.fzj.unicore.xnjs.ems.event;

import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.ActionStatus;

public class StartJobEvent extends ContinueProcessingEvent {

	public StartJobEvent(String actionID) {
		super(actionID);
	}

	@Override
	public void callback(Action action) {
		int s=action.getStatus();
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
