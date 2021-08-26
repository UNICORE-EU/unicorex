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

import java.io.OutputStreamWriter;
import java.util.UUID;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.io.IFileTransfer;
import de.fzj.unicore.xnjs.io.TransferInfo.Status;
import de.fzj.unicore.xnjs.io.IStorageAdapter;
import de.fzj.unicore.xnjs.io.TransferInfo;
import eu.unicore.security.Client;
import eu.unicore.util.Log;

/**
 * file import from inline data in the incoming XML
 *  
 * @author schuller
 */
public class Inline implements IFileTransfer {
	
	private final String workingDirectory;
	private final Client client;
	private final XNJS configuration;
	private OverwritePolicy overwrite;
	private String inlineData;
	private final TransferInfo info;
	
	public Inline(XNJS configuration, Client client, String workingDirectory, String target) {
		this.configuration=configuration;
		this.client=client;
		this.workingDirectory=workingDirectory;
		this.info = new TransferInfo(UUID.randomUUID().toString(), "inline://", target);
		info.setProtocol("inline");
	}

	@Override
	public TransferInfo getInfo(){
		return info;
	}
	
	public void setInlineData(String data){
		this.inlineData = data;
		if(data!=null)info.setDataSize(inlineData.length());
	}
	
	/**
	 * uses TSI link to write inline data to the target
	 */
	public void run() {
		info.setStatus(Status.RUNNING);
		if(tsi == null){
			tsi=configuration.getTargetSystemInterface(client);
		}
		tsi.setStorageRoot(workingDirectory);
		boolean append = OverwritePolicy.APPEND.equals(overwrite);
		try(OutputStreamWriter os = new OutputStreamWriter(tsi.getOutputStream(info.getTarget(),append),"UTF-8")){
			os.write(inlineData);
			info.setTransferredBytes(inlineData.length());
			info.setStatus(Status.DONE);
		}catch(Exception ex){
			info.setStatus(Status.FAILED, Log.createFaultMessage("Writing to '"
					+ workingDirectory+"/"+info.getTarget() + "' failed", ex));
		}
	}
	
	@Override
	public void abort(){}
	
	@Override
	public void setOverwritePolicy(OverwritePolicy overwrite) {
		this.overwrite = overwrite;
	}
	
	@Override
	public void setImportPolicy(ImportPolicy policy){
		// NOP
	}

	private IStorageAdapter tsi;
	
	@Override
	public void setStorageAdapter(IStorageAdapter adapter) {
		this.tsi = adapter;
	}

}
