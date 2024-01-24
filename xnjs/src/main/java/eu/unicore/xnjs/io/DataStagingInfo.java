package eu.unicore.xnjs.io;

import java.util.Map;

import eu.unicore.xnjs.io.IFileTransfer.OverwritePolicy;

/**
 * common interface for {@link DataStageInInfo} and {@link DataStageOutInfo}
 * @author schuller
 */
public interface DataStagingInfo extends Cloneable {

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

	/**
	 * extra, protocol-dependent parameters
	 */
	public void setExtraParameters(Map<String,String> parameters);

	public Map<String,String> getExtraParameters();

}
