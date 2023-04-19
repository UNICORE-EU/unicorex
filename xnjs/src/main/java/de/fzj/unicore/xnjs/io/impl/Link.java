/*********************************************************************************
 * Copyright (c) 2012 Forschungszentrum Juelich GmbH 
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

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.io.IFileTransfer;
import de.fzj.unicore.xnjs.io.IStorageAdapter;
import de.fzj.unicore.xnjs.io.TransferInfo;
import de.fzj.unicore.xnjs.io.TransferInfo.Status;
import de.fzj.unicore.xnjs.tsi.TSI;
import eu.unicore.security.Client;
import eu.unicore.util.Log;

/**
 * file "import" using a symbolic link
 *  
 * @author schuller
 */
public class Link implements IFileTransfer {
	
	private final String workingDirectory;
	private final Client client;
	private final XNJS configuration;
	private final TransferInfo info;
	
	public Link(XNJS configuration, Client client, String workingDirectory, String source, String target) {
		this.configuration=configuration;
		this.client=client;
		this.workingDirectory=workingDirectory;
		this.info = new TransferInfo(UUID.randomUUID().toString(), source, target);
		info.setProtocol("link");
	}
	
	public TransferInfo getInfo(){return info;}
	
	public long getDataSize() {
		return -1;
	}

	/**
	 * uses TSI link to create the link
	 */
	public void run() {
		try{
			TSI tsi=configuration.getTargetSystemInterface(client);
			tsi.setStorageRoot("/");
			String target = info.getSource();
			String linkName = workingDirectory+tsi.getFileSeparator()+info.getTarget();
			tsi.link(target,linkName);
			info.setStatus(Status.DONE);
		}catch(Exception ex){
			info.setStatus(Status.FAILED,Log.createFaultMessage("File transfer failed", ex));
		}
	}

	public void abort() {}

	public Map<String,Serializable>pause() {
		return null;
	}

	public void resume(Map<String,Serializable>state) {	
	}

	public void setOverwritePolicy(OverwritePolicy overwrite) {
	}
	
	public void setImportPolicy(ImportPolicy policy){
	}
	
	public void setStorageAdapter(IStorageAdapter adapter) {
	}
	
}
