package de.fzj.unicore.uas.testsuite;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;

import org.ggf.schemas.jsdl.x2005.x11.jsdl.ApplicationType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.CreationFlagEnumeration;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.DataStagingType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.SourceTargetType;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.client.JobClient;
import de.fzj.unicore.xnjs.jsdl.JSDLUtils;
import eu.unicore.bugsreporter.annotation.FunctionalTest;

public class RunCatWithStageInByCopy extends AbstractJobRun {

	String url;
	EndpointReferenceType tssepr;

	String testString="this is an import test";
	
	private File localFile;
	
	@FunctionalTest(id="RunCatTestCP", 
			description="Runs a 'cat' job including a stage-in using local copy")
	@Override
	public void testRunJob()throws Exception{
		preSubmit();
		super.testRunJob();
	}

	protected void preSubmit() throws Exception {
		localFile=File.createTempFile("uas","uastest");
		localFile.deleteOnExit();
		FileWriter fw=new FileWriter(localFile);
		fw.write(testString);
		fw.close();
	}

	@Override
	protected void onFinish(JobClient jms) throws Exception {
		String err=getStderr(jms);
		if(err.length()>0)throw new Exception("Error occured: "+err);
		String out=getStdout(jms);
		if(out.length()==0)throw new Exception("Error occured, stdout is empty");
		assertTrue(testString.equals(out));
	}
	
	@Override
	protected JobDefinitionDocument getJob() {
		JobDefinitionDocument jdd=JobDefinitionDocument.Factory.newInstance();
		ApplicationType app=jdd.addNewJobDefinition().addNewJobDescription().addNewApplication();
		app.setApplicationName("Cat");
		app.setApplicationVersion("1.0");
		DataStagingType s=jdd.getJobDefinition().getJobDescription().addNewDataStaging();
		s.setCreationFlag(CreationFlagEnumeration.OVERWRITE);
		s.setFileName("/infile");
		SourceTargetType stt=SourceTargetType.Factory.newInstance();
		stt.setURI("file://"+localFile.getAbsolutePath());
		s.setSource(stt);
		assertTrue(JSDLUtils.hasStageIn(jdd));
		return jdd;
	}
	
}
