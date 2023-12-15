package de.fzj.unicore.xnjs.io.impl;

import static de.fzj.unicore.xnjs.util.IOUtils.quote;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.io.DataStagingCredentials;
import de.fzj.unicore.xnjs.io.IOProperties;
import de.fzj.unicore.xnjs.io.TransferInfo.Status;
import de.fzj.unicore.xnjs.tsi.TSI;
import de.fzj.unicore.xnjs.util.IOUtils;
import eu.unicore.security.Client;

public class FTPUpload extends AsyncFilemover {

	private final UsernamePassword credentials;
	
	private final IOProperties ioProperties;
	
	public FTPUpload(Client client, String workingDirectory, String source, URI target, XNJS config, DataStagingCredentials credentials) {
		super(client,workingDirectory,source,target.toString(),config);
		info.setProtocol("ftp");
		if(credentials!=null && !(credentials instanceof UsernamePassword)){
			throw new IllegalArgumentException("Unsupported credential type <"+credentials.getClass().getName()
					+">, only UsernamePassword is supported.");
		}
		this.credentials=(UsernamePassword)credentials;
		ioProperties = config.getIOProperties();
	}
	
	public final boolean isImport(){
		return false;
	}

	/**
	 * build a curl commandline for downloading the file via FTP 
	 */
	public String makeCommandline()throws MalformedURLException{
		String curl=ioProperties.getValue(IOProperties.CURL);
		URL url=IOUtils.addFTPCredentials(new URL(info.getTarget()), credentials);
		StringBuilder sb=new StringBuilder();
		sb.append(curl).append(" -T ");
		String full = quote(workingDirectory+"/"+info.getSource()); 
		sb.append(full);
		sb.append(" ").append(quote(url.toString()));
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
	 * performs FTP upload
	 */
	public void runLocally() {
		try{
			URL url=IOUtils.addFTPCredentials(new URL(info.getTarget()), credentials);
			OutputStream os=url.openConnection().getOutputStream();
			InputStream is=null;
			if(storageAdapter==null){
				TSI tsi=configuration.getTargetSystemInterface(client);
				tsi.setStorageRoot(workingDirectory);
				is=tsi.getInputStream(info.getSource());
			}
			else{
				is=storageAdapter.getInputStream(info.getSource());
			}
			copyTrackingTransferedBytes(is, os);
			IOUtils.closeQuietly(is);
			IOUtils.closeQuietly(os);
			info.setStatus(Status.DONE);
		}catch(Exception ex){
			reportFailure("Could not do FTP upload", ex);
		}
	}

	public void setImportPolicy(ImportPolicy policy){
		// NOP
	}

}
