package eu.unicore.uas.trigger.impl;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import eu.unicore.security.Client;
import eu.unicore.uas.json.Builder;
import eu.unicore.uas.trigger.MultiFileAction;
import eu.unicore.uas.util.LogUtil;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.ems.Action;
import eu.unicore.xnjs.ems.Manager;
import eu.unicore.xnjs.io.IStorageAdapter;

/**
 * for a list of new files, builds a job and submits it to the XNJS
 * 
 * @author schuller
 */
public class BatchedJobAction extends BaseAction implements MultiFileAction {

	private static final Logger logger = LogUtil.getLogger(LogUtil.TRIGGER, BatchedJobAction.class);

	private final JSONObject job;
		
	private List<String>files;

	public BatchedJobAction(JSONObject job){
		this.job=job;
	}
	
	@Override
	public void setTarget(List<String>target) {
		this.files = target;
	}

	@Override
	public String run(IStorageAdapter storage, Client client, XNJS xnjs) throws Exception{
		logger.info("Running job as <{}> for <{}>",
				client.getSelectedXloginName(), client.getDistinguishedName());
		Map<String,String>context = getContext(storage, files, client, xnjs);
		String json = expandVariables(job.toString(), context);
		Builder b = new Builder(json);
		JSONObject job = b.getJSON();
		Action action = xnjs.makeAction(job);
		action.setUmask(storage.getUmask());
		action.getProcessingContext().put(Action.AUTO_SUBMIT, Boolean.TRUE);
		xnjs.get(Manager.class).add(action, client);
		return action.getUUID();
	}

	public String toString(){
		return "BATCHED-JOB";
	}

}
