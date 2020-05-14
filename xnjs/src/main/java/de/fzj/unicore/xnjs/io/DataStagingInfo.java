package de.fzj.unicore.xnjs.io;

import de.fzj.unicore.xnjs.io.IFileTransfer.OverwritePolicy;

/**
 * common interface for {@link DataStageInInfo} and {@link DataStageOutInfo}
 * @author schuller
 */
public interface DataStagingInfo {

	public OverwritePolicy getOverwritePolicy();

	/**
	 * get the local file name
	 */
	public String getFileName();
	
	/**
	 * get the ID of this transfer
	 */
	public String getID();
	
	/**
	 * should the local file be deleted after the job is finished?
	 */
	public boolean isDeleteOnTermination();

	/**
	 * get the data staging credentials associated with this transfer
	 * and contained in the original job description
	 */
	public DataStagingCredentials getCredentials();

	/**
	 * should failures be ignored
	 */
	public boolean isIgnoreFailure();

	/**
	 * get the file system name
	 */
	public String getFileSystemName();

}
