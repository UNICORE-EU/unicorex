/*********************************************************************************
* Copyright (c) 2006-2011 Forschungszentrum Juelich GmbH 
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
import java.net.URI;
import java.net.URL;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.io.DataStagingCredentials;
import de.fzj.unicore.xnjs.io.IOProperties;
import de.fzj.unicore.xnjs.io.TransferInfo.Status;
import de.fzj.unicore.xnjs.util.IOUtils;
import eu.unicore.security.Client;

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
