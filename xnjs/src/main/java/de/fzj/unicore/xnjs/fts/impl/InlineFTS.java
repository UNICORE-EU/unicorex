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
 

package de.fzj.unicore.xnjs.fts.impl;

import java.util.List;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.fts.FTSTransferInfo;
import de.fzj.unicore.xnjs.fts.IFTSController;
import de.fzj.unicore.xnjs.fts.SourceFileInfo;
import de.fzj.unicore.xnjs.io.IFileTransfer;
import de.fzj.unicore.xnjs.io.IFileTransfer.ImportPolicy;
import de.fzj.unicore.xnjs.io.IFileTransfer.OverwritePolicy;
import de.fzj.unicore.xnjs.io.IStorageAdapter;
import de.fzj.unicore.xnjs.io.impl.Inline;
import eu.unicore.security.Client;

/**
 * file import from inline data in the incoming job description
 *  
 * @author schuller
 */
public class InlineFTS implements IFTSController {
	
	private final String workingDirectory;
	private final Client client;
	private final XNJS xnjs;
	private final String target;
	private String inlineData;
	private OverwritePolicy overwrite;
	
	private IStorageAdapter tsi;
		
	public InlineFTS(XNJS xnjs, Client client, String workingDirectory, String target) {
		this.xnjs = xnjs;
		this.client = client;
		this.workingDirectory = workingDirectory;
		this.target = target;
	}
	
	public void setInlineData(String data){
		this.inlineData = data;
	}

	@Override
	public void setOverwritePolicy(OverwritePolicy overwrite) {
		this.overwrite = overwrite;
	}
	
	@Override
	public void setImportPolicy(ImportPolicy policy){
		// NOP
	}

	@Override
	public void setStorageAdapter(IStorageAdapter adapter) {
		this.tsi = adapter;
	}

	@Override
	public long collectFilesForTransfer(List<FTSTransferInfo> fileList) throws Exception {
		SourceFileInfo sfi = new SourceFileInfo();
		sfi.setPath("inline://");
		sfi.setSize(inlineData.length());
		fileList.add(new FTSTransferInfo(sfi, target, false));
		return inlineData.length();
	}

	@Override
	public IFileTransfer createTransfer(SourceFileInfo from, String to) throws Exception {
		Inline inline = new Inline(xnjs,client, workingDirectory, target);
		inline.setInlineData(inlineData);
		inline.setOverwritePolicy(overwrite);
		inline.setStorageAdapter(tsi);
		return inline;
	}

}
