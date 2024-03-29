package eu.unicore.uas.fts.uftp;

import eu.unicore.uas.fts.FileTransferModel;

public class UFTPFiletransferModel extends FileTransferModel {

	private static final long serialVersionUID = 1L;

	String clientHost;

	int streams;

	byte[] key;

	boolean compress = false;
	
	String serverHost;
	int serverPort;
	
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

	public boolean isCompress() {
		return compress;
	}

	public void setCompress(boolean compress) {
		this.compress = compress ;
	}

	public String getServerHost() {
		return serverHost;
	}

	public void setServerHost(String serverHost) {
		this.serverHost = serverHost;
	}

	public int getServerPort() {
		return serverPort;
	}

	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}
}
