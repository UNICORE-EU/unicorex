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
 
package de.fzj.unicore.xnjs.io;

import java.io.IOException;

import de.fzj.unicore.xnjs.fts.IFTSController;
import eu.unicore.security.Client;

public interface IFileTransferCreator {

	/**
	 * create a file import into the target file system
	 * 
	 * @param client - the Client
	 * @param workingDirectory - the working directory
	 * @param info - details about the transfer
	 */
	public IFileTransfer createFileImport(Client client, String workingDirectory, DataStageInInfo info);

	
	/**
	 * create a file export from the target file system
	 * 
	 * @param client - the Client
	 * @param workingDirectory - the working directory
	 * @param info - details about the transfer
	 */
	public IFileTransfer createFileExport(Client client, String workingDirectory, DataStageOutInfo info);

	/**
	 * Creates a new file export from the given working directory.<br/>
	 * The list of registered {@link IFileTransferCreator}s is traversed and the first
	 * non-null result is returned.
	 * @param client
	 * @param workingDirectory
	 * @param info - details about the transfer
	 */
	public default IFTSController createFTSExport(Client client, String workingDirectory, DataStageOutInfo info) throws IOException{
		return null;
	}

	/**
	 * Creates a new file import into the given working directory.<br/>
	 * The list of registered {@link IFileTransferCreator}s is traversed and the first
	 * non-null result is returned.
	 * 
	 * @param client
	 * @param workingDirectory
	 * @param info - details about the transfer
	 */
	public default IFTSController createFTSImport(Client client, String workingDirectory, DataStageInInfo info) throws IOException{
		return null;
	}
	
	/**
	 * returns the protocol
	 */
	public String getProtocol();

	/**
	 * returns the protocol for stage-in
	 */
	public String getStageInProtocol();

	/**
	 * returns the protocol for stage-out
	 */
	public String getStageOutProtocol();
	
	/**
	 * Used to order different file transfer creator implementations. Higher
	 * numbers mean higher priority. If your implementation wants to override
	 * an existing implementation for a certain protocol, you need to return a 
	 * higher priority than the implementation you want to override.
	 * @return
	 */
	public default int getPriority() {
		return 0;
	}
	
}
