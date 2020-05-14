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

import java.io.InputStream;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.InputStreamEntity;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.io.DataStagingCredentials;
import de.fzj.unicore.xnjs.io.TransferInfo.Status;
import de.fzj.unicore.xnjs.io.http.IConnectionFactory;
import de.fzj.unicore.xnjs.tsi.TSI;
import de.fzj.unicore.xnjs.util.IOUtils;
import eu.unicore.security.Client;

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
				TSI tsi=configuration.getTargetSystemInterface(client);
				tsi.setStorageRoot(workingDirectory);
				is=tsi.getInputStream(info.getSource());
			}
			else{
				is=storageAdapter.getInputStream(info.getSource());
			}
			IConnectionFactory cf=configuration.get(IConnectionFactory.class);
			
			HttpPut put=new HttpPut(info.getTarget());
			put.setEntity(new InputStreamEntity(is,-1));
			if(credentials!=null){
				put.addHeader("Authorization", credentials.getHTTPAuthorizationHeader(client));
			}
			HttpClient httpClient=cf.getConnection(info.getTarget(), client);
			HttpResponse response=httpClient.execute(put);
			int code=response.getStatusLine().getStatusCode();
			if(code<200 || code>=300){
				throw new Exception("Error performing upload: server returned <"+response.getStatusLine()+">");
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
