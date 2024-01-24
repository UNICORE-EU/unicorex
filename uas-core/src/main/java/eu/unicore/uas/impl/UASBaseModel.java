package eu.unicore.uas.impl;

import eu.unicore.services.impl.BaseModel;

public class UASBaseModel extends BaseModel {

	private static final long serialVersionUID = 1L;

	/**
	 * each set of services (TSS, SMS, JMS etc instances) may use its own XNJS instance.
	 * This variable contains an ID to this XNJS instance. If it is null, the
	 * default XNJS is used 
	 */
	private String xnjsReference;


	/**
	 *  used to prevent too-frequent refreshes of system info
	 */
	private long lastSystemInfoRefreshInstant;
	
	public String getXnjsReference() {
		return xnjsReference;
	}

	public void setXnjsReference(String xnjsReference) {
		this.xnjsReference = xnjsReference;
	}

	public long getLastSystemInfoRefreshInstant() {
		return lastSystemInfoRefreshInstant;
	}

	public void setLastSystemInfoRefreshInstant(long lastSystemInfoRefreshInstant) {
		this.lastSystemInfoRefreshInstant = lastSystemInfoRefreshInstant;
	}
	
}
