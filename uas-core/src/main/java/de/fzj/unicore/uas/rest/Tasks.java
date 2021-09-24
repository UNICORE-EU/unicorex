package de.fzj.unicore.uas.rest;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONObject;

import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.impl.task.TaskImpl;
import de.fzj.unicore.uas.impl.task.TaskModel;
import eu.unicore.services.rest.Link;
import eu.unicore.services.rest.USEResource;
import eu.unicore.services.rest.impl.ServicesBase;

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
	protected void doHandleAction(String action, JSONObject param) throws Exception {
		TaskImpl task = getResource();
		if("abort".equals(action)){
			task.cancel();
		}
	}
	
	@Override
	protected void updateLinks() {
		super.updateLinks();
		// TODO should be state-dependent
		links.add(new Link("action:abort",getBaseURL()+"/tasks/"+resource.getUniqueID()+"/actions/abort","Abort"));
	}

}
