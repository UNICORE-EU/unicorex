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

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.fzj.unicore.persist.annotations.ID;
import de.fzj.unicore.persist.annotations.Table;
import de.fzj.unicore.persist.util.JSON;
import de.fzj.unicore.persist.util.Wrapper;
import de.fzj.unicore.xnjs.idb.ApplicationInfo;
import de.fzj.unicore.xnjs.io.DataStageInInfo;
import de.fzj.unicore.xnjs.io.DataStageOutInfo;
import de.fzj.unicore.xnjs.persistence.GSONUtils;
import eu.unicore.security.Client;

/**
 * An Action is something that is managed by the XNJS, 
 * such as a job execution or a workflow<br>
 * 
 * @author schuller
 */
@Table(name="JOBS")
@JSON(customHandlers={GSONUtils.XmlBeansConverter.class,Wrapper.WrapperConverter.class})
public class Action implements Serializable {

	private static final long serialVersionUID=1L;

	//the unique id of the action
	private String UUID;

	//the type of action
	private String type;

	// if internal, this action is not managed by an external entity
	private boolean isInternal = false;
	
	//optional the id assigned by the batch system
	private String BSID;

	//the time by which the action will no longer be valid
	private Date terminationTime;

	//the EMS status of the action
	private int status;

	//the EMS transition status of the action
	private int transitionalStatus;

	//the owner of this action
	private Client client;

	//human readable name
	private String jobName;

	//the description of the action as submitted
	private Wrapper<Serializable> ajd;

	private ActionResult result;

	//a log trace
	private final List<String> log;

	//a context for use during processing: can use arbitrary objects objects
	private ProcessingContext processingContext;

	//the context during execution
	private ExecutionContext executionContext;

	//the application information
	private ApplicationInfo applicationInfo;

	//list of stage-ins
	private List<DataStageInInfo>stageIns;
	
	//list of stage-outs
	private List<DataStageOutInfo>stageOuts;
	
	//if this is true the action has been modified and needs to be persisted
	private transient boolean dirty=false;

	//if this is true the action has been waiting and will wait for
	//a wake-up event before continuing to be processed
	//this is transient, so it gets cleared in case of a server restart
	private transient boolean waiting=false;

	//the instant until this action should not be processed further
	private long notBefore;
	
	//Optional umask setting for this action. Is carried here as it is set by the TSS.
	private String umask;

	//the ID of the "parent" action
	private String parentActionID;

	//the ID of the "root" action
	private String rootActionID;

	public static final String AUTO_SUBMIT="EMS_AUTOSUBMIT";

	//list of URLs to send notifications to
	private List<String>notificationURls;
		
	/**
	 * creates a new action with a pre-defined UUID</br>
	 * The caller has to guarantee uniqueness!
	 * 
	 * @param uuid - the UUID of the new Action. 
	 */
	public Action(String uuid){
		UUID=uuid;
		if(UUID==null)throw new IllegalArgumentException("UUID must be non-null.");
		log=new ArrayList<String>();
		processingContext=new ProcessingContext();
		setResult(new ActionResult());
		setStatus(ActionStatus.CREATED);
		addLogTrace("Created with ID "+getUUID());
		setDirty();
	}
	
	/**
	 * creates a new Action
	 */
	public Action(){
		this(java.util.UUID.randomUUID().toString());
	}

	/**
	 * Returns the batch system ID
	 */
	public String getBSID() {
		return BSID;
	}
	
	/**
	 * @param bsid The BSID to set.
	 */
	public void setBSID(String bsid) {
		BSID = bsid;
		setDirty();
	}

	@ID
	public String getUUID() {
		return UUID;
	}

	public int getStatus() {
		return status;
	}

	/**
	 * Returns the status in a human-readable form
	 */
	public String getStatusAsString() {
		return ActionStatus.toString(status);
	}

	/**
	 * Set the action status. 
	 * This method is only to be invoked by the EMS itself, not in client code.
	 * @param status The status to set.
	 */
	public void setStatus(int status) {
		this.status = status;
		setDirty();
	}

	public java.util.Date getTerminationTime() {
		return terminationTime!=null?(Date)terminationTime.clone():null;
	}

	public void setTerminationTime(java.util.Date terminationTime) {
		this.terminationTime = terminationTime!=null?(Date)terminationTime.clone():null;
		setDirty();
	}

	/**
	 * Returns the job description
	 */
	public Object getAjd() {
		return ajd!=null?ajd.get():null;
	}

	public void setAjd(Serializable ajd) {
		this.ajd = new Wrapper<Serializable>(ajd);
		setDirty();
	}

	public void setUUID(String uuid) {
		UUID = uuid;
		setDirty();
	}

	public Client getClient() {
		return client;
	}

	public void setClient(Client client) {
		this.client = client;
		setDirty();
	}

	/**
	 * add a log entry to this action. A time stamp is added automatically
	 */
	public void addLogTrace(String tr){
		log.add(new Date().toString()+": "+tr);
		setDirty();
	}

	/**
	 * print the full logtrace to stdout
	 */
	public void printLogTrace(){
		PrintWriter pw=new PrintWriter(System.out);
		printLogTrace(pw);
		pw.flush();
	}

	/**
	 * print the full logtrace to the given PrintWriter
	 */
	public void printLogTrace(PrintWriter writer){
		writer.println("Action id: "+getUUID());
		for(String s: log){
			writer.println(s);
		}
	}
	/**
	 * append an actions logtrace to the logtrace of this action
	 * @param a the source of the logtrace
	 */
	public void appendLogTraceFrom(Action a){
		if(this.equals(a))return;
		List<String> l=a.getLog();
		for(String s: l)addLogTrace(s);
	}

	public List<String> getLog() {
		return log;
	}

	/**
	 * returns the action type
	 */
	public String getType() {
		return type;
	}


	/**
	 * set the type of this action
	 */
	public void setType(String type) {
		this.type = type;
		setDirty();
	}


	public ProcessingContext getProcessingContext() {
		return processingContext;
	}

	public void setProcessingContext(ProcessingContext processingContext) {
		this.processingContext = processingContext;
		setDirty();
	}

	public ActionResult getResult() {
		return result;
	}

	public void setResult(ActionResult result) {
		this.result = result;
		setDirty();
	}

	public ExecutionContext getExecutionContext() {
		return executionContext;
	}

	public void setExecutionContext(ExecutionContext executionContext) {
		this.executionContext = executionContext;
	}

	public ApplicationInfo getApplicationInfo() {
		return applicationInfo;
	}

	public void setApplicationInfo(ApplicationInfo applicationInfo) {
		this.applicationInfo = applicationInfo;
	}

	public List<DataStageInInfo> getStageIns() {
		return stageIns;
	}

	public void setStageIns(List<DataStageInInfo> stageIns) {
		this.stageIns = stageIns;
	}

	public List<DataStageOutInfo> getStageOuts() {
		return stageOuts;
	}

	public void setStageOuts(List<DataStageOutInfo> stageOuts) {
		this.stageOuts = stageOuts;
	}
	
	public void setWaitForClientStageIn(boolean waitForClientStageIn){
		processingContext.put(AUTO_SUBMIT, Boolean.valueOf(waitForClientStageIn));
	}
	
	public List<String> getNotificationURLs() {
		return notificationURls;
	}

	public void setNotificationURLs(List<String> notificationURls) {
		this.notificationURls = notificationURls;
	}

	public int getTransitionalStatus() {
		return transitionalStatus;
	}

	public void setTransitionalStatus(int transitionalStatus) {
		this.transitionalStatus = transitionalStatus;
		setDirty();
	}

	public String getJobName() {
		return jobName;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
		setDirty();
	}

	public String getUmask() {
		return umask;
	}

	public void setUmask(String umask) {
		this.umask = umask;
	}

	public void setDirty(){dirty=true;}
	public boolean isDirty(){return dirty;}
	public void clearDirty(){dirty=false;}

	public boolean isWaiting(){return waiting;}

	public void setWaiting(boolean waiting){
		this.waiting=waiting;
	}

	public long getNotBefore() {
		return notBefore;
	}

	/**
	 * set the instant until which the action should not be processed further
	 */
	public void setNotBefore(long notBefore) {
		this.notBefore = notBefore;
		addLogTrace("Further processing scheduled for "+new Date(notBefore));
		setDirty();
	}

	
	/**
	 * get the ID of the parent action
	 * 
	 * @return parent ID or null of this action does not have a parent
	 */
	public String getParentActionID() {
		return parentActionID;
	}

	public void setParentActionID(String parentActionID) {
		this.parentActionID = parentActionID;
	}

	/**
	 * get the ID of the "root" action (i.e. the ultimate parent) of this action
	 * 
	 * @return root action ID or the UUID if this action is a root action itself
	 */
	public String getRootActionID() {
		return rootActionID!=null?rootActionID:UUID;
	}

	/**
	 * store the ID of the "root" action (i.e. the ultimate parent) of this action
	 */
	public void setRootActionID(String rootActionID) {
		this.rootActionID = rootActionID;
	}

	public boolean isInternal() {
		return isInternal;
	}

	public void setInternal(boolean isInternal) {
		this.isInternal = isInternal;
	}

	/**
	 * helper method that sets the action to "FAILED"
	 */
	public void fail(){
		fail(null);
	}
	
	/**
	 * helper method that sets the action to "FAILED"
	 * 
	 * @param errorMessage
	 */
	public void fail(String errorMessage){
		String msg=errorMessage!=null?"Failed: "+errorMessage:"Failed.";
		ActionResult res=new ActionResult(ActionResult.NOT_SUCCESSFUL,msg);
		if(executionContext!=null && executionContext.getExitCode()!=null){
			res.setExitCode(executionContext.getExitCode());
		}
		setResult(res);
		addLogTrace("Result: "+msg);
		setStatus(ActionStatus.DONE);
		addLogTrace("Status set to DONE.");
	}
	
	public String toString(){
		StringBuilder sb=new StringBuilder();
		sb.append("Action ID       : ").append(UUID).append("\n");
		sb.append("Action type     : ").append(type).append("\n");
		sb.append("Status          : ").append(getStatusAsString());
		sb.append(" ").append(ActionStatus.transitionalStatus(getTransitionalStatus())).append("\n");
		sb.append("Result          : ").append(result).append("\n");
		try{
			sb.append("Owner           : ").append(client.getDistinguishedName()).append("\n");
		}catch(Exception e){
			sb.append("Owner           : not defined.\n");
		}
		if(rootActionID!=null){
			sb.append("Root action ID       : ").append(rootActionID).append("\n");
		}
		sb.append("Job Definition: ").append(getAjd()).append("\n");
		return sb.toString();
	}
}
