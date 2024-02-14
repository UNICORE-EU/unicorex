package eu.unicore.uas.trigger.impl;

import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import eu.unicore.security.Client;
import eu.unicore.uas.json.Builder;
import eu.unicore.uas.util.LogUtil;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.ems.Manager;
import eu.unicore.xnjs.io.IStorageAdapter;

/**
 * builds a job and submits it to the XNJS
 * 
 * @author schuller
 */
public class JobAction extends BaseAction {

	private static final Logger logger = LogUtil.getLogger(LogUtil.TRIGGER, JobAction.class);

	private final JSONObject job;
	
	public JobAction(JSONObject job){
		this.job=job;
	}
	
	@Override
	public String run(IStorageAdapter storage, String filePath, Client client, XNJS xnjs) throws Exception{
		logger.info("Running job as <"+client.getSelectedXloginName()
			+"> for <"+client.getDistinguishedName()+">");
		Map<String,String>context = getContext(storage, filePath, client, xnjs);
		String json=expandVariables(job.toString(), context);
		Builder b=new Builder(json);
		JSONObject job = b.getJSON();
		eu.unicore.xnjs.ems.Action action = xnjs.makeAction(job);
		action.setUmask(storage.getUmask());
		action.getProcessingContext().put(eu.unicore.xnjs.ems.Action.AUTO_SUBMIT, Boolean.TRUE);
		xnjs.get(Manager.class).add(action, client);
		return action.getUUID();
	}

	public String toString(){
		return "JOB";
	}
}
