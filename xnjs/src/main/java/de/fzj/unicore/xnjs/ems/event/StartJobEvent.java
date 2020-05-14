package de.fzj.unicore.xnjs.ems.event;

import org.apache.log4j.Logger;

import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.ActionStatus;
import de.fzj.unicore.xnjs.util.LogUtil;

public class StartJobEvent extends ContinueProcessingEvent {

	private static final Logger logger=LogUtil.getLogger(LogUtil.JOBS, StartJobEvent.class);

	@Override
	public void callback(Action action) {
		if(action==null){
			logger.error("Null action in callback.");
			return;
		}
		//check status
		int s=action.getStatus();
		if(!ActionStatus.canRun(s)){
			return;
		}
		//make it PENDING so the JobRunner will submit it
		if(s==ActionStatus.READY){
			action.setStatus(ActionStatus.PENDING);
		}
		else{
			action.getProcessingContext().put(Action.AUTO_SUBMIT, Boolean.TRUE);
			action.setDirty();
		}
	}

	public StartJobEvent(String actionID) {
		super(actionID);
	}

}
