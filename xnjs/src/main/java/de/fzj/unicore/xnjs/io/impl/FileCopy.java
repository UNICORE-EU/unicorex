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

import java.io.File;
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
 * file transfer using local copy
 *  
 * @author schuller
 */
public class FileCopy implements IFileTransfer {
	
	private final String workingDirectory;
	private final Client client;
	private final XNJS configuration;
	private final boolean isImport;
	private ImportPolicy policy = ImportPolicy.PREFER_COPY;
	
	private final TransferInfo transferInfo;
	
	public FileCopy(XNJS configuration, Client client, String workingDirectory, String source, String target, boolean isImport) {
		this.configuration=configuration;
		this.client=client;
		this.workingDirectory=workingDirectory;
		this.isImport=isImport;
		this.transferInfo = new TransferInfo(UUID.randomUUID().toString(), source, target);
		transferInfo.setStatus(Status.CREATED);
		transferInfo.setProtocol("file");
	}
	
	/**
	 * does simple copy
	 */
	public void run() {
		transferInfo.setStatus(Status.RUNNING);
		TSI tsi = configuration.getTargetSystemInterface(client);
		String mode = "copy";
		String s= isImport ? transferInfo.getSource() : workingDirectory+tsi.getFileSeparator()+transferInfo.getSource();
		String t=!isImport ? transferInfo.getTarget() : workingDirectory+tsi.getFileSeparator()+transferInfo.getTarget();
		try{
			createParentDirectories(t, tsi);
			if(ImportPolicy.PREFER_LINK == policy){
				mode = "link";
				tsi.link(s, t);
			}
			else{
				tsi.cp(s,t);
			}
			transferInfo.setStatus(Status.DONE);
		}catch(Exception ex){
			transferInfo.setStatus(Status.FAILED);
			transferInfo.setStatusMessage(Log.createFaultMessage("Could not "+mode+" "+s+"->"+t, ex));
		}
	}

	private void createParentDirectories(String target, TSI tsi)throws Exception{
		String parent=new File(target).getParent();
		if(parent!=null){
			tsi.mkdir(parent);
		}
	}
	
	public void abort() {}

	public Map<String,Serializable>pause() {
		return null;
	}

	public void resume(Map<String,Serializable>state) {	
	}

	public void setOverwritePolicy(OverwritePolicy overwrite) {
		// TODO Auto-generated method stub
	}

	public void setStorageAdapter(IStorageAdapter adapter) {
		// no effect...
	}
		
	@Override
	public void setImportPolicy(ImportPolicy policy){
		this.policy = policy;
	}
	
	public TransferInfo getInfo() {
		return transferInfo;
	}

}
