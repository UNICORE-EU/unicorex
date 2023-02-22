package de.fzj.unicore.uas.xnjs;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.json.JSONObject;

import de.fzj.unicore.uas.rest.Jobs;
import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.ActionResult;
import de.fzj.unicore.xnjs.ems.ActionStateChangeListener;
import de.fzj.unicore.xnjs.ems.ActionStatus;
import de.fzj.unicore.xnjs.ems.event.INotificationSender;
import eu.unicore.util.Log;

@Singleton
public class RESTNotificationSender implements ActionStateChangeListener {

	final int[] defaultTriggers = new int[] {ActionStatus.RUNNING, ActionStatus.DONE};
	
	private final XNJS xnjs;
	
	@Inject
	public RESTNotificationSender(XNJS xnjs) {
		this.xnjs = xnjs;
	}
	
	protected boolean isTrigger(int actionState, int[]triggers) {
		for(int i: triggers) {
			if(i==actionState)return true;
		}
		return false;
	}
	
	@Override
	public void stateChanged(Action action) {
		if(action==null || action.getNotificationURLs()==null || action.getNotificationURLs().isEmpty())return;
		
		int status = action.getStatus();
		if(!isTrigger(status, defaultTriggers))return;
		
		JSONObject message = new JSONObject();
		ActionResult result = action.getResult();
		message.put("status", Jobs.convertStatus(action.getStatus(),result.isSuccessful()));
		message.put("statusMessage", "");
		Integer exitCode = action.getExecutionContext().getExitCode();
		if(exitCode!=null){
			message.put("exitCode", String.valueOf(exitCode.longValue()));
		}
		if(!result.isSuccessful()){
			String errorMessage = result.getErrorMessage();
			if(errorMessage==null)errorMessage="";
			message.put("statusMessage", errorMessage);
		}
		try {
			INotificationSender sender = xnjs.get(INotificationSender.class);
			sender.send(message, action);
		}catch(Exception ex) {
			action.addLogTrace(Log.createFaultMessage("Could not send notification.", ex));
		}
	}
	
}
