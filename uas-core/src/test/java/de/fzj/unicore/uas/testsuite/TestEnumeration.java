package de.fzj.unicore.uas.testsuite;

import static org.junit.Assert.assertTrue;

import org.ggf.schemas.jsdl.x2005.x11.jsdl.ApplicationType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionDocument;
import org.unigrids.x2006.x04.services.tss.JobReferenceDocument;

import de.fzj.unicore.uas.client.EnumerationClient;
import de.fzj.unicore.uas.client.JobClient;
import de.fzj.unicore.uas.client.TSSClient;
import eu.unicore.bugsreporter.annotation.FunctionalTest;

public class TestEnumeration extends AbstractJobRun{

	EnumerationClient<JobReferenceDocument>eClient;

	@Override
	protected JobDefinitionDocument getJob() {
		JobDefinitionDocument jdd=JobDefinitionDocument.Factory.newInstance();
		ApplicationType app=jdd.addNewJobDefinition().addNewJobDescription().addNewApplication();
		app.setApplicationName("Date");
		app.setApplicationVersion("1.0");
		return jdd;
	}

	@FunctionalTest(id="RunEnumTest", 
					description="Tests the enumeration service")
	public void testRunJob()throws Exception{
		super.testRunJob();
	}
	
	@Override
	protected JobClient runJob(TSSClient tc)throws Exception{
		eClient=tc.getJobReferenceEnumeration();
		eClient.setUpdateInterval(-1);
		return super.runJob(tc);
	}
	
	@Override
	protected void onFinish(JobClient job) throws Exception{
		String out=getStdout(job);
		assertTrue(out!=null && out.length()>0);
		long n=eClient.getNumberOfResults();
		job.destroy();
		assertTrue(n-1==eClient.getNumberOfResults());
	}

	
}
