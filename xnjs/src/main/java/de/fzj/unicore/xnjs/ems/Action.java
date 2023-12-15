package de.fzj.unicore.xnjs.ems;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.fzj.unicore.xnjs.idb.ApplicationInfo;
import de.fzj.unicore.xnjs.io.DataStageInInfo;
import de.fzj.unicore.xnjs.io.DataStageOutInfo;
import de.fzj.unicore.xnjs.persistence.GSONUtils;
import eu.unicore.persist.annotations.ID;
import eu.unicore.persist.annotations.Table;
import eu.unicore.persist.util.JSON;
import eu.unicore.persist.util.Wrapper;
import eu.unicore.security.Client;

/**
 * An Action is a unit of work processed and managed by the XNJS, 
 * such as a job execution or a workflow
 *
 * @author schuller
 */
@Table(name="JOBS")
@JSON(customHandlers={Wrapper.WrapperConverter.class,GSONUtils.XmlBeansConverter.class})
public class Action implements Serializable {

	private static final long serialVersionUID=1L;

	private String UUID;

	private String type;

	// if internal, this action is not managed by an external entity
	private boolean isInternal = false;
	
	//optional the id assigned by the batch system
	private String BSID;

	private String bssDetails="";

	private Date terminationTime;

	//the EMS status of the action
	private int status = ActionStatus.CREATED;

	//the EMS transition status of the action
	private int transitionalStatus;

	private Client client;

	//human readable name
	private String jobName;

	//the description of the action as submitted
	private Wrapper<Serializable> ajd;

	private ActionResult result = new ActionResult();

	private final List<String> log = new ArrayList<>();

	//for storing state information used during processing
	private ProcessingContext processingContext = new ProcessingContext();

	//the context during execution
	private ExecutionContext executionContext;

	//the application information
	private ApplicationInfo applicationInfo;

	private List<DataStageInInfo>stageIns;
	
	private List<DataStageOutInfo>stageOuts;
	
	// if true, Action needs to be written back into persistence
	private transient boolean dirty=false;

	// if true, the action will wait for a wake-up event before continuing to be processed
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
	//list of states to send notifications for
	private List<String>notifyStates;
	//list of raw BSS states to send notifications for
	private List<String>notifyBSSStates;

	/**
	 * creates a new action with a pre-defined UUID</br>
	 * The caller has to guarantee uniqueness!
	 * 
	 * @param uuid - the UUID of the new Action. 
	 */
	public Action(String uuid){
		if(uuid==null)throw new IllegalArgumentException("UUID must be non-null.");
		UUID=uuid;
		dirty = true;
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

	public String getBssDetails() {
		return bssDetails;
	}

	public void setBssDetails(String details) {
		bssDetails = details;
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
		this.ajd = new Wrapper<>(ajd);
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

	public List<String> getNotifyStates() {
		return notifyStates;
	}

	public void setNotifyStates(List<String> notifyStates) {
		this.notifyStates = notifyStates;
	}

	public List<String> getNotifyBSSStates() {
		return notifyBSSStates;
	}

	public void setNotifyBSSStates(List<String> notifyBSSStates) {
		this.notifyBSSStates = notifyBSSStates;
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
		sb.append("Job definition: ").append(getAjd()).append("\n");
		return sb.toString();
	}
}
