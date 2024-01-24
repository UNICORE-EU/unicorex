package eu.unicore.xnjs.ems.processors;

import org.apache.logging.log4j.Logger;

import eu.unicore.security.Client;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.ems.Action;
import eu.unicore.xnjs.ems.ActionStatus;
import eu.unicore.xnjs.ems.Processor;
import eu.unicore.xnjs.util.LogUtil;

/**
 * Writes a usage record to the logger named "unicore.services.jobexecution.USAGE"<br/>
 * <p> 
 * (If needed, Log4j can be configured to send this log messages to a specific file instead of the
 * standard logfile)
 * <p>
 * The format is:
 * [result] [executable] [actionUUID] [clientDN] [BSSJobId] [clientXlogin] [jobName] [machineName] [VOs]
 * <p>
 * Values in brackets are escaped: each '\' is encoded as '\\' and each ']' as '\]'.
 * 
 * @author schuller
 * @author ml054
 * @author golbi
 */
public class UsageLogger extends Processor {
	
	private static final Logger usage = LogUtil.getLogger(LogUtil.JOBS+".USAGE", UsageLogger.class);
	
	private static final Logger logger = LogUtil.getLogger(LogUtil.JOBS, UsageLogger.class);
	
	public static final String USAGE_LOGGED="USAGE.logged";
	
	public UsageLogger(XNJS xnjs){
		super(xnjs);
	}
	
	public void done() {
		if(action.getStatus()!=ActionStatus.DONE)return;
		try{
			Boolean haveLogged=(Boolean)action.getProcessingContext().get(USAGE_LOGGED);
			if(haveLogged==null){
				usage.info(getUsage());
				action.getProcessingContext().put(USAGE_LOGGED,Boolean.TRUE);
				action.setDirty();
			}
		}catch(Exception ex){
			logger.error("Error when logging usage.",ex);
		}
	}
	
	/**
	 *  create a "usage record"
	 */
	public String getUsage(){
		StringBuilder sb=new StringBuilder();
		String uuid=action.getUUID();
		Client c = action.getClient();
		String client= c!=null? c.getDistinguishedName() : "n/a";
		String result=action.getResult().toString();
		String exec;
		if(action.getExecutionContext()!=null){
		    exec=action.getExecutionContext().getExecutable();
		}
		else exec="";
		if(exec==null){
			exec="n/a";
		}
		
		String bsid = action.getBSID();
		String localUserName = c!=null? c.getSelectedXloginName() : "n/a";
		String jobName = action.getJobName();
		
		String machineName = action.getExecutionContext().getPreferredExecutionHost();
		if (machineName == null)
			machineName = "n/a";
		
		String vo = null;
		if (c != null && c.getVos() != null &&
				c.getVos().length > 0) {
			String[] vos = c.getVos();
			StringBuilder vosb = new StringBuilder();
			int i=0;
			for (; i<vos.length-1; i++) {
				vosb.append(vos[i]);
				vosb.append(",");
			}
			if (i<vos.length)
				vosb.append(vos[i]);
			vo = vosb.toString();
		}
		
		if (bsid == null) {
			bsid = "";
		}
		if (localUserName == null) {
			localUserName = "";
		}
		if (jobName == null) {
			jobName = "";
		}
		if (vo == null) {
			vo = "";
		}
		
		String timeProfile = JobProcessor.getTimeProfile(action
				.getProcessingContext());
		
		sb.append("[").append(escapeBrackets(result)).append("] [ ");
		sb.append(escapeBrackets(exec)).append("] [ ");
		sb.append(escapeBrackets(uuid)).append("] [");
		sb.append(escapeBrackets(client)).append("] [");
		sb.append(escapeBrackets(bsid));
		sb.append("] [").append(escapeBrackets(localUserName));
		sb.append("] [").append(escapeBrackets(jobName));
		sb.append("] [").append(escapeBrackets(machineName));
		sb.append("] [").append(escapeBrackets(vo));
		sb.append("] [").append(escapeBrackets(timeProfile));
		sb.append("]");

		return sb.toString();
	}
	
	private static String escapeBrackets(String input)
	{
		String ret1 = input.replace("\\", "\\\\");
		String ret = ret1.replace("]", "\\]");
		return ret;
	}

	/**
	 * allow to set action for unit testing
	 */
	public void setAction(Action a){
		this.action=a;
	}

}


