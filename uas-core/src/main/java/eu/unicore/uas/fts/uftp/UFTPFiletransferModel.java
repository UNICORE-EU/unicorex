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

	boolean persistent = false;

}