package de.fzj.unicore.uas.testsuite;

import org.ggf.schemas.jsdl.x2005.x11.jsdl.ApplicationType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionDocument;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.client.JobClient;
import de.fzj.unicore.uas.client.StorageClient;
import eu.unicore.bugsreporter.annotation.FunctionalTest;

public class RunCat extends AbstractJobRun {

	String url;
	EndpointReferenceType tssepr;
	String testString="this is an import test";

	@Override
	protected void beforeStart(JobClient jms) throws Exception {
		StorageClient c=jms.getUspaceClient();
		System.out.println(c.getResourcePropertyDocument());
		c.upload("/infile").write(testString.getBytes());
	}

	@Override
	protected void onFinish(JobClient jms) throws Exception {
		String out=getStdout(jms);
		if(!out.contains(testString))throw new Exception("Error occured.");
	}
	
	@FunctionalTest(id="RunCatTest", 
			description="Runs a 'cat' job including a stage-in")
	@Override
	protected JobDefinitionDocument getJob() {
		JobDefinitionDocument jdd=JobDefinitionDocument.Factory.newInstance();
		ApplicationType app=jdd.addNewJobDefinition().addNewJobDescription().addNewApplication();
		app.setApplicationName("Cat");
		app.setApplicationVersion("1.0");
		return jdd;
	}
	
}
