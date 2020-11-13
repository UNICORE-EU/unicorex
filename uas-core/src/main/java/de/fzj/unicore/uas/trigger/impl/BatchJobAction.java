package de.fzj.unicore.uas.trigger.impl;

import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionDocument;
import org.json.JSONObject;

import de.fzj.unicore.uas.json.Builder;
import de.fzj.unicore.uas.util.LogUtil;
import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.Manager;
import de.fzj.unicore.xnjs.io.IStorageAdapter;
import eu.unicore.security.Client;

/**
 * builds a JSDL and submits a job to the XNJS, resulting in a batch job  
 * 
 * @author schuller
 */
public class BatchJobAction extends BaseAction {

	private static final Logger logger = LogUtil.getLogger(LogUtil.TRIGGER, BatchJobAction.class);

	private final JSONObject job;
	
	public BatchJobAction(JSONObject job){
		this.job=job;
	}
	
	@Override
	public void fire(IStorageAdapter storage, String filePath, Client client, XNJS xnjs) throws Exception{
		logger.info("Executing local script as <"+client.getSelectedXloginName()
			+"> for <"+client.getDistinguishedName()+">");
		Map<String,String>context = getContext(storage, filePath, client, xnjs);
		String json=expandVariables(job.toString(), context);
		Builder b=new Builder(json);
		JobDefinitionDocument job=b.getJob();
		de.fzj.unicore.xnjs.ems.Action action = xnjs.makeAction(job);
		action.setUmask(storage.getUmask());
		action.getProcessingContext().put(de.fzj.unicore.xnjs.ems.Action.AUTO_SUBMIT, Boolean.TRUE);
		xnjs.get(Manager.class).add(action, client);
	}

	public String toString(){
		return "BATCH";
	}
}
