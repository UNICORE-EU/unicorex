package eu.unicore.xnjs.tsi.local;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import eu.unicore.security.Client;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.fts.IUFTPRunner;
import eu.unicore.xnjs.io.impl.UsernamePassword;
import eu.unicore.xnjs.tsi.TSI;
import eu.unicore.xnjs.util.IOUtils;
import jakarta.inject.Inject;

public class UFTPRunnerImpl implements IUFTPRunner {

	private Client client;

	private final XNJS xnjs;

	private String subactionID;

	@Inject
	public UFTPRunnerImpl(XNJS xnjs) {
		this.xnjs = xnjs;
	}

	@Override
	public void setClient(Client client) {
		this.client = client;
	}

	@Override
	public void setID(String id) {
		// NOP
	}

	@Override
	public void setClientHost(String host) {
		// NOP
	}

	@Override
	public void setParentActionID(String actionID) {
		// NOP;
	}

	@Override
	public void get(String from, String to, String workdir, String host, int port, String secret) throws Exception {
		TSI tsi = xnjs.getTargetSystemInterface(client);
		tsi.setStorageRoot(workdir);
		try(InputStream is = openDownload(host, port, from, secret);
				OutputStream os = tsi.getOutputStream(to, false))
		{
			org.apache.commons.io.IOUtils.copy(is, os);
		}
	}

	@Override
	public void put(String from, String to, String workdir, String host, int port, String secret) throws Exception {
		TSI tsi = xnjs.getTargetSystemInterface(client);
		tsi.setStorageRoot(workdir);
		try(InputStream is = tsi.getInputStream(from);
				OutputStream os = openUpload(host, port, to, secret)){
			org.apache.commons.io.IOUtils.copy(is, os);
		}
	}

	@Override
	public String getSubactionID() {
		return subactionID;
	}

	private InputStream openDownload(String host, int port, String target, String secret) throws IOException {
		URL u = IOUtils.addFTPCredentials(new URL(String.format("ftp://%s:%d/%s", host, port, target)),
				new UsernamePassword("anonymous", secret)); 
		return u.openStream();
	}

	private OutputStream openUpload(String host, int port, String target, String secret) throws IOException {
		URL u = IOUtils.addFTPCredentials(new URL(String.format("ftp://%s:%d/%s", host, port, target)),
				new UsernamePassword("anonymous", secret)); 
		return u.openConnection().getOutputStream();
	}

}