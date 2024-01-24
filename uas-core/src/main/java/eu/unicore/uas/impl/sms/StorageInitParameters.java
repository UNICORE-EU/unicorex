package eu.unicore.uas.impl.sms;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import eu.unicore.uas.impl.BaseInitParameters;

public class StorageInitParameters extends BaseInitParameters {
	
	public StorageInitParameters() {
		super();
	}

	public StorageInitParameters(String uuid, Calendar terminationTime) {
		super(uuid, terminationTime);
	}

	public StorageInitParameters(String uuid, TerminationMode terminationMode) {
		super(uuid, terminationMode);
	}

	public StorageInitParameters(String uuid) {
		super(uuid);
	}

	public StorageDescription storageDescription;
	
	public String factoryID;
	
	public boolean inheritSharing = false;

	public boolean appendUniqueID = false;
	
	public boolean skipResolve = false;
	
	public final Map<String,String> userParameters = new HashMap<>();
	
}
