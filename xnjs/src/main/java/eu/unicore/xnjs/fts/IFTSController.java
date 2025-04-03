package eu.unicore.xnjs.fts;

import java.util.List;
import java.util.Map;

import eu.unicore.xnjs.io.IFileTransfer;
import eu.unicore.xnjs.io.IFileTransfer.ImportPolicy;
import eu.unicore.xnjs.io.IFileTransfer.OverwritePolicy;
import eu.unicore.xnjs.io.IStorageAdapter;

/**
 * interface to manage the processing of file transfers 
 * 
 * @author schuller
 */
public interface IFTSController {
	
	/**
	 * set the storage adaptor for accessing the local endpoint
	 * @param adapter
	 */
	public void setStorageAdapter(IStorageAdapter adapter);
	
	/**
	 * set the overwrite behaviour
	 * @see OverwritePolicy
	 * @param overwrite
	 */
	public void setOverwritePolicy(OverwritePolicy overwrite);
	
	/**
	 * set the import behaviour (import, try-copy, try-link).
	 * This is a best-effort feature, and an implementation may completely 
	 * ignore it!
	 */
	public void setImportPolicy(ImportPolicy policy);

	/**
	 * set the transfer protocol to use
	 */
	public void setProtocol(String protocol);

	/**
	 * set protocol-dependent extra information
	 */
	public default void setExtraParameters(Map<String,String> extraParameters) {}

	/**
	 * Set the desired permissions (e.g. "rwx") on the target file.
	 * Might not be supported by every implementation.
	 */
	public default void setPermissions(String permissions) {}

	/**
	 * Set the desired login node for running the file transfer(s).
	 * Might not be supported by every implementation.
	 */
	public default void setPreferredLoginNode(String loginNode) {}

	/**
	 * collect all the files that should be transferred and put them into the
	 * given list
	 * 
	 * @return the total size, including one extra byte for each directory
	 */
	public long collectFilesForTransfer(List<FTSTransferInfo> fileList) throws Exception;
	
	/**
	 * 
	 * @param from
	 * @param to
	 * @return
	 * @throws Exception
	 */
	public IFileTransfer createTransfer(SourceFileInfo from, String to) throws Exception;

}