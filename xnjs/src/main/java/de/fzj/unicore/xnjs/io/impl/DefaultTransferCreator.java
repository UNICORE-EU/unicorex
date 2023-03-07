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
 ********************************************************************************/
 
package de.fzj.unicore.xnjs.io.impl;

import java.net.URI;

import javax.inject.Inject;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.fts.IFTSController;
import de.fzj.unicore.xnjs.fts.impl.InlineFTS;
import de.fzj.unicore.xnjs.io.DataStageInInfo;
import de.fzj.unicore.xnjs.io.DataStageOutInfo;
import de.fzj.unicore.xnjs.io.DataStagingCredentials;
import de.fzj.unicore.xnjs.io.IFileTransfer;
import de.fzj.unicore.xnjs.io.IFileTransferCreator;
import eu.unicore.security.Client;

/**
 * Creates transfers for the following mechanisms
 * FTP, GSIFTP, SCP, HTTP(s), file
 * 
 * @author schuller
 */
public class DefaultTransferCreator implements IFileTransferCreator {
	
	private final XNJS configuration;
	
	@Inject
	public DefaultTransferCreator (XNJS config){
		this.configuration=config;
	}

	@Override
	public String getProtocol() {
		return "ftp, gsiftp, scp, http, https, file, link, inline";
	}
	
	@Override
	public String getStageInProtocol() {
		return "ftp, gsiftp, scp, http, https, file, link, inline";
	}

	@Override
	public String getStageOutProtocol() {
		return "ftp, gsiftp, scp, http, https, file";
	}

	@Override
	public IFileTransfer createFileExport(Client client, String workingDirectory, DataStageOutInfo info) {
		URI target = info.getTarget();
		String scheme = target.getScheme();
		String source = info.getFileName();
		DataStagingCredentials credentials = info.getCredentials();
		
		if("ftp".equalsIgnoreCase(scheme)){
			return new FTPUpload(client,workingDirectory,source,target,configuration,credentials);
		}
		if("gsiftp".equalsIgnoreCase(scheme)){
			return new GSIFTPUpload(client,workingDirectory,source,target,configuration);
		}
		if("scp".equalsIgnoreCase(scheme)){
			return new ScpUpload(client,workingDirectory,source,target,configuration,credentials);
		}
		if("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)){
			return new HTTPFileUpload(client, workingDirectory, source, target.toString(), configuration,credentials);
		}
		if("file".equalsIgnoreCase(scheme)){
			return new FileCopy(configuration, client, workingDirectory, source,target.getRawPath(), false);
		}
		return null;
	}
	
	@Override
	public IFileTransfer createFileImport(Client client, String workingDirectory,  DataStageInInfo info) {
		URI source = info.getSources()[0]; // TODO
		String scheme = source.getScheme();
		String target = info.getFileName();
		DataStagingCredentials credentials = info.getCredentials();
		if("ftp".equalsIgnoreCase(scheme)){
			return new FTPDownload(client,workingDirectory,source,target,configuration,credentials);
		}
		if("gsiftp".equalsIgnoreCase(scheme)){
			return new GSIFTPDownload(client,workingDirectory,source,target,configuration);
		}
		if("scp".equalsIgnoreCase(scheme)){
			return new ScpDownload(client,workingDirectory,source,target,configuration,credentials);
		}
		if("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)){
			return new HTTPFileDownload(client, workingDirectory, source.toString(), target, configuration,credentials);
		}
		if("file".equalsIgnoreCase(scheme)){
			return new FileCopy(configuration, client, workingDirectory, source.getRawPath(),target, true);
		}
		if("link".equalsIgnoreCase(scheme)){
			return new Link(configuration, client, workingDirectory, source.getSchemeSpecificPart(),target);
		}
		if("inline".equalsIgnoreCase(scheme)){
			Inline ft = new Inline(configuration, client, workingDirectory,target);
			ft.setInlineData(info.getInlineData());
			return ft;
		}
		return null;
	}
	
	@Override
	public IFTSController createFTSImport(Client client, String workingDirectory,  DataStageInInfo info) {
		URI source = info.getSources()[0]; // TODO
		String scheme = source.getScheme();
		String target = info.getFileName();
		if("inline".equalsIgnoreCase(scheme)){
			InlineFTS ft = new InlineFTS(configuration, client, workingDirectory,target);
			ft.setInlineData(info.getInlineData());
			return ft;
		}
		return null;
	}
	
	@Override
	public IFTSController createFTSExport(Client client, String workingDirectory,  DataStageOutInfo info) {
		URI target = info.getTarget();
		String scheme = target.getScheme();
		if(scheme.toLowerCase().startsWith("http")) {
			return new HttpExportsController(configuration, client, target.toString(), info, workingDirectory);
		}
		return null;
	}
}
