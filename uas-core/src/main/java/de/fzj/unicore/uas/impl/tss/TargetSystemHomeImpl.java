/*********************************************************************************
 * Copyright (c) 2006-2011 Forschungszentrum Juelich GmbH 
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
 ********************************************************************************/
 

package de.fzj.unicore.uas.impl.tss;

import eu.unicore.services.InitParameters;
import eu.unicore.services.Resource;
import eu.unicore.services.impl.DefaultHome;

public class TargetSystemHomeImpl extends DefaultHome {
	
	private boolean jobSubmissionEnabled=true;
	
	//the default message used when job submission is disabled
	public static final String DEFAULT_MESSAGE="Job submission is currently disabled --- please try again later!";
	
	private String highMessage = "";

	@Override
	protected Resource doCreateInstance(InitParameters initObjs) throws Exception {
		String clazz = initObjs.resourceClassName;
		return(Resource)(Class.forName(clazz).getConstructor().newInstance());
	}

	@Override
	protected Resource doCreateInstance() throws Exception {
		return new TargetSystemImpl();
	}

	@Override
	public void notifyConfigurationRefresh() {
		super.notifyConfigurationRefresh();
		//TODO refresh TSS instances
	}

	public void setJobSubmissionEnabled(boolean enabled){
		this.jobSubmissionEnabled=enabled;
	}
	
	public boolean isJobSubmissionEnabled(){
		return jobSubmissionEnabled;
	}

	public String getHighMessage() {
		if(!jobSubmissionEnabled && highMessage.isEmpty()){
			return DEFAULT_MESSAGE;
		}
		return highMessage;
	}

	public void setHighMessage(String highMessage) {
		this.highMessage = highMessage;
	}
	
	

}
