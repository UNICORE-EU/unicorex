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
 

package de.fzj.unicore.xnjs.io;

import java.io.IOException;

import de.fzj.unicore.persist.Persist;
import de.fzj.unicore.persist.PersistenceException;
import de.fzj.unicore.xnjs.fts.FTSInfo;
import de.fzj.unicore.xnjs.fts.IFTSController;
import eu.unicore.security.Client;


/**
 * Create and manage file transfers
 * 
 * @author schuller
 */
public interface IFileTransferEngine {
	
	/**
	 * Creates a new file export from the given working directory.<br/>
	 * The list of registered {@link IFileTransferCreator}s is traversed and the first
	 * non-null result is returned.
	 * @param client
	 * @param workingDirectory
	 * @param info - details about the transfer
	 */
	public IFileTransfer createFileExport(Client client, String workingDirectory, DataStageOutInfo info) throws IOException;

	/**
	 * Creates a new file import into given working directory.<br/>
	 * The list of registered {@link IFileTransferCreator}s is traversed and the first
	 * non-null result is returned.
	 * 
	 * @param client
	 * @param workingDirectory
	 * @param info - details about the transfer
	 */
	public IFileTransfer createFileImport(Client client, String workingDirectory, DataStageInInfo info) throws IOException;
	
	/**
	 * register a new {@link IFileTransferCreator}
	 */
	public void registerFileTransferCreator(IFileTransferCreator creator);
	
	/**
	 * list the protocols supported by this file transfer engine
	 * @return String[] list of protocol names
	 */
	public String[] listProtocols();
	
	/**
	 * register a newly created file transfer
	 * 
	 * @param transfer
	 */
	public void registerFileTransfer(IFileTransfer transfer);
	
	/**
	 * get the status of a file transfer
	 * 
	 * @param id - the unique ID of the file transfer
	 */
	public TransferInfo getInfo(String id);
	
	/**
	 * update the info about a file transfer
	 * 
	 * @param info
	 */
	public void updateInfo(TransferInfo info);
	
	/**
	 * cleanup a file transfer 
	 * (after it has run and the owning process has acknowledged this)
	 * 
	 * @param id - the unique ID of the file transfer
	 */
	public void cleanup(String id);
	
	/**
	 * abort a file transfer
	 * 
	 * @param id - the unique ID of the file transfer
	 */
	public void abort(String id);

	
	
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
	 * get a DB connector for storing information about FTS instances
	 */
	public default Persist<FTSInfo> getFTSStorage() throws PersistenceException {
		return null;
	}
}
