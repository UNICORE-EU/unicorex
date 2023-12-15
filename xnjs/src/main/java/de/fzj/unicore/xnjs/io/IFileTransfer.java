package de.fzj.unicore.xnjs.io;

import java.util.Map;

/**
 * interface to manage a running file transfer 
 * 
 * @author schuller
 */
public interface IFileTransfer extends Runnable {
	
	
	/**
	 * how to deal with existing files?
	 * <ul>
	 *  <li>OVERWRITE: overwrite existing file (default)</li> 
	 *  <li>APPEND: append to existing file</li>
	 *  <li>DONT_OVERWRITE: throw error if file exists</li>
	 *  <li>RESUME_FAILED_TRANSFER: try to pick up a previously aborted transfer</li>
	 * </ul>
	 */
	public static enum OverwritePolicy {
		 OVERWRITE,
		 APPEND,
		 DONT_OVERWRITE,
		 RESUME_FAILED_TRANSFER
	}
	
	/**
	 * For imports, UNICORE will check if the file is already
	 * available locally (e.g. the import source is on the same
	 * file system).
	 *  <ul>
	 *    <li>PREFER_COPY: (default) data is copied if possible</li> 
	 *    <li>FORCE_COPY: data is always copied</li>
	 *    <li>PREFER_LINK: data is sym-linked, if possible</li>
	 *  </ul>
	 */
	public static enum ImportPolicy {
		PREFER_COPY, 
		FORCE_COPY,
		PREFER_LINK,
	}
	
	/**
	 * set the storage adaptor
	 * @param adapter
	 */
	public void setStorageAdapter(IStorageAdapter adapter);
	
	/**
	 * set the overwrite behaviour
	 * @see OverwritePolicy
	 * @param overwrite
	 */
	public void setOverwritePolicy(OverwritePolicy overwrite)throws OptionNotSupportedException;
	
	/**
	 * set the import behaviour (import, try-copy, try-link).
	 * This is a best-effort feature, and an implementation may completely 
	 * ignore it!
	 */
	public void setImportPolicy(ImportPolicy policy);
	
	/**
	 * start the transfer
	 */
	public void run();

	/**
	 * attempt to abort the transfer
	 */
	public void abort();
	
	/**
	 * get the transfer info instance that keeps track of the status of this transfer
	 */
	public TransferInfo getInfo();
	
	/**
	 * set the file creation umask
	 */
	public default void setUmask(String umask) {}
	
	/**
	 * Allows to configure any extra, protocol dependent things.
	 * The default implementation does nothing
	 *
	 * @param params
	 */
	public default void setExtraParameters(Map<String,String>params){}

}