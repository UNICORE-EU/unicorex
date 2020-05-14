package de.fzj.unicore.uas.fts.uftp;

import de.fzj.unicore.uas.fts.FileTransferModel;

public class UFTPFiletransferModel extends FileTransferModel {

	private static final long serialVersionUID = 1L;

	String clientHost;

	int streams;

	byte[] key;
	
	boolean isSession;

	boolean compress = false;
	
	public String getClientHost() {
		return clientHost;
	}

	public void setClientHost(String clientHost) {
		this.clientHost = clientHost;
	}

	public int getStreams() {
		return streams;
	}

	public void setStreams(int streams) {
		this.streams = streams;
	}

	public byte[] getKey() {
		return key;
	}

	public void setKey(byte[] key) {
		this.key = key;
	}

	public boolean isSession() {
		return isSession;
	}

	public void setSession(boolean isSession) {
		this.isSession = isSession;
	}

	public boolean isCompress() {
		return compress;
	}

	public void setCompress(boolean compress) {
		this.compress = compress ;
	}

}
