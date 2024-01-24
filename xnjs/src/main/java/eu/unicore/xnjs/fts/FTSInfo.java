package eu.unicore.xnjs.fts;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import eu.unicore.persist.annotations.ID;
import eu.unicore.persist.annotations.Table;

/**
 * persistent information about a multi-file transfer
 * 
 * @author schuller
 */
@Table(name="FTSInfo")
public class FTSInfo implements Serializable {

	private static final long serialVersionUID = 1L;

	private String uid;

	private List<FTSTransferInfo> transfers = new ArrayList<>();
	
	private long totalSize;
	
	private int runningTransfers;
	
	public FTSInfo(String uid) {
		this.uid = uid;
	}

	public List<FTSTransferInfo> getTransfers(){
		return transfers;
	}
	
	public void setTransfers(List<FTSTransferInfo> transfers){
		this.transfers = transfers;
	}
	
	@ID
	public String getUID() {
		return uid;
	}

	public long getTotalSize() {
		return totalSize;
	}

	public void setTotalSize(long totalSize) {
		this.totalSize = totalSize;
	}

	public int getRunningTransfers() {
		return runningTransfers;
	}

	public void setRunningTransfers(int runningTransfers) {
		this.runningTransfers = runningTransfers;
	}

}
