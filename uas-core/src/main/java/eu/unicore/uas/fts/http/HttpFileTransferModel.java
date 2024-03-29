package eu.unicore.uas.fts.http;

import eu.unicore.security.Client;
import eu.unicore.uas.fts.FileTransferModel;

public class HttpFileTransferModel extends FileTransferModel {

	private static final long serialVersionUID = 1L;

	String accessURL;

	String contentType;
	
	Client client;
	
	public String getAccessURL() {
		return accessURL;
	}

	public void setAccessURL(String accessURL) {
		this.accessURL = accessURL;
	}

	public Client getClient() {
		return client;
	}

	public void setClient(Client client) {
		this.client = client;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

}
