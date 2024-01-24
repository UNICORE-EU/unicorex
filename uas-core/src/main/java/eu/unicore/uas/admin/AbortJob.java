package eu.unicore.uas.admin;

import java.util.Map;

import eu.unicore.services.Kernel;
import eu.unicore.services.admin.AdminAction;
import eu.unicore.services.admin.AdminActionResult;
import eu.unicore.uas.xnjs.XNJSFacade;
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

	private void abortAction(String requestedID, String xnjsReference,  Kernel kernel, boolean cleanup) 
			throws Exception {
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