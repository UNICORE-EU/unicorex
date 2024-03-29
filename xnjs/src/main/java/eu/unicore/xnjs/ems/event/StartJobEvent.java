package eu.unicore.xnjs.ems.event;

import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.ems.Action;
import eu.unicore.xnjs.ems.ActionStatus;

public class StartJobEvent extends ContinueProcessingEvent implements CallbackEvent {

	public StartJobEvent(String actionID) {
		super(actionID);
	}

	@Override
	public void callback(final Action action, final XNJS xnjs) {
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
