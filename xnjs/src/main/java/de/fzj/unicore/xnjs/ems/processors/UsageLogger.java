/*********************************************************************************
 * Copyright (c) 2006 Forschungszentrum Juelich GmbH 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * (1) Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the disclaimer at the end. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * 
 * (2) Neither the name of Forschungszentrum Juelich GmbH nor the names of its 
 * contributors may be used to endorse or promote products derived from this 
 * software without specific prior written permission.
 * 
 * DISCLAIMER
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *********************************************************************************/
 

package de.fzj.unicore.xnjs.ems.processors;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.ActionStatus;
import de.fzj.unicore.xnjs.util.LogUtil;
import eu.unicore.security.Client;

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
public class UsageLogger extends DefaultProcessor {
	
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
}


