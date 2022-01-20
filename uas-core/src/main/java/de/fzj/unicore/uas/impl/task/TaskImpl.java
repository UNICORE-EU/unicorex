/*********************************************************************************
 * Copyright (c) 2010 Forschungszentrum Juelich GmbH 
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
 

package de.fzj.unicore.uas.impl.task;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.impl.BaseResourceImpl;
import eu.unicore.services.Home;
import eu.unicore.services.InitParameters;
import eu.unicore.services.Kernel;

/**
 * implementation of the {@link Task} service.<br/>
 * 
 * The actual process is running in the background and can
 * use the {@link #putResult(Kernel, String, XmlObject, String, int)} method to make the result
 * available to the client once it is available.<br/>
 *  
 * @author schuller
 */
public class TaskImpl extends BaseResourceImpl {

	public TaskImpl(){
		super();
	}
	
	public void cancel() {
		TaskModel m = getModel();
		String status = m.getStatus();
		if("RUNNING".equals(status)){
			m.setStatus("FAILED");
			m.setStatusMessage("Cancelled");
			getModel().setResult(new HashMap<>());
		}
	}

	@Override 
	public TaskModel getModel(){
		return (TaskModel)super.getModel();
	}
	
	@Override
	public void initialise(InitParameters initParams)
			throws Exception {
		TaskModel m = getModel();
		if(m==null){
			m = new TaskModel();
			setModel(m);
		}
		super.initialise(initParams);
		String parentService = initParams.parentServiceName;
		Calendar submissionTime=Calendar.getInstance();
		m.setStatus("RUNNING");
		m.setServiceSpec(parentService);
		m.setSubmissionTime(submissionTime);
	}

	/**
	 * put a result<br/>
	 * 
	 * TODO notify once we support notifications
	 * 
	 * @param kernel
	 * @param uuid - task instance UUID
	 * @param result - the result document
	 * @param message - status message
	 * @param exitCode
	 * @throws Exception
	 */
	public static void putResult(Kernel kernel, String uuid, Map<String, String> result, String message, int exitCode)throws Exception {
		Home home=kernel.getHome(UAS.TASK);
		TaskImpl ti=(TaskImpl)home.getForUpdate(uuid);
		try{
			TaskModel model = ti.getModel();
			model.setStatus("SUCCESSFUL");
			model.setStatusMessage(message);
			model.setExitCode(exitCode);
			model.setResult(result);
		}
		finally{
			home.persist(ti);
		}
	}
	
	public static void failTask(Kernel kernel, String uuid, String message, int exitCode)throws Exception {
		Home home=kernel.getHome(UAS.TASK);
		TaskImpl ti=(TaskImpl)home.getForUpdate(uuid);
		try{
			TaskModel model = ti.getModel();
			model.setStatus("FAILED");
			model.setStatusMessage(message);
			model.setExitCode(exitCode);
		}
		finally{
			home.persist(ti);
		}
	}
	
}
