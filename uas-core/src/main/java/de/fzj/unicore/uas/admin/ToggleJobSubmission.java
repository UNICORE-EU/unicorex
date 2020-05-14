package de.fzj.unicore.uas.admin;

import java.util.Map;

import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.impl.tss.TargetSystemHomeImpl;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.admin.AdminAction;
import de.fzj.unicore.wsrflite.admin.AdminActionResult;

/**
 * {@link AdminAction} for enabling/disabling job submission. The admin can pass in 
 * a "high message" that will be displayed to the user when she tries to submit
 * to a disabled TSS service
 */
public class ToggleJobSubmission implements AdminAction {

	@Override
	public String getName() {
		return "ToggleJobSubmission";
	}

	@Override
	public AdminActionResult invoke(Map<String, String> params, Kernel kernel) {
		String highMessage=params.get("message");
		boolean success=true;
		String message=null;
		TargetSystemHomeImpl th=(TargetSystemHomeImpl)kernel.getHome(UAS.TSS);
		if(th==null){
			success=false;
			message="No target system service available at this site!";
		}
		else{
			boolean enabled=th.isJobSubmissionEnabled();
			th.setJobSubmissionEnabled(!enabled);
			if(highMessage!=null){
				th.setHighMessage(highMessage);
			}
			else{
				th.setHighMessage(TargetSystemHomeImpl.DEFAULT_MESSAGE);
			}
			enabled=th.isJobSubmissionEnabled();
			message=enabled?"OK - job submission is now enabled" : "OK - job submission is disabled";
		}
		return new AdminActionResult(success,message);
	}
	
	@Override
	public String getDescription() {
		return "parameters: [message]";
	}

}
