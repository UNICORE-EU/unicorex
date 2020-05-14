package de.fzj.unicore.uas.impl.task;

import java.util.Calendar;

import org.apache.xmlbeans.XmlObject;

import de.fzj.unicore.uas.impl.UASBaseModel;
import de.fzj.unicore.uas.impl.task.TaskImpl.TaskStatus;

public class TaskModel extends UASBaseModel {

	private static final long serialVersionUID = 1L;

	private String serviceSpec;
	
	private Calendar submissionTime;
	
	private XmlObject result;
	
	private TaskStatus status = new TaskStatus();

	public String getServiceSpec() {
		return serviceSpec;
	}

	public void setServiceSpec(String serviceSpec) {
		this.serviceSpec = serviceSpec;
	}

	public Calendar getSubmissionTime() {
		return submissionTime;
	}

	public void setSubmissionTime(Calendar submissionTime) {
		this.submissionTime = submissionTime;
	}

	public XmlObject getResult() {
		return result;
	}

	public void setResult(XmlObject result) {
		this.result = result;
	}

	public TaskStatus getStatus() {
		return status;
	}

	public void setStatus(TaskStatus status) {
		this.status = status;
	}
	
	
}
