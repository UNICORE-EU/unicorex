package de.fzj.unicore.uas.testsuite;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;

import org.ggf.schemas.jsdl.x2005.x11.jsdl.ApplicationType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.DataStagingType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.SourceTargetType;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.client.JobClient;
import de.fzj.unicore.uas.client.StorageClient;
import de.fzj.unicore.xnjs.jsdl.JSDLUtils;
import eu.unicore.bugsreporter.annotation.FunctionalTest;

public class RunCatWithStageInFromRemoteSMS extends AbstractJobRun {

	String url;
	EndpointReferenceType tssepr;

	String testString="this is an import test";

	private File localFile;

	@FunctionalTest(id="RunCatRemoteData", 
			description="Runs a 'cat' job including a stage-in from a remote SMS")
	@Override
	public void testRunJob()throws Exception{
		super.testRunJob();
	}

	@Override
	protected void beforeStart(JobClient jms) throws Exception {
		System.out.println("Job submitted at: "+jms.getSubmissionTime());
		
		localFile=File.createTempFile("uas","uastest");
		localFile.deleteOnExit();
		checkDefaultSMSAvailable();
		FileWriter fw=new FileWriter(localFile);
		fw.write(testString);
		fw.close();
		
		StorageClient uspace=jms.getUspaceClient();
		uspace.upload("test.txt").write("import test".getBytes());
		uspace.upload("folder/test.txt").write("import test in subfolder".getBytes());
		jms.waitUntilReady(180*1000);
	}

	protected void checkDefaultSMSAvailable()throws Exception{
		EndpointReferenceType epr=EndpointReferenceType.Factory.newInstance();
		epr.addNewAddress().setStringValue("http://localhost:65321/services/StorageManagement?res=WORK");
		StorageClient sms=new StorageClient(epr,kernel.getClientConfiguration());
		assertTrue("WORK".equals(sms.getStorageName()));
	}

	@Override
	protected void onFinish(JobClient jms) throws Exception {
		String err=getStderr(jms);
		if(err.length()>0)throw new Exception("Error occured: "+err);
		String out=getStdout(jms);
		System.out.println("stdout: " +out);
		if(out.length()==0)throw new Exception("Error occured, stdout is empty");
	}

	@Override
	protected JobDefinitionDocument getJob() {
		JobDefinitionDocument jdd=JobDefinitionDocument.Factory.newInstance();
		ApplicationType app=jdd.addNewJobDefinition().addNewJobDescription().addNewApplication();
		app.setApplicationName("Cat");
		app.setApplicationVersion("1.0");
		DataStagingType s=jdd.getJobDefinition().getJobDescription().addNewDataStaging();
		s.setFileName("/infile");
		SourceTargetType stt=SourceTargetType.Factory.newInstance();
		stt.setURI("BFT:http://localhost:65321/services/StorageManagement?res=WORK#test.txt");
		s.setSource(stt);
		assertTrue(JSDLUtils.hasStageIn(jdd));
		return jdd;
	}

}
