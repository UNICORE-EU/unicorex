package de.fzj.unicore.uas.impl.job;

import java.util.Calendar;

import de.fzj.unicore.uas.impl.PersistingPrefsModel;

public class JobModel extends PersistingPrefsModel {

	private static final long serialVersionUID = 1L;

	private String uspaceId;
	
	private Calendar submissionTime;

	// should this job be re-attached to a new TSS instance 
	// in case the old one is deleted?
	private boolean reAttachable = true;
	
	public String getUspaceId() {
		return uspaceId;
	}

	public void setUspaceId(String uspaceId) {
		this.uspaceId = uspaceId;
	}

	public Calendar getSubmissionTime() {
		return submissionTime;
	}

	public void setSubmissionTime(Calendar submissionTime) {
		this.submissionTime = submissionTime;
	}

	public boolean isReAttachable() {
		return reAttachable;
	}

	public void setReAttachable(boolean reAttachable) {
		this.reAttachable = reAttachable;
	}
	
}
