package eu.unicore.xnjs.io.impl;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;

import eu.unicore.security.Client;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.io.DataStagingCredentials;
import eu.unicore.xnjs.io.TransferInfo.Status;
import eu.unicore.xnjs.io.http.IConnectionFactory;
import eu.unicore.xnjs.tsi.TSI;

/**
 * simple HTTP upload implementation of the FileTransfer
 * 
 * @author schuller
 */
public class HTTPFileUpload extends AsyncFilemover{

	private final DataStagingCredentials credentials;
	
	public HTTPFileUpload(Client client, String workingDirectory, String source, String target, 
			XNJS config, 
			DataStagingCredentials credentials){
		super(client,workingDirectory,source,target,config);
		info.setProtocol(target.toLowerCase().startsWith("https") ? "https" : "http");
		this.credentials=credentials;
	}

	public final boolean isImport(){
		return false;
	}
	
	protected void doRun() throws Exception {
		runLocally();	
		//TODO some curl based implementation?
	}

	public String makeCommandline(){
		throw new IllegalStateException();
	}

	public void runLocally() {
		InputStream is = null;
		try{
			if(storageAdapter==null){
				TSI tsi=configuration.getTargetSystemInterface(client, preferredLoginNode);
				tsi.setStorageRoot(workingDirectory);
				is=tsi.getInputStream(info.getSource());
			}
			else{
				is=storageAdapter.getInputStream(info.getSource());
			}
			IConnectionFactory cf=configuration.get(IConnectionFactory.class);
			
			HttpPut put=new HttpPut(info.getTarget());
			put.setEntity(new InputStreamEntity(is, -1, ContentType.WILDCARD));
			if(credentials!=null){
				put.addHeader("Authorization", credentials.getHTTPAuthorizationHeader(client));
			}
			HttpClient httpClient=cf.getConnection(info.getTarget(), client);
			try(ClassicHttpResponse response = httpClient.executeOpen(null, put, HttpClientContext.create())){
				int code=response.getCode();
				if(code<200 || code>=300){
					throw new Exception("Error performing upload: server returned <"+response.getReasonPhrase()+">");
				}
			}
			info.setStatus(Status.DONE);
		}catch(Exception ex){
			reportFailure("Could not perform HTTP upload", ex);
		}
		finally{
			IOUtils.closeQuietly(is);
		}
	}

}
