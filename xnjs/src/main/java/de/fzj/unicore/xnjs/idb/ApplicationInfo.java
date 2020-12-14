package de.fzj.unicore.xnjs.idb;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.fzj.unicore.xnjs.resources.ResourceRequest;

/**
 * Holds information required to execute an application on the backend system.
 * It is generated from the user job and an application template from the IDB.
 * 
 * @author schuller
 */
public class ApplicationInfo implements Cloneable, Serializable {

	private static final long serialVersionUID = 1L;

	private String name, version, description;

	private String executable;

	//arguments
	private final List<String> arguments = new ArrayList<>();

	//environment variables
	private final Map<String,String> environment = new HashMap<>();

	private String stdin,stdout,stderr; 

	private String userPreCommand;
	private boolean userPreOnLoginNode=true;
	private boolean userPreIgnoreExitCode=false;
	private String userPostCommand;
	private boolean userPostOnLoginNode=true;
	private boolean userPostIgnoreExitCode=false;
	
	private boolean runOnLoginNode=false;
	private String preferredLoginNode;
	private String rawBatchFile;
	
	private boolean ignoreNonZeroExitCode = false;
	
	// pre/post-command 
	// (run on login node before submitting main job to batch system)
	private String preCommand;
	private String postCommand;
	
	// pre-command (e.g. module loads), goes into batch script
	private String prologue;
	
	// post-command, goes into batch script
	private String epilogue;
			
	// default resource spec - can be overridden by client
	private List<ResourceRequest> resourceRequests = new ArrayList<>();

	private ApplicationMetadata appMetadata;
	
	public ApplicationInfo(){}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public ApplicationMetadata getMetadata() {
		return appMetadata;
	}

	public void setMetadata(ApplicationMetadata applicationMetadata) {
		this.appMetadata = applicationMetadata;
	}
	
	public void setPreCommand(String preCommand) {
		this.preCommand = preCommand;
	}
	
	public String getPreCommand() {
		return preCommand;
	}

	public String getPostCommand() {
		return postCommand;
	}

	public void setPostCommand(String postCommand) {
		this.postCommand = postCommand;
	}
	
	public String getPrologue() {
		return prologue;
	}

	public void setPrologue(String prologue) {
		this.prologue = prologue;
	}

	public String getEpilogue() {
		return epilogue;
	}

	public void setEpilogue(String epilogue) {
		this.epilogue = epilogue;
	}

	public List<ResourceRequest> getResourceRequests() {
		return resourceRequests;
	}
	
	public void setResourceRequest(List<ResourceRequest> resourceRequests) {
		this.resourceRequests = resourceRequests;
	}
	
	public String getExecutable() {
		return executable;
	}

	public void setExecutable(String executable) {
		this.executable = executable;
	}

	public Map<String, String> getEnvironment() {
		return environment;
	}

	public List<String> getArguments() {
		return arguments;
	}

	public void setArguments(String[] arguments) {
		this.arguments.clear();
		this.arguments.addAll(Arrays.asList(arguments));
	}

	public void addArgument(String argument) {
		this.arguments.add(argument);
	}

	public String getStdin() {
		return stdin;
	}

	public void setStdin(String stdin) {
		this.stdin = stdin;
	}

	public String getStdout() {
		return stdout;
	}

	public void setStdout(String stdout) {
		this.stdout = stdout;
	}

	public String getStderr() {
		return stderr;
	}

	public void setStderr(String stderr) {
		this.stderr = stderr;
	}

	public String getUserPreCommand() {
		return userPreCommand;
	}
	
	public void setUserPreCommand(String userPreCommand) {
		this.userPreCommand = userPreCommand;
	}

	public boolean isUserPreCommandOnLoginNode() {
		return userPreOnLoginNode;
	}
	
	public void setUserPreCommandOnLoginNode(boolean loginNode) {
		this.userPreOnLoginNode = loginNode;
	}
	
	public boolean isUserPreCommandIgnoreExitCode() {
		return userPreIgnoreExitCode;
	}
	
	public void setUserPreCommandIgnoreExitCode(boolean ignore) {
		this.userPreIgnoreExitCode = ignore;
	}
	
	public String getUserPostCommand() {
		return userPostCommand;
	}

	public void setUserPostCommand(String userPostCommand) {
		this.userPostCommand = userPostCommand;
	}

	public boolean isUserPostCommandOnLoginNode() {
		return userPostOnLoginNode;
	}
	
	public void setUserPostCommandOnLoginNode(boolean loginNode) {
		this.userPostOnLoginNode = loginNode;
	}
	
	public boolean isUserPostCommandIgnoreExitCode() {
		return userPostIgnoreExitCode;
	}
	
	public void setUserPostCommandIgnoreExitCode(boolean ignore) {
		this.userPostIgnoreExitCode = ignore;
	}
	
	public boolean isRunOnLoginNode() {
		return runOnLoginNode;
	}
	
	public void setRunOnLoginNode(boolean loginNode) {
		this.runOnLoginNode = loginNode;
	}

	public String getPreferredLoginNode() {
		return preferredLoginNode;
	}

	public void setPreferredLoginNode(String preferredLoginNode) {
		this.preferredLoginNode = preferredLoginNode;
	}

	public boolean ignoreNonZeroExitCode() {
		return ignoreNonZeroExitCode;
	}

	public void setIgnoreNonZeroExitCode(boolean ignoreNonZeroExitCode) {
		this.ignoreNonZeroExitCode = ignoreNonZeroExitCode;
	}

	public boolean isRawJob() {
		return rawBatchFile!=null;
	}
	
	public String getRawBatchFile() {
		return rawBatchFile;
	}

	public void setRawBatchFile(String rawBatchFile) {
		this.rawBatchFile = rawBatchFile;
	}

	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("Application ");
		if(getName()!=null){
			sb.append("<");
			sb.append(getName());
			if(getVersion()!=null){
				sb.append(":").append(getVersion());
			}
			sb.append(">");
		}
		else{
			sb.append("<unnamed>");
		}
		return sb.toString();
	}
	
	public boolean matches(String name, String version){
		if(!name.equals(name))return false;
		if(version==null)return true;
		return version.equals(version);
	}
	

	public ApplicationInfo clone(){
		ApplicationInfo clone = new ApplicationInfo();
		clone.name = this.name;
		clone.version = this.version;
		clone.description = this.description;
		clone.executable = this.executable;
		clone.arguments.addAll(this.arguments);
		clone.environment.putAll(this.environment);		
		clone.stdin = this.stdin;
		clone.stdout = this.stdout;
		clone.stderr = this.stderr;
		clone.userPreCommand = this.userPreCommand;
		clone.userPreOnLoginNode = this.userPreOnLoginNode;
		clone.userPostCommand = this.userPostCommand;
		clone.userPostOnLoginNode = this.userPostOnLoginNode;
		clone.runOnLoginNode = this.runOnLoginNode;
		clone.preferredLoginNode = this.preferredLoginNode;
		clone.rawBatchFile = this.rawBatchFile;
		clone.ignoreNonZeroExitCode = this.ignoreNonZeroExitCode;
		clone.preCommand = this.preCommand;
		clone.postCommand = this.postCommand;
		clone.prologue = this.prologue;
		clone.epilogue = this.epilogue;
		clone.resourceRequests.addAll(this.resourceRequests);
		clone.appMetadata = this.appMetadata;
		
		return clone;
	}

	public static enum JobType {
		BATCH, INTERACTIVE, RAW,
	}
	
}
