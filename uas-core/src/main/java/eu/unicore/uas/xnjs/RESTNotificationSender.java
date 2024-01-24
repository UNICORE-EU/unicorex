package eu.unicore.uas.xnjs;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.json.JSONObject;

import eu.unicore.uas.rest.Jobs;
import eu.unicore.util.Log;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.ems.Action;
import eu.unicore.xnjs.ems.ActionResult;
import eu.unicore.xnjs.ems.ActionStateChangeListener;
import eu.unicore.xnjs.ems.ActionStatus;
import eu.unicore.xnjs.ems.event.INotificationSender;

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
		boolean success = action.getResult().isSuccessful();
		int[] triggers = defaultTriggers;
		List<String> userdefinedTriggers = action.getNotifyStates();
		if(userdefinedTriggers!=null) {
			triggers = new int[userdefinedTriggers.size()];
			for(int i=0; i<userdefinedTriggers.size();i++) {
				triggers[i] = notifyFor(userdefinedTriggers.get(i), success);
			}
		}
		if(!isTrigger(status, triggers))return;
		
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
	

	private int notifyFor(String userDefinedTrigger, boolean success){
		switch (userDefinedTrigger){
			case "STAGINGIN": 
				return ActionStatus.PREPROCESSING;
			case "STAGINGOUT": 
				return ActionStatus.POSTPROCESSING;
			case "RUNNING":
				return ActionStatus.RUNNING;
			case "QUEUED":
				return ActionStatus.QUEUED;
			case "READY": 
				return ActionStatus.READY;
			case "SUCCESSFUL":
				return success? ActionStatus.DONE : -1;
			case "FAILED":
				return success? -1 : ActionStatus.DONE;
			default:
				return ActionStatus.UNKNOWN;
		}
	}
}
