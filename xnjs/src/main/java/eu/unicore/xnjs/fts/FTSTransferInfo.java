package eu.unicore.xnjs.fts;

import java.io.Serializable;

import eu.unicore.xnjs.io.TransferInfo.Status;

/**
 * Stores info about a single file transfer of a multi-file FTS
 * 
 * @author schuller
 **/
public class FTSTransferInfo implements Serializable {

	private static final long serialVersionUID = 1l;
	
	private final boolean isExport;
	
	private final SourceFileInfo source;

	private final String target;
	
	private Status status = Status.CREATED;
	private String statusMessage = "";
	private String transferUID;

	public FTSTransferInfo(SourceFileInfo source, String target, boolean isExport) {
		this.source = source;
		this.target = target;
		this.isExport = isExport;
	}

	public SourceFileInfo getSource() {
		return source;
	}

	public String getTarget() {
		return target;
	}

	public boolean isExport() {
		return isExport;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public String getTransferUID() {
		return transferUID;
	}

	public void setTransferUID(String transferUID) {
		this.transferUID = transferUID;
	}

	public String getStatusMessage() {
		return statusMessage;
	}

	public void setStatusMessage(String statusMessage) {
		this.statusMessage = statusMessage;
	}

	public String toString(){
		return String.valueOf(source)+"->"+String.valueOf(target)+" : "+status+(transferUID!=null ? " ftUID="+transferUID:"");
	}
	
}

