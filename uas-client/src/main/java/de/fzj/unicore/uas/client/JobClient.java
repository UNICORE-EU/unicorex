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
 ********************************************************************************/
 

package de.fzj.unicore.uas.client;

import java.math.BigInteger;
import java.util.Calendar;

import org.apache.log4j.Logger;
import org.unigrids.services.atomic.types.StatusInfoType;
import org.unigrids.services.atomic.types.StatusType;
import org.unigrids.x2006.x04.services.jms.AbortDocument;
import org.unigrids.x2006.x04.services.jms.HoldDocument;
import org.unigrids.x2006.x04.services.jms.JobPropertiesDocument;
import org.unigrids.x2006.x04.services.jms.RestartDocument;
import org.unigrids.x2006.x04.services.jms.ResumeDocument;
import org.unigrids.x2006.x04.services.jms.StartDocument;
import org.unigrids.x2006.x04.services.jms.SubmissionTimeDocument;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.JobManagement;
import de.fzj.unicore.wsrflite.xfire.ClientException;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.IClientConfiguration;

/**
 * a client to manage a Job resource and to access the job working directory (Uspace).
 * It can be used to
 * <ul>
 *   <li>upload data (using the StorageClient provided by {@link #getUspaceClient()})
 *   <li>start the job
 *   <li>wait until it finishes
 *   <li>download results (using the StorageClient provided by {@link #getUspaceClient()})
 * </ul>
 *
 * @author schuller
 */
public class JobClient extends BaseUASClient   {
	
	private static final Logger logger=Log.getLogger(Log.CLIENT,JobClient.class);

	private Calendar submissionTime;
	private Integer exitCode;
	private String statusDescription;
	
	private final JobManagement jobService;
	
	/**
	 * connect to the Job service at EPR 'address' using the given URL
	 * @param url - the URL of the job
	 * @param address - the EPR of the job
	 * @param sec - the security settings to use
	 */
	public JobClient(String url, EndpointReferenceType address, IClientConfiguration sec)throws Exception {
		super(url, address,sec);
		jobService=makeProxy(JobManagement.class);
	} 

	/**
	 * connect to the Job service at EPR 'address'
	 * @param address - the EPR of the jon
	 * @param sec - the security settings to use
	 * @throws Exception
	 */
	public JobClient(EndpointReferenceType address, IClientConfiguration sec)throws Exception {
		this(address.getAddress().getStringValue(),address,sec);
	}

	/**
	 * returns the service's JobPropertiesDocument
	 */
	public JobPropertiesDocument getResourcePropertiesDocument()throws Exception{
		return JobPropertiesDocument.Factory.parse(GetResourcePropertyDocument().getGetResourcePropertyDocumentResponse().newInputStream());
	}
	
	/**
	 * Create a client for accessing the working directory
	 * 
	 * @return StorageClient
	 * @throws Exception
	 */
	public StorageClient getUspaceClient()throws Exception{
		EndpointReferenceType epr=getResourcePropertiesDocument().getJobProperties().getWorkingDirectoryReference();
		StorageClient storageClient=new StorageClient(epr, getSecurityConfiguration());
		return storageClient;
	}

	/**
	 * Start this job<br/>
	 * This will throw an exception if the job is not in the READY state
	 */
	public void start() throws Exception{
		logger.debug("Calling service at wsaTo: "+getEPR().getAddress().getStringValue());
		StartDocument startDoc=StartDocument.Factory.newInstance();
		startDoc.addNewStart();
		jobService.Start(startDoc);
	}
	
	/**
	 * Abort this job
	 */
	public void abort() throws Exception{
		logger.debug("Calling service at wsaTo: "+getEPR().getAddress().getStringValue());
		AbortDocument abortDoc=AbortDocument.Factory.newInstance();
		abortDoc.addNewAbort();
		jobService.Abort(abortDoc);
	}

	
	/**
	 * Hold this job
	 */
	public void hold() throws Exception{
		logger.debug("Calling service at wsaTo: "+getEPR().getAddress().getStringValue());
		HoldDocument holdDoc=HoldDocument.Factory.newInstance();
		holdDoc.addNewHold();
		jobService.Hold(holdDoc);
	}

	
	/**
	 * Resume this job
	 */
	public void resume() throws Exception{
		logger.debug("Calling service at wsaTo: "+getEPR().getAddress().getStringValue());
		ResumeDocument resumeDoc=ResumeDocument.Factory.newInstance();
		resumeDoc.addNewResume();
		
		jobService.Resume(resumeDoc);
	}
	
	/**
	 * Restart this job
	 * @since 1.7.0
	 */
	public void restart() throws Exception{
		if(!checkVersion("1.7.0")){
			throw new ClientException("Restart not supported (server version too old)");
		}
		logger.debug("Calling service at wsaTo: "+getEPR().getAddress().getStringValue());
		RestartDocument restartDoc=RestartDocument.Factory.newInstance();
		restartDoc.addNewRestart();
		jobService.Restart(restartDoc);
	}
	
	/**
	 * Convenience method that waits until a job has finished
	 * and returns the final status (SUCCESSFUL or FAILED)
	 * @param timeout in milliseconds (null for no timeout)
	 * @return status string
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
				logger.info("Job finished successfully.");
				break;
			}
			else if(status.equals(StatusType.FAILED)){
				logger.info("Job failed."+(statusDescription!=null?" The error was: "+statusDescription:""));
				break;
			}
			Thread.sleep(500);
		}
		return status.toString();
	}
	
	/**
	 * Convenience method that waits until a job is READY
	 * and can be started. If the job is already past the READY state,
	 * an exception is thrown.
	 * @param timeout in milliseconds (null for no timeout)
	 * @return status
	 */
	public String waitUntilReady(int timeout) throws Exception{
		StatusType.Enum status=StatusType.UNDEFINED;
		long start=System.currentTimeMillis();
		long elapsed=0;
		while(true){
			if(timeout>0 && elapsed>timeout)break;
			elapsed=System.currentTimeMillis()-start;
			status=getStatus();
			if(status.equals(StatusType.READY))break;
			if(status.equals(StatusType.FAILED)||status.equals(StatusType.SUCCESSFUL)){
				throw new Exception("Job is already done, status is <"+status.toString()+">, error description is <"+statusDescription+">");
			}
			Thread.sleep(500);
		}
		return status.toString();
	}
	
	/**
	 * return the job status
	 * @throws Exception
	 */
	public StatusType.Enum getStatus() throws Exception{
		StatusInfoType s=getResourcePropertiesDocument().getJobProperties().getStatusInfo();
		if(s!=null){
			statusDescription=s.getDescription();
			return s.getStatus();	
		}
		else{
			statusDescription="Status is not available";
			return StatusType.UNDEFINED;
		}
	}
	
	/**
	 * get the job status message
	 * @throws Exception
	 */
	public String getStatusMessage() throws Exception{
		StatusInfoType s=getResourcePropertiesDocument().getJobProperties().getStatusInfo();
		statusDescription=s.getDescription();
		return s.getDescription();
	}
	
	/**
	 * return the exit code of the job
	 * @return the exit code or <code>null</code> if not (yet) available
	 */
	public Integer getExitCode(){
		if(exitCode==null){
			try{
				StatusInfoType sit=getResourcePropertiesDocument().getJobProperties().getStatusInfo();
				if(sit.isSetExitCode()){
					BigInteger eCode=sit.getExitCode();
					if(eCode!=null)exitCode=eCode.intValue();	
				}
			}catch(Exception e){
				Log.logException("Can't get exit code.",e,logger);
			}
		}
		return exitCode;
	}
	

	/**
	 * return the progress of the job
	 * @return a value between 0 and 1 or <code>null</code> if progress value not available
	 */
	public Float getProgress(){
		try{
			if(getResourcePropertiesDocument().getJobProperties().getStatusInfo().isSetProgress()){
				return getResourcePropertiesDocument().getJobProperties().getStatusInfo().getProgress();
			}
		}catch(Exception e){
			Log.logException("Can't get progress.",e,logger);
		}
		return null;
		
	}
	
	/**
	 * get the time of submission
	 */
	public Calendar getSubmissionTime(){
		if(submissionTime==null)
		{
			try{
				submissionTime=getSingleResourceProperty(SubmissionTimeDocument.class).getSubmissionTime();
			}catch(Exception e){
				Log.logException("Can't get submission time.",e,logger);
			}
		}
		return submissionTime;
	}
	
	/**
	 * Get the job log, usually a multi-line string
	 */
	public String getJobLog()throws Exception{
		String log=getResourcePropertiesDocument().getJobProperties().getLog();
		String lineBreak=System.getProperty("line.separator", "\n");
		return log.replaceAll("\r?\n", lineBreak);
	}

	/**
	 * get the batch system queue this job is in
	 * @return batch queue or null if not available
	 * @since 1.7.0
	 */
	public String getBatchQueue() throws Exception{
		return getResourcePropertiesDocument().getJobProperties().getQueue();
	}
	
	/**
	 * get an estimated end time for this job
	 * @return estimated end time or null if not available
	 * @since 1.7.0
	 */
	public Calendar getEstimatedEndtime() throws Exception{
		return getResourcePropertiesDocument().getJobProperties().getEstimatedEndTime();
	}
}
