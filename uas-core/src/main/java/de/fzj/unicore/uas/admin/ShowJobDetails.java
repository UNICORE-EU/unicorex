package de.fzj.unicore.uas.admin;

import java.util.Map;

import de.fzj.unicore.uas.xnjs.XNJSFacade;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.admin.AdminAction;
import de.fzj.unicore.wsrflite.admin.AdminActionResult;
import de.fzj.unicore.xnjs.ems.Action;
import eu.unicore.util.Log;

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
		StringBuilder info= null;
		String xnjsReference=params.get("xnjsReference");
		try{
			if(requestedID != null){
				message = "Job information for "+requestedID;
				if(xnjsReference!=null){
					message+=" on XNJS "+xnjsReference;
				}
				info = getSingleActionInfo(requestedID, xnjsReference, kernel);
			}
			else{
				message = "XNJS Action list";
				if(xnjsReference!=null){
					message+=" on XNJS "+xnjsReference;
				}
				info = getActionList(xnjsReference, kernel);
			}
		}catch(Exception ex){
			success=false;
			message=Log.createFaultMessage("Getting info failed", ex);
		}
		AdminActionResult res=new AdminActionResult(success,message);
		res.addResult("Info", info.toString());
		return res;

	}

	private StringBuilder getSingleActionInfo(String requestedID, String xnjsReference,  Kernel kernel){
		StringBuilder info=new StringBuilder();
		Action a=XNJSFacade.get(xnjsReference, kernel).getAction(requestedID);
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
		return info;
	}

	private StringBuilder getActionList(String xnjsReference,  Kernel kernel) throws Exception {
		StringBuilder info=new StringBuilder();
		for(String id: XNJSFacade.get(xnjsReference, kernel).listJobIDs(null)){
			info.append(id).append("\n");
		}
		return info;
	}

	@Override
	public String getDescription() {
		return "parameters: [jobID], [xnjsReference]";
	}
}