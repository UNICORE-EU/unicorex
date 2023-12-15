package de.fzj.unicore.xnjs.io;

import java.io.IOException;

import de.fzj.unicore.xnjs.fts.FTSInfo;
import de.fzj.unicore.xnjs.fts.IFTSController;
import eu.unicore.persist.Persist;
import eu.unicore.persist.PersistenceException;
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
