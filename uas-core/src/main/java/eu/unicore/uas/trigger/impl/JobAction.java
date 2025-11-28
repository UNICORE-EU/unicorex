package eu.unicore.uas.trigger.impl;

import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import eu.unicore.security.Client;
import eu.unicore.uas.json.Builder;
import eu.unicore.uas.trigger.SingleFileAction;
import eu.unicore.uas.util.LogUtil;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.ems.Action;
import eu.unicore.xnjs.ems.Manager;
import eu.unicore.xnjs.io.IStorageAdapter;

/**
 * builds a job and submits it to the XNJS
 * 
 * @author schuller
 */
public class JobAction extends BaseAction implements SingleFileAction {

	private static final Logger logger = LogUtil.getLogger(LogUtil.TRIGGER, JobAction.class);

	private final JSONObject job;

	private String filePath;

	public JobAction(JSONObject job){
		this.job=job;
	}

	@Override
	public void setTarget(String filePath) {
		this.filePath = filePath;
	}

	@Override
	public String run(IStorageAdapter storage, Client client, XNJS xnjs) throws Exception{
		logger.info("Running job as <{}> for <{}>",
				client.getSelectedXloginName(), client.getDistinguishedName());
		Map<String,String>context = getContext(storage, filePath, client, xnjs);
		String json = expandVariables(job.toString(), context);
		Builder b = new Builder(json);
		JSONObject job = b.getJSON();
		Action action = xnjs.makeAction(job);
		action.setUmask(storage.getUmask());
		action.getProcessingContext().put(Action.AUTO_SUBMIT, Boolean.TRUE);
		xnjs.get(Manager.class).add(action, client);
		return action.getUUID();
	}

	@Override
	public String toString(){
		return "JOB";
	}

}