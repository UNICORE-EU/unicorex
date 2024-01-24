package eu.unicore.uas.impl;

import java.util.Calendar;

import eu.unicore.services.InitParameters;

public class BaseInitParameters extends InitParameters {

	public BaseInitParameters() {
		super();
	}

	public BaseInitParameters(String uuid, Calendar terminationTime) {
		super(uuid, terminationTime);
	}

	public BaseInitParameters(String uuid, TerminationMode terminationMode) {
		super(uuid, terminationMode);
	}

	public BaseInitParameters(String uuid) {
		super(uuid);
	}
	
	protected BaseInitParameters(String uuid, TerminationMode terminationMode, Calendar terminationTime) {
		super(uuid, terminationMode, terminationTime);
	}

	public String xnjsReference;

}
