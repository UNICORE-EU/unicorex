package de.fzj.unicore.uas.testsuite;

import org.ggf.schemas.jsdl.x2005.x11.jsdl.ApplicationType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.DataStagingType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionDocument;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.client.JobClient;
import eu.unicore.bugsreporter.annotation.FunctionalTest;

/**
 * stages in a file from a plain http(s) location
 *
 * @author schuller
 */
public class RunCatWithHttpsStagein extends AbstractJobRun {

	String url;
	EndpointReferenceType tssepr;

	@Override
	protected void beforeStart(JobClient jms) throws Exception {
	}

	@Override
	protected void onFinish(JobClient jms) throws Exception {
		String out=getStdout(jms);
		System.out.println(out);
		if(!out.contains("Registry"))throw new Exception("Error occured.");
	}

	@FunctionalTest(id="RunCatTestHttp", 
			description="Runs a 'cat' job including a http stage-in")
	@Override
	public void testRunJob()throws Exception{
		super.testRunJob();
	}
	
	@Override
	protected JobDefinitionDocument getJob() {
		String url = kernel.getServer().getUrls()[0].toExternalForm();
		System.out.println(url);
		JobDefinitionDocument jdd=JobDefinitionDocument.Factory.newInstance();
		ApplicationType app=jdd.addNewJobDefinition().addNewJobDescription().addNewApplication();
		app.setApplicationName("Cat");
		app.setApplicationVersion("1.0");
		DataStagingType dst=jdd.getJobDefinition().getJobDescription().addNewDataStaging();
		dst.addNewSource().setURI(url+"/services");
		dst.setFileName("/infile");
		return jdd;
	}
	
}
