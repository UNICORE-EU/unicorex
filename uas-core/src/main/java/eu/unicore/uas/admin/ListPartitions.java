package eu.unicore.uas.admin;

import java.util.Collection;
import java.util.Map;

import eu.unicore.services.Kernel;
import eu.unicore.services.admin.AdminAction;
import eu.unicore.services.admin.AdminActionResult;
import eu.unicore.uas.xnjs.XNJSFacade;
import eu.unicore.util.Log;
import eu.unicore.xnjs.idb.Partition;
import eu.unicore.xnjs.tsi.IExecutionSystemInformation;

/**
 * {@link AdminAction} which prints a list of available BSS Partitions 
 */
public class ListPartitions implements AdminAction {

	@Override
	public String getName() {
		return "ListPartitions";
	}

	@Override
	public AdminActionResult invoke(Map<String, String> params, Kernel kernel) {
		boolean success = true;
		String message = null;
		String xnjsReference = params.get("xnjsReference");
		Collection<Partition> partitions = null;
		try{
			message = "Partitions";
			if(xnjsReference!=null){
				message+=" on XNJS "+xnjsReference;
			}
			partitions = getPartitions(xnjsReference, kernel);
		}catch(Exception ex){
			success = false;
			message = Log.createFaultMessage("Getting partitions failed", ex);
		}
		AdminActionResult res = new AdminActionResult(success,message);
		for(Partition p: partitions) {
			res.addResult(p.getName(), p.toString());
		}
		return res;
	}

	private Collection<Partition> getPartitions(String xnjsReference, Kernel kernel) throws Exception {
		return XNJSFacade.get(xnjsReference, kernel).getXNJS().
				get(IExecutionSystemInformation.class).getPartitionInfo();
	}

	@Override
	public String getDescription() {
		return "parameters: [xnjsReference]";
	}
}