package de.fzj.unicore.uas.impl.tss;

import java.util.List;

import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.impl.UASBaseModel;
import de.fzj.unicore.xnjs.io.IStorageAdapter;

public class TSSModel extends UASBaseModel {

	private static final long serialVersionUID = 1L;

	Boolean supportsReservation;

	String umask=Integer.toOctalString(IStorageAdapter.DEFAULT_UMASK);

	public List<String> getReservationIDs() {
		return getChildren(UAS.RESERVATIONS);
	}

	public void setReservationIDs(List<String> reservationIDs) {
		getChildren().put(UAS.RESERVATIONS,reservationIDs);
	}

	public List<String> getJobIDs() {
		return getChildren(UAS.JMS);
	}

	public void setJobIDs(List<String> jobIDs) {
		getChildren().put(UAS.JMS,jobIDs);
	}

	public List<String> getStorageIDs() {
		return getChildren(UAS.SMS);
	}

	public void setStorageIDs(List<String> storageIDs) {
		getChildren().put(UAS.SMS,storageIDs);
	}

	public Boolean getSupportsReservation() {
		return supportsReservation;
	}

	public void setSupportsReservation(Boolean supportsReservation) {
		this.supportsReservation = supportsReservation;
	}

	public String getUmask() {
		return umask;
	}

	public void setUmask(String umask) {
		this.umask = umask;
	}

}
