package eu.unicore.uas.admin;

import java.util.Map;

import eu.unicore.services.Kernel;
import eu.unicore.services.admin.AdminAction;
import eu.unicore.services.admin.AdminActionResult;
import eu.unicore.uas.xnjs.XNJSFacade;
import eu.unicore.util.Log;
import eu.unicore.xnjs.ems.Action;

/**
 * {@link AdminAction} which prints details about an XNJS action 
 */
public class ShowJobDetails implements AdminAction {

	@Override
	public String getName() {
		return "ShowJobDetails";
	}

	@Override
	public AdminActionResult invoke(Map<String, String> params, Kernel kernel) {
		String requestedID=params.get("jobID");
		boolean success=true;
		String message = null;
		StringBuilder info = new StringBuilder();
		String xnjsReference = params.get("xnjsReference");
		try{
			if(requestedID != null){
				message = "Job information for "+requestedID;
				if(xnjsReference!=null){
					message+=" on XNJS "+xnjsReference;
				}
				getSingleActionInfo(info, requestedID, xnjsReference, kernel);
			}
			else{
				message = "XNJS Action list";
				if(xnjsReference!=null){
					message+=" on XNJS "+xnjsReference;
				}
				getActionList(info, xnjsReference, kernel);
			}
		}catch(Exception ex){
			success = false;
			message = Log.createFaultMessage("Getting info failed", ex);
		}
		AdminActionResult res=new AdminActionResult(success,message);
		res.addResult("Info", info.toString());
		return res;

	}

	private void getSingleActionInfo(StringBuilder info, String requestedID, String xnjsReference,  Kernel kernel)
			throws Exception {
		Action a = XNJSFacade.get(xnjsReference, kernel).getAction(requestedID);
		if(a!=null){
			info.append(a.toString());
			info.append("Processing context: ");
			info.append(a.getProcessingContext());
			if(a.getApplicationInfo()!=null){
				info.append("\nApplication Info: ");
				info.append(a.getApplicationInfo());
			}
			info.append("\nJob log: ");
			for(String l: a.getLog()){
				info.append("\n").append(l);
			}
		}
		else{
			info.append("No such job!");
		}
	}

	private void getActionList(StringBuilder info, String xnjsReference,  Kernel kernel) throws Exception {
		for(String id: XNJSFacade.get(xnjsReference, kernel).listJobIDs(null)){
			info.append(id).append("\n");
		}
	}

	@Override
	public String getDescription() {
		return "parameters: [jobID], [xnjsReference]";
	}
}