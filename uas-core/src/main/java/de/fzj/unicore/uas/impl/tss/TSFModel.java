package de.fzj.unicore.uas.impl.tss;

import de.fzj.unicore.uas.impl.UASBaseModel;

public class TSFModel extends UASBaseModel {

	private static final long serialVersionUID = 1L;

	boolean supportsReservation;

	boolean supportsVirtualImages;

	public boolean getSupportsReservation() {
		return supportsReservation;
	}

	public void setSupportsReservation(boolean supportsReservation) {
		this.supportsReservation = supportsReservation;
	}

	public boolean isSupportsVirtualImages() {
		return supportsVirtualImages;
	}

	public void setSupportsVirtualImages(boolean supportsVirtualImages) {
		this.supportsVirtualImages = supportsVirtualImages;
	}

}
