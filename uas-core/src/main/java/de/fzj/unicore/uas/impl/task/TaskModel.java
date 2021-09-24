package de.fzj.unicore.uas.impl.task;

import java.util.Calendar;
import java.util.Map;

import de.fzj.unicore.uas.impl.UASBaseModel;

public class TaskModel extends UASBaseModel {

	private static final long serialVersionUID = 1L;

	private String serviceSpec;
	
	private Calendar submissionTime;
	
	private Map<String,String> result;
	
	private String status = "CREATED";
	private String statusMessage = "";
	private Integer exitCode;
	
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

	public Map<String,String> getResult() {
		return result;
	}

	public void setResult(Map<String,String> result) {
		this.result = result;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getStatusMessage() {
		return statusMessage;
	}

	public void setStatusMessage(String statusMessage) {
		this.statusMessage = statusMessage;
	}

	public Integer getExitCode() {
		return exitCode;
	}

	public void setExitCode(int exitCode) {
		this.exitCode = exitCode;
	}

}
