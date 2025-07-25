package eu.unicore.xnjs.io;

import eu.unicore.persist.Persist;
import eu.unicore.persist.PersistenceException;
import eu.unicore.security.Client;
import eu.unicore.xnjs.fts.FTSInfo;
import eu.unicore.xnjs.fts.IFTSController;


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
	public IFileTransfer createFileExport(Client client, String workingDirectory, DataStageOutInfo info) throws Exception;

	/**
	 * Creates a new file import into given working directory.<br/>
	 * The list of registered {@link IFileTransferCreator}s is traversed and the first
	 * non-null result is returned.
	 * 
	 * @param client
	 * @param workingDirectory
	 * @param info - details about the transfer
	 */
	public IFileTransfer createFileImport(Client client, String workingDirectory, DataStageInInfo info) throws Exception;

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
	public IFTSController createFTSExport(Client client, String workingDirectory, DataStageOutInfo info) throws Exception;
	/**
	 * Creates a new file import into the given working directory.<br/>
	 * The list of registered {@link IFileTransferCreator}s is traversed and the first
	 * non-null result is returned.
	 * 
	 * @param client
	 * @param workingDirectory
	 * @param info - details about the transfer
	 */
	public IFTSController createFTSImport(Client client, String workingDirectory, DataStageInInfo info) throws Exception;

	/**
	 * get a DB connector for storing information about FTS instances
	 */
	public Persist<FTSInfo> getFTSStorage() throws PersistenceException;

}
