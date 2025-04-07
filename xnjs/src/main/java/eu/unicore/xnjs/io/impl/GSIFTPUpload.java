package eu.unicore.xnjs.io.impl;

import static eu.unicore.xnjs.util.IOUtils.quote;

import java.io.File;
import java.net.URI;

import eu.unicore.security.Client;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.io.IOProperties;

/**
 * gsiftp file export implementation
 * 
 * @author schuller
 */
public class GSIFTPUpload extends AsyncFilemover{

	private final IOProperties ioProperties;

	public GSIFTPUpload(Client client, String workingDirectory, String source,URI target, XNJS config) {
		super(client,workingDirectory,source,target.toASCIIString(),config);
		info.setProtocol("gsiftp");
		ioProperties = config.getIOProperties();
	}

	public String makeCommandline(){
		String executable=ioProperties.getValue(IOProperties.GLOBUS_URL_COPY);
		String params=ioProperties.getValue(IOProperties.GLOBUS_URL_COPY_PARAMS);
		File f = new File(workingDirectory,info.getSource());
		URI uri = f.toURI();
		return executable+" "+params+" "+quote(uri.toASCIIString())+" "+quote(info.getTarget());
	}

	public final boolean isImport(){
		return false;
	}

	protected void preSubmit()throws Exception{
		GSIFTPDownload.writeProxyIfRequired(xnjs, client, workingDirectory, preferredLoginNode);
		ach.setEnvironmentVariable("X509_USER_PROXY", ".proxy");
	}

}
