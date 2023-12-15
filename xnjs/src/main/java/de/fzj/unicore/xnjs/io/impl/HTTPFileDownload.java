package de.fzj.unicore.xnjs.io.impl;

import static de.fzj.unicore.xnjs.util.IOUtils.quote;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpResponse;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.io.DataStagingCredentials;
import de.fzj.unicore.xnjs.io.IOProperties;
import de.fzj.unicore.xnjs.io.TransferInfo.Status;
import de.fzj.unicore.xnjs.io.http.IConnectionFactory;
import eu.unicore.security.Client;

/**
 * simple HTTP download implementation of the FileTransfer
 * 
 * @author schuller
 */
public class HTTPFileDownload extends AsyncFilemover{

	private final DataStagingCredentials credentials;

	private final IOProperties ioProperties;
	
	public HTTPFileDownload(Client client, String workingDirectory, String source, String target, 
			XNJS config,
			DataStagingCredentials credentials){
		super(client,workingDirectory,source,target,config);
		info.setProtocol(source.toLowerCase().startsWith("https") ? "https" : "http");
		this.credentials=credentials;
		ioProperties = config.getIOProperties();
	}

	public final boolean isImport(){
		return true;
	}

	protected void doRun() throws Exception {
		if(ioProperties.getValue(IOProperties.WGET)==null){
			runLocally();	
		}
		else{
			super.doRun();
		}
	}

	public String makeCommandline(){
		StringBuilder sb = new StringBuilder();
		String wgetCmd = ioProperties.getValue(IOProperties.WGET);
		String wgetOpts = ioProperties.getValue(IOProperties.WGET_PARAMS);
		sb.append(wgetCmd);
		if(wgetOpts!=null){
			sb.append(" ").append(wgetOpts);
		}
		if(credentials!=null){
			sb.append(" --header ").append(quote("Authorization: "+credentials.getHTTPAuthorizationHeader(client)));
		}
		sb.append(" -O ").append(quote(info.getTarget()));
		sb.append(" ").append(quote(info.getSource()));
		return sb.toString();
	}

	// write data via TSI
	public void runLocally() {
		try(InputStream is = getInputStream(info.getSource());
			OutputStream os=storageAdapter.getOutputStream(info.getTarget(), false))
		{
			copyTrackingTransferedBytes(is, os);
			info.setStatus(Status.DONE);
		}catch(Exception ex){
			reportFailure("Download failed.", ex);
		}
	}

	protected InputStream getInputStream(String url)throws IOException, MalformedURLException{
		IConnectionFactory cf=configuration.get(IConnectionFactory.class);
		HttpGet get=new HttpGet(info.getSource());
		if(credentials!=null){
			get.addHeader("Authorization", credentials.getHTTPAuthorizationHeader(client));
		}
		HttpClient httpClient=cf.getConnection(info.getSource(),client);
		ClassicHttpResponse response = httpClient.executeOpen(null, get, HttpClientContext.create());
		int status=response.getCode();
		if(status<200||status>=300){
			throw new IOException("Error downloading file, server message: <"+response.getReasonPhrase()+">");
		}
		return response.getEntity().getContent();
	}

}
