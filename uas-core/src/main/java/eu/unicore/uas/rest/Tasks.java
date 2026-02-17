package eu.unicore.uas.rest;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import eu.unicore.services.rest.Link;
import eu.unicore.services.rest.USEResource;
import eu.unicore.services.rest.impl.ServicesBase;
import eu.unicore.uas.UAS;
import eu.unicore.uas.impl.task.TaskImpl;
import eu.unicore.uas.impl.task.TaskModel;
import jakarta.ws.rs.Path;

/**
 * REST interface to tasks
 *
 * @author schuller
 */
@Path("/tasks")
@USEResource(home=UAS.TASK)
public class Tasks extends ServicesBase {

	@Override
	protected Map<String,Object>getProperties() throws Exception {
		Map<String,Object> props = super.getProperties();
		TaskModel model = getModel();
		props.put("submissionTime", getISODateFormatter().format(model.getSubmissionTime().getTime()));
		props.put("status", model.getStatus());
		props.put("statusMessage", model.getStatusMessage());
		props.put("submissionService", model.getServiceSpec());
		Integer exitCode = model.getExitCode();
		if(exitCode!=null){
			props.put("exitCode", exitCode.intValue());
		}
		props.put("result", model.getResult()!=null? model.getResult() : new HashMap<String, String>());
		return props;
	}

	@Override
	public TaskModel getModel(){
		return (TaskModel)model;
	}

	@Override
	public TaskImpl getResource(){
		return (TaskImpl)resource;
	}

	@Override
	protected JSONObject doHandleAction(String action, JSONObject param) throws Exception {
		TaskImpl task = getResource();
		JSONObject reply = null;
		if("abort".equals(action)){
			task.cancel();
		}
		else {
			reply = super.doHandleAction(action, param);
		}
		return reply;
	}

	@Override
	protected void updateLinks() {
		super.updateLinks();
		links.add(new Link("action:abort",getBaseURL()+"/tasks/"+resource.getUniqueID()+"/actions/abort","Abort"));
	}

}
