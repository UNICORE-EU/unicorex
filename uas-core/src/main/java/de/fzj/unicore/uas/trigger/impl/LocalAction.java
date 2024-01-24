package de.fzj.unicore.uas.trigger.impl;

import java.util.Map;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.uas.trigger.Action;
import de.fzj.unicore.uas.util.LogUtil;
import eu.unicore.security.Client;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.io.IStorageAdapter;
import eu.unicore.xnjs.util.AsyncCommandHelper;

/**
 * executes a script "locally" i.e. on the login node 
 * 
 * @author schuller
 */
public class LocalAction extends BaseAction {

	private static final Logger logger = LogUtil.getLogger(LogUtil.TRIGGER, LocalAction.class);

	private final String script;
	
	private String outDir;
	private String stdout;
	private String stderr;
	
	public LocalAction(String script){
		this.script=script;
	}
	
	@Override
	public void fire(IStorageAdapter storage, String filePath, Client client, XNJS xnjs) throws Exception{
		logger.info("Executing local script as <"+client.getSelectedXloginName()
				+"> for <"+client.getDistinguishedName()+">");
		
		Map<String,String>context=getContext(storage, filePath, client, xnjs);
		String workDir=context.get(Action.CURRENT_DIR);
		
		AsyncCommandHelper ach=new AsyncCommandHelper(xnjs, 
				expandVariables(script,context), 
				"trigger", 
				null, 
				client,
				workDir);
		if(stderr!=null){
			ach.setStderr(expandVariables(stderr,context));
		}
		if(stdout!=null){
			ach.setStdout(expandVariables(stdout,context));
		}
		if(outDir!=null){
			ach.getSubCommand().outputDir=expandVariables(outDir,context);
		}
		ach.setUmask(storage.getUmask());
		ach.submit();
	}

	public void setOutcomeDir(String outDir) {
		this.outDir = outDir;
	}

	public void setStdout(String stdout) {
		this.stdout = stdout;
	}

	public void setStderr(String stderr) {
		this.stderr = stderr;
	}

	public String toString(){
		return "LOCAL";
	}
}
