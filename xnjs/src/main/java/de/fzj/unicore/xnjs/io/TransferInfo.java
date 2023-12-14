package de.fzj.unicore.xnjs.io;

import java.io.Serializable;
import java.util.UUID;

public class TransferInfo implements Serializable {

	private static final long serialVersionUID = 1l;

	public static enum Status {
		 CREATED,
		 RUNNING,
		 DONE,
		 FAILED,
		 ABORTED
	}

	private String uniqueID, parentActionID;
	
	private Status status = Status.CREATED;
	
	private String statusMessage = "OK.";
	
	private String source, target, protocol;
	
	private long dataSize=-1;
	
	private long transferredBytes=0;
	
	private boolean ignoreFailure;
	
	private transient IFileTransferEngine engine;
	
	public TransferInfo(){
		this.uniqueID = UUID.randomUUID().toString();
	}
	
	public TransferInfo(String uid, String source, String target){
		this.uniqueID = uid;
		this.source = source;
		this.target = target;
	}
	
	public String getUniqueId() {
		return uniqueID;
	}

	public Status getStatus() {
		return status;
	}

	public String getStatusMessage() {
		return statusMessage;
	}

	public long getTransferredBytes() {
		return transferredBytes;
	}

	public long getDataSize() {
		return dataSize;
	}

	public String getSource() {
		return source;
	}

	public String getTarget() {
		return target;
	}

	public void setParentActionID(String actionID) {
		this.parentActionID = actionID;
	}

	public String getParentActionID() {
		return parentActionID;
	}

	public String getProtocol() {
		return protocol;
	}

	public boolean isIgnoreFailure() {
		return ignoreFailure;
	}

	public void setIgnoreFailure(boolean ignoreFailure) {
		this.ignoreFailure = ignoreFailure;
	}

	public void setUniqueId(String uniqueID) {
		this.uniqueID = uniqueID;
	}

	/**
	 * update the status
	 * 
	 * @param status
	 */
	public void setStatus(Status status) {
		this.status = status;
		if(engine!=null){
			engine.updateInfo(this);
		}
	}

	/**
	 * update the status
	 * 
	 * @param status
	 * @param statusMessage
	 */
	public void setStatus(Status status, String statusMessage) {
		this.statusMessage = statusMessage;
		setStatus(status);
	}

	public void setStatusMessage(String statusMessage) {
		this.statusMessage = statusMessage;
	}
	
	public void setSource(String source) {
		this.source = source;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public void setDataSize(long dataSize) {
		this.dataSize = dataSize;
	}

	public void setTransferredBytes(long transferredBytes) {
		this.transferredBytes = transferredBytes;
	}
	
	public String toString(){
		return "["+source+" -> "+target+"]";
	}

	public void setFileTransferEngine(IFileTransferEngine engine){
		this.engine = engine;
	}
}
