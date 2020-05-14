package de.fzj.unicore.uas.testsuite;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.ApplicationType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.DataStagingType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.SourceTargetType;

import de.fzj.unicore.uas.client.JobClient;
import de.fzj.unicore.uas.xnjs.XNJSFacade;
import de.fzj.unicore.xnjs.jsdl.JSDLUtils;
import eu.unicore.bugsreporter.annotation.FunctionalTest;

public class RunStageInWithMissingDir extends AbstractJobRun {

	String testString="this is an import test";
	
	private File localFile;
	
	@FunctionalTest(id="RunStageinTest", 
			description="Tests data stage-in")
	@Override
	public void testRunJob()throws Exception{
		super.testRunJob();
	}

	@Override
	protected void beforeStart(JobClient jms) throws Exception {
		localFile=File.createTempFile("uas","uastest");
		localFile.deleteOnExit();
		FileWriter fw=new FileWriter(localFile);
		fw.write(testString);
		fw.close();
		jms.waitUntilReady(180*1000);
	}

	@Override
	protected void onError(JobClient job){
		try{
			String uid=job.getEPR().getAddress().getStringValue().split("=")[1];
			System.out.println(uid);
			XNJSFacade.get(null,kernel).getAction(uid).printLogTrace();
		}catch(Exception e){
			
		}
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
		try{
			FileUtils.write(new File("target/unicorex-test/test.txt"), "test data", "UTF-8");
		}catch(IOException e){throw new RuntimeException(e);}
		
		JobDefinitionDocument jdd=JobDefinitionDocument.Factory.newInstance();
		ApplicationType app=jdd.addNewJobDefinition().addNewJobDescription().addNewApplication();
		app.setApplicationName("Date");
		DataStagingType s=jdd.getJobDefinition().getJobDescription().addNewDataStaging();
		s.setFileName("/newdir/infile");
		SourceTargetType stt=SourceTargetType.Factory.newInstance();
		stt.setURI("BFT:http://localhost:65321/services/StorageManagement?res=WORK#test.txt");
		s.setSource(stt);
		assertTrue(JSDLUtils.hasStageIn(jdd));
		return jdd;
	}
	
}
