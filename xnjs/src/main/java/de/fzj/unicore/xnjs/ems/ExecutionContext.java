/*********************************************************************************
 * Copyright (c) 2006 Forschungszentrum Juelich GmbH 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * (1) Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the disclaimer at the end. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * 
 * (2) Neither the name of Forschungszentrum Juelich GmbH nor the names of its 
 * contributors may be used to endorse or promote products derived from this 
 * software without specific prior written permission.
 * 
 * DISCLAIMER
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *********************************************************************************/
 

package de.fzj.unicore.xnjs.ems;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.fzj.unicore.xnjs.resources.ResourceRequest;
import de.fzj.unicore.xnjs.tsi.remote.TSIMessages;
import de.fzj.unicore.xnjs.tsi.remote.TSIUtils;

/**
 * The execution context defines a job's execution environment<br>
 * 
 * @author schuller
 */
public class ExecutionContext implements Serializable{
	
	private static final long serialVersionUID = 1l;
	
	private String workingDirectory = "./";
	
	//the outcome dir (by default equal to the working dir)
	private String outcomeDirectory;
	
	private HashMap<String,String> environment = new HashMap<>();
	
	//some id where this job is running (node, ip, url, whatever)
	private String location;
	
	//in/out/error locations
	private String stdout="stdout",stderr="stderr",stdin;
	
	//the executable
	private String executable;
	
	//should the execution bypass the batch system?
	private boolean runOnLoginNode;
	
	//PID file name (for interactive execution on the login node)
	private String pidFileName = TSIMessages.PID_FILENAME;
		
	//the process exit code
	private Integer exitCode;
	
	//the process exit code
	private Float progress;

	//the resources requested for executing the job
	private List<ResourceRequest> resourceRequest = new ArrayList<>();

	private String exitCodeFileName = TSIMessages.EXITCODE_FILENAME;
	
	//the preferred execution host (hostname or IP)
	private String preferredExecutionHost;
	
	//the umask as octal string
	private String umask;
	
	//the queue the job is running
	private String batchQueue;
	
	//the time the job is estimated to finish execution
	private long estimatedEndtime;
	
	//should the execution output be discarded?
	private boolean discardOutput;
	
	//should non-zero exit code lead to failure?
	private boolean ignoreExitCode;

	public HashMap<String,String> getEnvironment() {
		return environment;
	}
	
	public void setEnvironment(HashMap<String,String> environment) {
		this.environment = environment;
	}
	
	public String getLocation() {
		return location;
	}
	
	public void setLocation(String location) {
		this.location = location;
	}
	/**
	 * @return workingDirectory (which is guaranteed to end with the target system's file separator)
	 */
	public String getWorkingDirectory() {
		return workingDirectory;
	}

	public void setWorkingDirectory(String workingDirectory) {
		this.workingDirectory = workingDirectory;
	}

	/**
	 * @return outcome directory (which is guaranteed to end with the target system's file separator)
	 */
	public String getOutcomeDirectory() {
		return outcomeDirectory!=null? outcomeDirectory : workingDirectory;
	}
	
	public void setOutcomeDirectory(String outcomeDirectory) {
		this.outcomeDirectory = outcomeDirectory;
	}

	public String getStderr() {
		return stderr;
	}

	public void setStderr(String stderr) {
		this.stderr = stderr;
	}

	public String getStdout() {
		return stdout;
	}

	public void setStdout(String stdout) {
		this.stdout = stdout;
	}

	public String getStdin() {
		return stdin;
	}

	public void setStdin(String stdin) {
		this.stdin = stdin;
	}
	
	/**
	 * return the exit code (=null if undefined)
	 */
	public Integer getExitCode() {
		return exitCode;
	}
	
	public void setExitCode(int exitCode) {
		this.exitCode = exitCode;
	}
	
	public String getExitCodeFileName() {
		return exitCodeFileName;
	}
	
	public void setExitCodeFileName(String exitCodeFileName) {
		this.exitCodeFileName = exitCodeFileName;
	}
	
	/**
	 * if <code>true</code> the job will be run on the front-end node, and not
	 * submitted to the batch system
	 */
	public boolean isRunOnLoginNode() {
		return runOnLoginNode;
	}
	
	/**
	 * specify that the execution should be done on the front-end node, i.e. 
	 * not submitted to the batch system
	 */
	public void setRunOnLoginNode(boolean runOnLoginNode) {
		this.runOnLoginNode = runOnLoginNode;
	}

	public boolean isIgnoreExitCode() {
		return ignoreExitCode;
	}

	public void setIgnoreExitCode(boolean ignore) {
		this.ignoreExitCode = ignore;
	}

	/**
	 * Get the progress indication value
	 *
	 * @return a value between 0 and 1 indicating the progress of a running job
	 */
	public Float getProgress() {
		return progress;
	}
	
	/**
	 * Set the progress indication value. Required to be between 0 and 1, otherwise 
	 * a {@link IllegalArgumentException} is thrown

	 * @param progress - the progress
	 * @throws IllegalArgumentException if the progress is non-null and not between 0 and 1
	 */
	public void setProgress(Float progress) {
		if(progress!=null){
			if(progress<0)throw new IllegalArgumentException("Progress value is required to be >=0.");
			if(progress>1)throw new IllegalArgumentException("Progress value is required to be <=1.");
		}
		this.progress = progress;
	}
	public String getExecutable() {
		return executable;
	}
	public void setExecutable(String executable) {
		this.executable = executable;
	}
	
	/**
	 * get the resource request
	 */
	public List<ResourceRequest> getResourceRequest() {
		return resourceRequest;
	}
	
	/**
	 * set the resources to be requested from the system for executing the job
	 * 
	 * @param resourceRequest
	 */
	public void setResourceRequest(List<ResourceRequest> resourceRequest) {
		this.resourceRequest = resourceRequest;
	}

	public String getPreferredExecutionHost() {
		return preferredExecutionHost;
	}
	
	public void setPreferredExecutionHost(String preferredExecutionHost) {
		this.preferredExecutionHost = preferredExecutionHost;
	}
	
	public String getUmask() {
		return umask;
	}
	
	public void setUmask(String umask) {
		this.umask = umask;
	}

	public String getPIDFileName() {
		return pidFileName;
	}

	public void setPIDFileName(String pidFileName) {
		this.pidFileName = pidFileName;
	}
	
	/**
	 * the batch queue the job is (or was) running in (can be <code>null</code> if not known)
	 */
	public String getBatchQueue() {
		return batchQueue;
	}
	

	/**
	 * the batch queue the job is (or was) running in <br/>
	 * this is <b>not for requesting</b> a certain queue, use the resource set to do that!
	 */
	public void setBatchQueue(String batchQueue) {
		this.batchQueue = batchQueue;
	}
	
	/**
	 * get the estimated(!) instant the job will finish (can be <code>0</code> if not known)
	 */
	public long getEstimatedEndtime() {
		return estimatedEndtime;
	}
	
	public void setEstimatedEndtime(long estimatedEndtime) {
		this.estimatedEndtime = estimatedEndtime;
	}
	
	/**
	 * (scripts only) should the output (stderr and stdout) be discarded?
	 */
	public boolean isDiscardOutput() {
		return discardOutput;
	}
	
	/**
	 * (scripts only) should the output (stderr and stdout) be discarded?
	 * @param discardOutput - if <code>true</code>, output will be discarded
	 */
	public void setDiscardOutput(boolean discardOutput) {
		this.discardOutput = discardOutput;
	}

}
