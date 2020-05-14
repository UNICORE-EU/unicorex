/*********************************************************************************
 * Copyright (c) 2011 Forschungszentrum Juelich GmbH 
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

import java.net.URI;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.io.DataStagingCredentials;
import de.fzj.unicore.xnjs.io.IOProperties;
import de.fzj.unicore.xnjs.util.IOUtils;
import eu.unicore.security.Client;

/**
 * scp upload as defined by HPC file staging profile (GFD.135)
 * 
 * @author schuller
 */
public class ScpUpload extends AsyncFilemover{

	private final UsernamePassword credentials;
	
	private final IOProperties ioProperties;
	
	/**
	 * create an SCP upload
	 * 
	 * @param client
	 * @param workingDirectory
	 * @param source
	 * @param target
	 * @param config
	 * @param credentials - only UsernamePassword is supported
	 */
	public ScpUpload(Client client, String workingDirectory, String source, URI target, XNJS config, DataStagingCredentials credentials){
		super(client,workingDirectory,source,target.toString(),config);
		info.setProtocol("scp");
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
	
	public String makeCommandline()throws Exception{
		String scpWrapper=ioProperties.getValue(IOProperties.SCP_WRAPPER);
		String uri=info.getTarget();
		if(credentials!=null){
			//add username, but not password as per GFD.135
			uri=IOUtils.makeSCPAddress(uri, credentials.getUser());	
		}
		else throw new IllegalArgumentException("Username required for scp");
		
		StringBuilder sb=new StringBuilder();
		String full = quote(workingDirectory+"/"+info.getSource());
		sb.append(scpWrapper).append(" ").append(full).append(" ");
		sb.append(quote(uri));
		if(credentials!=null && credentials.getPassword()!=null){
			sb.append(" ").append(quote(credentials.getPassword()));
		}
		return sb.toString();
	}

}
