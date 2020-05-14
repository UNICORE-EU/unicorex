package de.fzj.unicore.uas.client;

import java.math.BigInteger;
import java.util.Calendar;

import org.apache.xmlbeans.XmlObject;
import org.unigrids.services.atomic.types.StatusInfoType;
import org.unigrids.services.atomic.types.StatusType;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.Task;
import eu.unicore.util.httpclient.IClientConfiguration;
import eu.unicore.unicore6.task.CancelRequestDocument;
import eu.unicore.unicore6.task.ResultDocument.Result;
import eu.unicore.unicore6.task.SubmissionTimeDocument;
import eu.unicore.unicore6.task.TaskPropertiesDocument;

/**
 * client for talking to a Task service
 * 
 * @author schuller
 */
public class TaskClient extends BaseUASClient {

	private Calendar submissionTime;

	private Integer exitCode;

	public TaskClient(String url, EndpointReferenceType epr, IClientConfiguration sec) throws Exception {
		super(url, epr, sec);
	}

	public TaskClient(EndpointReferenceType epr, IClientConfiguration sec) throws Exception {
		this(epr.getAddress().getStringValue(), epr, sec);
	}

	public TaskPropertiesDocument getResourcePropertiesDocument()throws Exception{
		return TaskPropertiesDocument.Factory.parse(GetResourcePropertyDocument().getGetResourcePropertyDocumentResponse().newInputStream());
	}

	/**
	 * get the Task's result or <code>null</code> if not available
	 * @throws Exception
	 */
	public XmlObject getResult()throws Exception{
		Result result=getResourcePropertiesDocument().getTaskProperties().getResult();
		if(result==null)return null;
		return XmlObject.Factory.parse(result.newXMLStreamReader());
	}
	/**
	 * return the task status
	 * @throws Exception
	 */
	public StatusType.Enum getStatus() throws Exception{
		return getStatusInfo().getStatus();
	}

	/**
	 * return the exit code of the task or <code>null</code> if not available
	 */
	public Integer getExitCode()throws Exception{
		if(exitCode==null){
			StatusInfoType sit=getStatusInfo();
			if(sit.isSetExitCode()){
				BigInteger eCode=sit.getExitCode();
				if(eCode!=null)exitCode=eCode.intValue();	
			}
		}
		return exitCode;
	}


	/**
	 * return the progress of the task as a value between 0 and 1 
	 * or <code>null</code> if progress value not available
	 */
	public Float getProgress()throws Exception{
		StatusInfoType status=getStatusInfo();
		if(status.isSetProgress()){
			return status.getProgress();
		}
		return null;
	}

	/**
	 * get the time at which the task was submitted
	 * @throws Exception
	 */
	public Calendar getSubmissionTime()throws Exception{
		if(submissionTime==null)
		{
			submissionTime=getSingleResourceProperty(SubmissionTimeDocument.class).getSubmissionTime();
		}
		return submissionTime;
	}

	public String getStatusMessage()throws Exception{
		return getResourcePropertiesDocument().getTaskProperties().getStatusInfo().getDescription();
	}

	/**
	 * Cancel execution of the task
	 * @throws Exception
	 */
	public void cancel()throws Exception{
		CancelRequestDocument c=CancelRequestDocument.Factory.newInstance();
		c.addNewCancelRequest();
		makeProxy(Task.class).Cancel(c);
	}
	
	/**
	 * convenience method that waits until a job has finished
	 * and returns the final status (SUCCESSFUL or FAILED)
	 * @param timeout in milliseconds (null for no timeout)
	 */
	public String waitUntilDone(int timeout) throws Exception{
		StatusType.Enum status=StatusType.UNDEFINED;
		long start=System.currentTimeMillis();
		long elapsed=0;
		while(true){
			if(timeout>0 && elapsed>timeout)break;
			elapsed=System.currentTimeMillis()-start;
			status=getStatus();
			if(status.equals(StatusType.SUCCESSFUL)){
				logger.info("Task finished successfully.");
				break;
			}
			else if(status.equals(StatusType.FAILED)){
				String statusDescription=getStatusMessage();
				logger.info("Task failed."+(statusDescription!=null?" The error was: "+statusDescription:""));
				break;
			}
			Thread.sleep(500);
		}
		return status.toString();
	}
	
	protected StatusInfoType getStatusInfo() throws Exception {
		return getResourcePropertiesDocument().getTaskProperties().getStatusInfo();
	}
}
