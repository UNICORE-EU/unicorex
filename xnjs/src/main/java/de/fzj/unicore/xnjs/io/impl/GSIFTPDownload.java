/*********************************************************************************
 * Copyright (c) 2008 Forschungszentrum Juelich GmbH 
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
