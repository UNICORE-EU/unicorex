package eu.unicore.uas.fts;

import java.util.HashMap;
import java.util.Map;

import eu.unicore.uas.impl.BaseInitParameters;
import eu.unicore.uas.xnjs.StorageAdapterFactory;

public class FiletransferInitParameters extends BaseInitParameters {

	public String protocol;

	public String actionID;

	public String source;

	public String target;

	public String smsUUID;

	public String workdir;

	public boolean isExport;
	
	public boolean overwrite = true;
	
	public String umask;

	public StorageAdapterFactory storageAdapterFactory;

	public Map<String,String> extraParameters = new HashMap<>();

	public long numbytes = -1;
	
	public String mimetype;

}
