package eu.unicore.xnjs.io.impl;

import static eu.unicore.xnjs.util.IOUtils.quote;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import eu.unicore.security.Client;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.io.DataStagingCredentials;
import eu.unicore.xnjs.io.IOProperties;
import eu.unicore.xnjs.io.TransferInfo.Status;
import eu.unicore.xnjs.util.IOUtils;

/**
 * 
 * File download using FTP
 *
 * @author schuller
 */
public class FTPDownload extends AsyncFilemover{

	private final UsernamePassword credentials;
	
	private final IOProperties ioProperties;
	
	public FTPDownload(Client client, String workingDirectory, URI source, String target, XNJS config, DataStagingCredentials credentials) {
		super(client,workingDirectory,source.toString(),target,config);
		info.setProtocol("ftp");
		if(credentials!=null && !(credentials instanceof UsernamePassword)){
			throw new IllegalArgumentException("Unsupported credential type <"+credentials.getClass().getName()
					+">, only UsernamePassword is supported.");
		}
		this.credentials=(UsernamePassword)credentials;
		ioProperties = config.getIOProperties();
	}

	public final boolean isImport(){
		return true;
	}
	
	/**
	 * build a curl commandline for downloading the file via FTP 
	 */
	public String makeCommandline()throws MalformedURLException{
		String curl=ioProperties.getValue(IOProperties.CURL);
		URL url=IOUtils.addFTPCredentials(new URL(info.getSource()), credentials);
		StringBuilder sb=new StringBuilder();
		sb.append(curl).append(" ").append(quote(url.toString())).append(" -o ");
		String full = quote(workingDirectory+"/"+info.getTarget());
		sb.append(full);
		return sb.toString();
	}
	
	protected void doRun() throws Exception {
		if(ioProperties.getValue(IOProperties.CURL)==null){
			runLocally();
		}
		else{
			super.doRun();
		}
	}
	
	/**
	 * performs FTP download using java.net.URL directly
	 */
	public void runLocally() {
		try(InputStream is=openURL();
			OutputStream os=storageAdapter.getOutputStream(info.getTarget(), false))
		{
			copyTrackingTransferedBytes(is, os);
			info.setStatus(Status.DONE);
		}catch(Exception ex){
			reportFailure("Could not do FTP download", ex);
		}
	}
	
	private InputStream openURL() throws IOException {
		URL u = IOUtils.addFTPCredentials(new URL(info.getSource()), credentials); 
		return u.openStream();
	}

	public void setImportPolicy(ImportPolicy policy){
		// NOP
	}
}
