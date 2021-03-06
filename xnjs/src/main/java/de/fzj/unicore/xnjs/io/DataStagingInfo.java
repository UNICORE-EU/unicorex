package de.fzj.unicore.xnjs.io;

import de.fzj.unicore.xnjs.io.IFileTransfer.OverwritePolicy;

/**
 * common interface for {@link DataStageInInfo} and {@link DataStageOutInfo}
 * @author schuller
 */
public interface DataStagingInfo {

	public OverwritePolicy getOverwritePolicy();

	public void setOverwritePolicy(OverwritePolicy overwrite);

	public String getFileName();
	
	public String getID();
	
	/**
	 * should the local file be deleted after the job is finished?
	 */
	public boolean isDeleteOnTermination();
	
	public void setDeleteOnTermination(boolean ignoreFailure);
	
	/**
	 * get the data staging credentials associated with this transfer
	 * and contained in the original job description
	 */
	public DataStagingCredentials getCredentials();

	public void setCredentials(DataStagingCredentials credentials);
	
	/**
	 * should failures be ignored
	 */
	public boolean isIgnoreFailure();

	public void setIgnoreFailure(boolean ignoreFailure);
	
	/**
	 * file system name
	 */
	public String getFileSystemName();

	public void setFileSystemName(String fileSystemName);

}
