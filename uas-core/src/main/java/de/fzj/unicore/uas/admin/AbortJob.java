package de.fzj.unicore.uas.admin;

import java.util.Map;

import de.fzj.unicore.uas.xnjs.XNJSFacade;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.admin.AdminAction;
import de.fzj.unicore.wsrflite.admin.AdminActionResult;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import eu.unicore.util.Log;

/**
 * {@link AdminAction} aborting an XNJS action.
 * 
 * If the cleanup parameter is 'true', the action's uspace is destroyed as well
 */
public class AbortJob implements AdminAction {

	@Override
	public String getName() {
		return "AbortJob";
	}

	@Override
	public AdminActionResult invoke(Map<String, String> params, Kernel kernel) {
		String requestedID=params.get("jobID");
		boolean cleanup=Boolean.parseBoolean(params.get("cleanup"));
		boolean success=true;
		String message = null;
		String xnjsReference=params.get("xnjsReference");
		try{
			if(requestedID != null){
				message = "Aborting job "+requestedID;
				if(xnjsReference!=null){
					message+=" on XNJS "+xnjsReference;
				}
				abortAction(requestedID, xnjsReference, kernel, cleanup);
			}
			else{
				success=false;
				message="Please specify the 'jobID' parameter!";
			}
		}catch(Exception ex){
			success=false;
			message=Log.createFaultMessage("Aborting job failed", ex);
		}
		return new AdminActionResult(success,message);
	}

	private void abortAction(String requestedID, String xnjsReference,  Kernel kernel, boolean cleanup) throws ExecutionException {
		if(cleanup){
			XNJSFacade.get(xnjsReference, kernel).destroyAction(requestedID, null);
		}else{
			XNJSFacade.get(xnjsReference, kernel).getManager().abort(requestedID,null);
		}
	}

	@Override
	public String getDescription() {
		return "parameters: jobID, [cleanup:false/true], [xnjsReference]";
	}
}