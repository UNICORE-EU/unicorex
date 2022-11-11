/*********************************************************************************
 * Copyright (c) 2006 Forschungszentrum Juelich GmbH 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * (1) Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the disclaimer at the end. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * 
 * (2) Neither the name of Forschungszentrum Juelich GmbH nor the names of its 
 * contributors may be used to endorse or promote products derived from this 
 * software without specific prior written permission.
 * 
 * DISCLAIMER
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *********************************************************************************/


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
import de.fzj.unicore.xnjs.tsi.TSI;
import de.fzj.unicore.xnjs.util.IOUtils;
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

	//use the inputstream from the TSI to write the data 
	public void runLocally() {
		InputStream is=null;
		OutputStream os=null;
		try{
			is=getInputStream(info.getSource());
			if(storageAdapter==null){
				TSI tsi=configuration.getTargetSystemInterface(client);
				tsi.setStorageRoot(workingDirectory);
				os=tsi.getOutputStream(info.getTarget());
			}
			else{
				os=storageAdapter.getOutputStream(info.getTarget(), false);
			}
			copyTrackingTransferedBytes(is, os);
			info.setStatus(Status.DONE);
		}catch(Exception ex){
			reportFailure("Download failed.", ex);
		}
		finally{
			IOUtils.closeQuietly(is);
			IOUtils.closeQuietly(os);
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
