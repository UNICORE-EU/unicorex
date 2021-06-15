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

import java.io.Serializable;
import java.util.Calendar;

import javax.xml.namespace.QName;

import org.apache.xmlbeans.XmlObject;
import org.unigrids.services.atomic.types.StatusType;

import de.fzj.unicore.uas.Task;
import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.impl.UASWSResourceImpl;
import eu.unicore.services.Home;
import eu.unicore.services.InitParameters;
import eu.unicore.services.Kernel;
import eu.unicore.services.ws.BaseFault;
import eu.unicore.services.ws.renderers.AddressRenderer;
import eu.unicore.services.ws.renderers.ValueRenderer;
import eu.unicore.unicore6.task.CancelRequestDocument;
import eu.unicore.unicore6.task.CancelResponseDocument;
import eu.unicore.unicore6.task.SubmissionTimeDocument;
import eu.unicore.unicore6.task.TaskPropertiesDocument;

/**
 * implementation of the {@link Task} service.<br/>
 * 
 * The actual process is running in the background and can
 * use the {@link #putResult(Kernel, String, XmlObject, String, int)} method to make the result
 * available to the client once it is available.<br/>
 *  
 * @author schuller
 */
public class TaskImpl extends UASWSResourceImpl implements Task {

	public TaskImpl(){
		super();
		addRenderer(new ResultRP(this));
		addRenderer(new StatusRP(this));
		addRenderer(new AddressRenderer(this, RP_SUBMISSION_SERVICE_REFERENCE, false){
			@Override
			protected String getServiceSpec(){
				return getModel().getServiceSpec();
			}
		});
		addRenderer(new ValueRenderer(this, RP_SUBMISSION_TIME) {
			@Override
			protected Object getValue() throws Exception {
				SubmissionTimeDocument d=SubmissionTimeDocument.Factory.newInstance();
				d.setSubmissionTime(getModel().getSubmissionTime());
				return d;
			}
		});
	}
	
	public CancelResponseDocument Cancel(CancelRequestDocument in)throws BaseFault {
		TaskStatus status = getModel().getStatus();
		if(StatusType.RUNNING.equals(status.status)){
			status.status=StatusType.FAILED;
			status.message="Cancelled";
			getModel().setResult(getCancelledResult());
		}
		CancelResponseDocument crd=CancelResponseDocument.Factory.newInstance();
		crd.addNewCancelResponse();
		return crd;
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
		String parentID = initParams.parentUUID;
		String serviceSpec=parentID!=null?parentService+"?res="+parentID:parentService;
		Calendar submissionTime=Calendar.getInstance();
		TaskStatus s=new TaskStatus();
		s.status=StatusType.RUNNING;
		m.setStatus(s);
		m.setServiceSpec(serviceSpec);
		m.setSubmissionTime(submissionTime);
	}

	@Override
	public QName getResourcePropertyDocumentQName() {
		return TaskPropertiesDocument.type.getDocumentElementName();
	}

	public XmlObject getResult(){
		return getModel().getResult();
	}

	public TaskStatus getStatus(){
		return getModel().getStatus();
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
	public static void putResult(Kernel kernel, String uuid, XmlObject result, String message, int exitCode)throws Exception {
		Home home=kernel.getHome(UAS.TASK);
		TaskImpl ti=(TaskImpl)home.getForUpdate(uuid);
		try{
			ti.getModel().setResult(result);
			TaskStatus newStatus=new TaskStatus();
			newStatus.status=StatusType.SUCCESSFUL;
			newStatus.message=message;
			newStatus.exitCode=exitCode;
			ti.getModel().setStatus(newStatus);
		}
		finally{
			home.persist(ti);
		}
	}
	
	public static void failTask(Kernel kernel, String uuid, String message, int exitCode)throws Exception {
		Home home=kernel.getHome(UAS.TASK);
		TaskImpl ti=(TaskImpl)home.getForUpdate(uuid);
		try{
			TaskStatus newStatus=new TaskStatus();
			newStatus.status=StatusType.FAILED;
			newStatus.message=message;
			newStatus.exitCode=exitCode;
			ti.getModel().setStatus(newStatus);
		}
		finally{
			home.persist(ti);
		}
	}
	public static class TaskStatus implements Serializable {
		private static final long serialVersionUID = 1L;
		
		public volatile String message;
		public volatile Float progress;
		public volatile Integer exitCode;
		public volatile StatusType.Enum status=StatusType.UNDEFINED;
	}
	
	public static XmlObject getDefaultResult(){
		String x="<Completed xmlns=\""+NAMESPACE+"\"/>";
		try{
			return XmlObject.Factory.parse(x);
		}catch(Exception ex){
			//can't happen
			throw new RuntimeException(ex);
		}
	}
	public static XmlObject getCancelledResult(){
		String x="<Cancelled xmlns=\""+NAMESPACE+"\"/>";
		try{
			return XmlObject.Factory.parse(x);
		}catch(Exception ex){
			//can't happen
			throw new RuntimeException(ex);
		}
	}
	
}
