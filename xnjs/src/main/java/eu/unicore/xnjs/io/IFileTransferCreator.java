package eu.unicore.xnjs.io;

import eu.unicore.security.Client;
import eu.unicore.xnjs.fts.IFTSController;

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
	public default IFTSController createFTSExport(Client client, String workingDirectory, DataStageOutInfo info) throws Exception{
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
	public default IFTSController createFTSImport(Client client, String workingDirectory, DataStageInInfo info) throws Exception{
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
