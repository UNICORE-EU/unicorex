package eu.unicore.uas.fts.http;

import eu.unicore.security.Client;
import eu.unicore.uas.fts.FileTransferModel;

public class HttpFileTransferModel extends FileTransferModel {

	private static final long serialVersionUID = 1L;

	String accessURL;

	Client client;

	public String getAccessURL() {
		return accessURL;
	}

	public Client getClient() {
		return client;
	}

	public void setClient(Client client) {
		this.client = client;
	}

}