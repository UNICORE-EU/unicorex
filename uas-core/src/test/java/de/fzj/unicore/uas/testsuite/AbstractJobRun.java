package de.fzj.unicore.uas.testsuite;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.util.Calendar;

import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionDocument;
import org.junit.Ignore;
import org.junit.Test;
import org.unigrids.x2006.x04.services.tss.SubmitDocument;
import org.unigrids.x2006.x04.services.tss.SubmitResponseDocument;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.Base;
import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.client.FileTransferClient;
import de.fzj.unicore.uas.client.JobClient;
import de.fzj.unicore.uas.client.StorageClient;
import de.fzj.unicore.uas.client.TSFClient;
import de.fzj.unicore.uas.client.TSSClient;
import de.fzj.unicore.uas.impl.tss.TargetSystemFactoryHomeImpl;
import eu.unicore.services.ContainerProperties;
import eu.unicore.util.httpclient.ClientProperties;
import eu.unicore.util.httpclient.IClientConfiguration;

@Ignore
public abstract class AbstractJobRun extends Base {

	protected String url;

	protected TSSClient tss;

	protected TSFClient tsf;

	/**
	 * setup clients for the default TSF and a newly created TSS
	 * @throws Exception
	 */
	protected void initClients()throws Exception{
		url=kernel.getContainerProperties().getValue(ContainerProperties.EXTERNAL_URL);
		EndpointReferenceType tsfepr=EndpointReferenceType.Factory.newInstance();
		String tsfUrl=url+"/services/"+UAS.TSF+"?res="+TargetSystemFactoryHomeImpl.DEFAULT_TSF;
		tsfepr.addNewAddress().setStringValue(tsfUrl);
		if(tsf==null)tsf=new TSFClient(tsfUrl,tsfepr,getSecurityProperties());
		if(tss==null)tss=tsf.createTSS();
		Calendar c=tss.getCurrentTime();
		assertTrue(c!=null);
	}
	
	@Test
	public void testRunJob()throws Exception{
		initClients();
		runJob(tss);
	}

	/**
	 * return security settings for making a client call
	 */
	protected IClientConfiguration getSecurityProperties(){
		ClientProperties sp=uas.getKernel().getClientConfiguration().clone();
		sp.setMessageLogging(false);
		return sp;
	}

	protected void onError(JobClient jms){

	}

	/**
	 * runs a job until completed (can be SUCCESSFUL or FAILED)
	 */
	protected JobClient runJob(TSSClient tss) throws Exception{
		JobClient job=submitJob(tss);
		beforeStart(job);
		job.waitUntilReady(180*1000);
		job.start();
		afterStart(job);
		job.waitUntilDone(100*180*1000);
		onFinish(job);
		return job;
	}

	/**
	 * actions to be done by the client before start of the job
	 */
	protected void beforeStart(JobClient jms) throws Exception{}

	/**
	 * actions to be done by the client after starting the job (and before checking that it is done)
	 */
	protected void afterStart(JobClient jms) throws Exception{}


	/**
	 * actions to be done by the client after completion the job
	 */
	protected void onFinish(JobClient jms) throws Exception{}

	/**
	 * performs job submission
	 * @param tss
	 * @return
	 * @throws Exception
	 */
	protected JobClient submitJob(TSSClient tss)throws Exception{
		JobDefinitionDocument jdd=getJob();
		SubmitDocument req=SubmitDocument.Factory.newInstance();
		req.addNewSubmit().setJobDefinition(jdd.getJobDefinition());
		SubmitResponseDocument res=tss.Submit(req);
		return new JobClient(res.getSubmitResponse().getJobReference(),uas.getKernel().getClientConfiguration());
	}

	protected String getStdout(JobClient jms)throws Exception{
		return new String(stageOut(jms,"/stdout"));
	}

	protected String getStderr(JobClient jms)throws Exception{
		return new String(stageOut(jms,"/stderr"));
	}

	protected void stageIn(JobClient jms,String fileName, byte[] data)throws Exception{
		StorageClient uspace=jms.getUspaceClient();
		try(FileTransferClient fileClient=uspace.upload(fileName)){
			fileClient.write(data);
		}
	}

	protected byte[] stageOut(JobClient jms,String fileName)throws Exception{
		StorageClient uspace=jms.getUspaceClient();
		try(FileTransferClient fileClient=uspace.upload(fileName)){
			ByteArrayOutputStream bos=new ByteArrayOutputStream();
			fileClient.readAllData(bos);
			return bos.toByteArray();
		}
	}

	protected JobDefinitionDocument getJob() {
		JobDefinitionDocument jdd=JobDefinitionDocument.Factory.newInstance();
		jdd.addNewJobDefinition().setId("EmptyJob");
		return jdd;
	}
}
