package de.fzj.unicore.xnjs.io;

import java.io.Serializable;
import java.net.URI;

import de.fzj.unicore.xnjs.io.IFileTransfer.OverwritePolicy;
import eu.unicore.persist.util.Wrapper;

/**
 * Stores information about a single local file to be staged out
 * 
 * @author schuller
 */
public class DataStageOutInfo implements Serializable, DataStagingInfo {
	
	private static final long serialVersionUID=1L;

	private String id;
	
	private String fileName;
	
	private String fileSystemName;
	
	private URI target;
	
	private OverwritePolicy overwritePolicy=OverwritePolicy.OVERWRITE;
	
	private boolean ignoreFailure=false;
	
	private boolean deleteOnTermination=false;
	
	private boolean performStageOutOnFailure=true;
	
	private Wrapper<DataStagingCredentials> credentials=null;
	
	public DataStageOutInfo(){}
	
	/**
	 * get the ID of this data staging item 
	 */
	public void setID(String id) {
		this.id=id;
	}

	/**
	 * get the ID of this data staging item 
	 */
	public String getID() {
		return id;
	}

	/**
	 * get the local file name
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * set the local file name
	 */
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	/**
	 * should failure of the stage-out be ignored
	 * @return <code>true</code> if failure of the stage out does NOT cause the job to fail
	 */
	public boolean isIgnoreFailure() {
		return ignoreFailure;
	}

	/**
	 * set the ignoreFailure
	 * @param ignoreFailure <code>true</code> if failure of the stage out should NOT cause the job to fail
	 */
	public void setIgnoreFailure(boolean ignoreFailure) {
		this.ignoreFailure = ignoreFailure;
	}

	/**
	 * should this stage-out be performed if the user job was "not successful" 
	 * (i.e. depending on the exit code)
	 * @return <code>true</code> if the stage-out is dependent on the exit code
	 */
	public boolean isPerformStageOutOnFailure() {
		return performStageOutOnFailure;
	}

	/**
	 * should this stage-out be performed if the user job was "not successful" 
	 * (i.e. depending on the exit code)
	 * 
	 * @param performStageOutOnFailure
	 */
	public void setPerformStageOutOnFailure(boolean performStageOutOnFailure) {
		this.performStageOutOnFailure = performStageOutOnFailure;
	}

	public URI getTarget() {
		return target;
	}

	public void setTarget(URI target) {
		this.target = target;
	}

	public OverwritePolicy getOverwritePolicy() {
		return overwritePolicy;
	}

	public void setOverwritePolicy(OverwritePolicy overwritePolicy) {
		this.overwritePolicy = overwritePolicy;
	}

	public boolean isDeleteOnTermination() {
		return deleteOnTermination;
	}

	public void setDeleteOnTermination(boolean deleteOnTermination) {
		this.deleteOnTermination = deleteOnTermination;
	}

	public DataStagingCredentials getCredentials() {
		return credentials!=null? credentials.get():null;
	}

	public void setCredentials(DataStagingCredentials credentials) {
		this.credentials = new Wrapper<DataStagingCredentials>(credentials);
	}

	public String getFileSystemName() {
		return fileSystemName;
	}

	public void setFileSystemName(String fileSystemName) {
		this.fileSystemName = fileSystemName;
	}

	@Override
	public DataStageOutInfo clone() throws CloneNotSupportedException {
		return (DataStageOutInfo)super.clone();
	}
	
	public String toString() {
		return "'"+fileName+"' -> "+target.toString();
	}
}
