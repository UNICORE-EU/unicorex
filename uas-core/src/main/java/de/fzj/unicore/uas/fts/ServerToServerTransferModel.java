package de.fzj.unicore.uas.fts;

import eu.unicore.security.Client;

public class ServerToServerTransferModel extends FileTransferModel {

	private static final long serialVersionUID = 1L;

	long scheduledStartTime=0;

	String fileTransferUID;

	Client client;

	public long getScheduledStartTime() {
		return scheduledStartTime;
	}

	public void setScheduledStartTime(long scheduledStartTime) {
		this.scheduledStartTime = scheduledStartTime;
	}

	public Client getClient() {
		return client;
	}

	public void setClient(Client client) {
		this.client = client;
	}

	public String getFileTransferUID() {
		return fileTransferUID;
	}

	public void setFileTransferUID(String filetransferUID) {
		this.fileTransferUID = filetransferUID;
	}

}
