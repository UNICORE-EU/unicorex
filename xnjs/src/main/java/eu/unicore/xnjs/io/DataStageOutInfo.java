package eu.unicore.xnjs.io;

import java.io.Serializable;
import java.net.URI;
import java.util.Map;

import eu.unicore.persist.util.Wrapper;
import eu.unicore.xnjs.io.IFileTransfer.OverwritePolicy;

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
	
	private Map<String,String> extraParameters = null;

	private String preferredLoginNode;

	public DataStageOutInfo(){}
	
	public void setID(String id) {
		this.id=id;
	}

	@Override
	public String getID() {
		return id;
	}

	@Override
	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	@Override
	public boolean isIgnoreFailure() {
		return ignoreFailure;
	}

	@Override
	public void setIgnoreFailure(boolean ignoreFailure) {
		this.ignoreFailure = ignoreFailure;
	}

	public boolean isPerformStageOutOnFailure() {
		return performStageOutOnFailure;
	}

	public void setPerformStageOutOnFailure(boolean performStageOutOnFailure) {
		this.performStageOutOnFailure = performStageOutOnFailure;
	}

	public URI getTarget() {
		return target;
	}

	public void setTarget(URI target) {
		this.target = target;
	}

	@Override
	public OverwritePolicy getOverwritePolicy() {
		return overwritePolicy;
	}

	@Override
	public void setOverwritePolicy(OverwritePolicy overwritePolicy) {
		this.overwritePolicy = overwritePolicy;
	}

	@Override
	public boolean isDeleteOnTermination() {
		return deleteOnTermination;
	}

	@Override
	public void setDeleteOnTermination(boolean deleteOnTermination) {
		this.deleteOnTermination = deleteOnTermination;
	}

	@Override
	public DataStagingCredentials getCredentials() {
		return credentials!=null? credentials.get():null;
	}

	@Override
	public void setCredentials(DataStagingCredentials credentials) {
		this.credentials = new Wrapper<DataStagingCredentials>(credentials);
	}

	@Override
	public String getFileSystemName() {
		return fileSystemName;
	}

	@Override
	public void setFileSystemName(String fileSystemName) {
		this.fileSystemName = fileSystemName;
	}

	@Override
	public Map<String, String> getExtraParameters() {
		return extraParameters;
	}

	@Override
	public void setExtraParameters(Map<String, String> extraParameters) {
		this.extraParameters = extraParameters;
	}

	@Override
	public String getPreferredLoginNode() {
		return preferredLoginNode;
	}

	@Override
	public void setPreferredLoginNode(String preferredLoginNode) {
		this.preferredLoginNode = preferredLoginNode;
	}

	@Override
	public DataStageOutInfo clone() throws CloneNotSupportedException {
		return (DataStageOutInfo)super.clone();
	}
	
	public String toString() {
		return "'"+fileName+"' -> "+target.toString();
	}
}
