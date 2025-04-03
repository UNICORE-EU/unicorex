package eu.unicore.xnjs.io;

import java.io.Serializable;
import java.net.URI;
import java.util.Arrays;
import java.util.Map;

import eu.unicore.persist.util.Wrapper;
import eu.unicore.xnjs.io.IFileTransfer.ImportPolicy;
import eu.unicore.xnjs.io.IFileTransfer.OverwritePolicy;

/**
 * Stores information about a single local file to be staged in
 * 
 * @author schuller
 */
public class DataStageInInfo implements Serializable, DataStagingInfo {

	private static final long serialVersionUID=1L;

	private String id;

	private String fileName;

	private String fileSystemName;

	//in case of a file import, multiple alternative sources could be given
	private URI[] sources;

	//if true, sources may be used concurrently for performance
	//if false, sources are used as alternatives for fault tolerance
	private boolean concurrentSources=false;

	private OverwritePolicy overwritePolicy=OverwritePolicy.OVERWRITE;

	private ImportPolicy importPolicy=ImportPolicy.PREFER_COPY;

	private boolean deleteOnTermination=false;

	private boolean ignoreFailure=false;

	//can the file be shared between jobs? 
	private boolean allowShare=false;

	private Wrapper<DataStagingCredentials> credentials=null;

	private String inlineData;

	private Map<String,String> extraParameters = null;

	private String permissions;

	private String preferredLoginNode;

	public DataStageInInfo(){}

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

	public URI[] getSources() {
		return sources;
	}

	public void setSources(URI[] sources) {
		this.sources = sources;
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
	public boolean isIgnoreFailure() {
		return ignoreFailure;
	}

	@Override
	public void setIgnoreFailure(boolean ignoreFailure) {
		this.ignoreFailure = ignoreFailure;
	}

	public boolean isConcurrentSources() {
		return concurrentSources;
	}

	/**
	 * select whether the sources are to be used concurrently
	 * 
	 * TODO not really supported yet!!!
	 */
	public void setConcurrentSources(boolean concurrentSources) {
		this.concurrentSources = concurrentSources;
	}

	@Override
	public OverwritePolicy getOverwritePolicy() {
		return overwritePolicy;
	}

	@Override
	public void setOverwritePolicy(OverwritePolicy overwritePolicy) {
		this.overwritePolicy = overwritePolicy;
	}

	public ImportPolicy getImportPolicy() {
		return importPolicy;
	}

	public void setImportPolicy(ImportPolicy importPolicy) {
		this.importPolicy = importPolicy;
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

	public boolean isAllowShare() {
		return allowShare;
	}

	public void setAllowShare(boolean allowShare) {
		this.allowShare = allowShare;
	}

	public String getInlineData() {
		return inlineData;
	}

	public void setInlineData(String inlineData) {
		this.inlineData = inlineData;
	}
	
	@Override
	public Map<String, String> getExtraParameters() {
		return extraParameters;
	}

	@Override
	public void setExtraParameters(Map<String, String> extraParameters) {
		this.extraParameters = extraParameters;
	}

	public String getPermissions() {
		return permissions;
	}

	public void setPermissions(String permissions) {
		this.permissions = permissions;
	}

	@Override
	public String getPreferredLoginNode() {
		return preferredLoginNode;
	}

	@Override
	public void setPreferredLoginNode(String preferredLoginNode) {
		this.preferredLoginNode = preferredLoginNode;
	}

	public String toString(){
		return Arrays.asList(sources)+" -> "+fileName;
	}
	
	@Override
	public DataStageInInfo clone() throws CloneNotSupportedException {
		return (DataStageInInfo)super.clone();
	}
}
