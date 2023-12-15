package de.fzj.unicore.xnjs.io.impl;

import static de.fzj.unicore.xnjs.util.IOUtils.quote;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.OutputStream;
import java.net.URI;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.io.IOProperties;
import de.fzj.unicore.xnjs.tsi.TSI;
import de.fzj.unicore.xnjs.util.IOUtils;
import eu.unicore.security.Client;

/**
 * gsiftp file import implementation
 * 
 * TODO how to get remote file size?
 * 
 * @author schuller
 */
public class GSIFTPDownload extends AsyncFilemover {

	private final IOProperties ioProperties;
	
	public GSIFTPDownload(Client client, String workingDirectory, URI source, String target, XNJS config) {
		super(client,workingDirectory,source.toASCIIString(),target,config);
		info.setProtocol("gsiftp");
		ioProperties = config.getIOProperties();
	}
	
	public String makeCommandline(){
		String executable=ioProperties.getValue(IOProperties.GLOBUS_URL_COPY);
		String params=ioProperties.getValue(IOProperties.GLOBUS_URL_COPY_PARAMS);
		File f = new File(workingDirectory,info.getTarget());
		URI uri = f.toURI();
		return executable+" "+params+" "+quote(info.getSource())+" "+quote(uri.toASCIIString());
	}
	
	public final boolean isImport(){
		return true;
	}
	
	protected void preSubmit()throws Exception{
		writeProxyIfRequired(configuration,client,workingDirectory);
		ach.setEnvironmentVariable("X509_USER_PROXY", ".proxy");
	}
	
	public static void writeProxyIfRequired(XNJS configuration, Client client, String workingDirectory)
	throws Exception{
		
		String pem=(String)client.getSecurityTokens().getContext().get("Proxy");
		if(pem==null){
			logger.error("No proxy cert available in security attributes! Can't execute GSIFTP");
			throw new Exception("No proxy cert available in security attributes! Can't execute GSIFTP");
		}
		TSI tsi=configuration.getTargetSystemInterface(client);
		tsi.setStorageRoot(workingDirectory);
		ByteArrayInputStream is=new ByteArrayInputStream(pem.getBytes());
		OutputStream os=tsi.getOutputStream(".proxy");
		try{
			IOUtils.copy(is, os, Integer.MAX_VALUE);
		}finally{
			IOUtils.closeQuietly(os);
		}
	}
	
}
