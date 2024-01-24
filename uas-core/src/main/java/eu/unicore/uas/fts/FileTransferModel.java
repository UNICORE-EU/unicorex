package eu.unicore.uas.fts;

import java.util.Map;

import eu.unicore.uas.impl.UASBaseModel;
import eu.unicore.uas.xnjs.StorageAdapterFactory;

public class FileTransferModel extends UASBaseModel {

	private static final long serialVersionUID = 1L;

	public static final int STATUS_UNDEFINED = 1;
	public static final int STATUS_READY = 2;
	public static final int STATUS_RUNNING = 3;
	public static final int STATUS_DONE = 4;
	public static final int STATUS_FAILED = 5;
	
	String source;

	String target;

	String protocol;

	String serviceSpec;

	String workdir;

	Boolean firstWrite=Boolean.TRUE;

	Boolean isExport=null;

	Boolean overWrite=Boolean.TRUE;

	Long transferredBytes=0L;

	Map<String,String>extraParameters=null;
	
	// class name!
	private String storageAdapterFactory;

	String umask;

	int status=1;

	String description="";

	// number of bytes to be read/written, -1 if not known
	long numberOfBytes=-1l;

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public String getServiceSpec() {
		return serviceSpec;
	}

	public void setServiceSpec(String serviceSpec) {
		this.serviceSpec = serviceSpec;
	}

	public String getWorkdir() {
		return workdir;
	}

	public void setWorkdir(String workdir) {
		this.workdir = workdir;
	}

	public Boolean isFirstWrite() {
		return firstWrite;
	}

	public void setFirstWrite(Boolean firstWrite) {
		this.firstWrite = firstWrite;
	}

	public Boolean getIsExport() {
		return isExport;
	}

	public void setIsExport(Boolean isExport) {
		this.isExport = isExport;
	}

	public Boolean getOverWrite() {
		return overWrite;
	}

	public void setOverWrite(Boolean overWrite) {
		this.overWrite = overWrite;
	}

	public Long getTransferredBytes() {
		return transferredBytes;
	}

	public void setTransferredBytes(Long transferredBytes) {
		this.transferredBytes = transferredBytes;
	}

	public Map<String, String> getExtraParameters() {
		return extraParameters;
	}

	public void setExtraParameters(Map<String, String> extraParameters) {
		this.extraParameters = extraParameters;
	}

	public StorageAdapterFactory getStorageAdapterFactory() {
		try{
			return (StorageAdapterFactory)(Class.forName(storageAdapterFactory).getConstructor().newInstance());
		}
		catch(Exception e){
			throw new RuntimeException("Can't create instance of class "+storageAdapterFactory,e);
		}
	}

	public void setStorageAdapterFactory(StorageAdapterFactory storageAdapterFactory) {
		if(storageAdapterFactory!=null){
			this.storageAdapterFactory = storageAdapterFactory.getClass().getName();	
		}
	}

	public String getUmask() {
		return umask;
	}

	public void setUmask(String umask) {
		this.umask = umask;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * get expected number of bytes to be read/written, <code>-1</code> if not known
	 */
	public long getNumberOfBytes() {
		return numberOfBytes;
	}

	/**
	 * set expected number of bytes to be read/written, <code>-1</code> if not known
	 */
	public void setNumberOfBytes(long numberOfBytes) {
		this.numberOfBytes = numberOfBytes;
	}

	private String frontend;
	
	public String getFrontend(String serviceType) {
		return frontend;
	}
	
	public void setFrontend(String frontend) {
		this.frontend = frontend;
	}
}
