package de.fzj.unicore.uas.fts;

import java.util.HashMap;
import java.util.Map;

import org.unigrids.services.atomic.types.ProtocolType;

import de.fzj.unicore.uas.impl.BaseInitParameters;
import de.fzj.unicore.uas.xnjs.StorageAdapterFactory;

public class FiletransferInitParameters extends BaseInitParameters {

	public ProtocolType.Enum protocol;

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
