package de.fzj.unicore.uas.testsuite;

import static junit.framework.Assert.assertTrue;

import org.ggf.schemas.jsdl.x2005.x11.jsdl.CreationFlagEnumeration;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.DataStagingType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.SourceTargetType;
import org.unigrids.services.atomic.types.StatusType;

import de.fzj.unicore.uas.client.JobClient;
import de.fzj.unicore.uas.client.StorageClient;
import de.fzj.unicore.uas.client.TSSClient;

/**
 * submit a job containing faulty stage out
 * @author schuller
 */
public class TestStageInStageOutError extends AbstractJobRun {

	String testString="this is a staging test";

	StorageClient helperUSpace;

	String uspaceUrl;

	@Override
	protected JobClient submitJob(TSSClient tss)throws Exception{
		return super.submitJob(tss);
	}

	@Override
	protected void beforeStart(JobClient jms) throws Exception {
		jms.getUspaceClient().upload("test.txt").write(testString.getBytes());
	}

	@Override
	protected void onFinish(JobClient jms) throws Exception {
		jms.setUpdateInterval(-1);
		System.out.println("Job is "+jms.getStatus());
		Thread.sleep(1000);
		System.out.println(jms.getResourcePropertyDocument());
		assertTrue(StatusType.FAILED==jms.getStatus());
	}

	@Override
	protected JobDefinitionDocument getJob() {
		JobDefinitionDocument jdd=JobDefinitionDocument.Factory.newInstance();
		jdd.addNewJobDefinition().addNewJobDescription();

		//add stage-out
		StringBuilder sb=new StringBuilder();
		DataStagingType s=jdd.getJobDefinition().getJobDescription().addNewDataStaging();
		s.setFileName("/test.txt");
		SourceTargetType stt=SourceTargetType.Factory.newInstance();
		sb.append("xxx://wrongURL");
		stt.setURI(sb.toString());
		s.setTarget(stt);
		s.setCreationFlag(CreationFlagEnumeration.OVERWRITE);

		return jdd;
	}

}
