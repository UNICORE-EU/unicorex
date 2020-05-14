package de.fzj.unicore.uas.testsuite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Map;

import org.unigrids.services.atomic.types.StatusType;
import org.unigrids.x2006.x04.services.tss.JobReferenceDocument;

import de.fzj.unicore.uas.TargetSystemFactory;
import de.fzj.unicore.uas.client.EnumerationClient;
import de.fzj.unicore.uas.client.JobClient;
import de.fzj.unicore.wsrflite.xmlbeans.WSUtilities;
import eu.unicore.bugsreporter.annotation.FunctionalTest;

public class TestTSSClient extends RunDate {

	@FunctionalTest(id="TSSTest", 
					description="Tests TargetSystem service")
	@Override
	public void testRunJob()throws Exception{
		initClients();
		//assert we have the EPR of the new TSS in the TSF properties
		String s=tsf.getResourceProperty(TargetSystemFactory.RPTSSReferences);
		assertTrue(s.contains(tss.getEPR().getAddress().getStringValue()));
		long start=System.currentTimeMillis();
		int n=tsf.getAccessibleTargetSystems().size();
		System.out.println("Have "+n+" accessible TSS, took: "+(System.currentTimeMillis()-start)+ " ms.");
		assertTrue(n==1);
		
		EnumerationClient<JobReferenceDocument> c=tss.getJobReferenceEnumeration();
		assertTrue(c!=null);
		c.setUpdateInterval(-1);

		int N=3;
		String[]ids=new String[N];
		for(int i=0; i<N; i++){
			JobClient job = runJob(tss);
			ids[i]=WSUtilities.extractResourceID(job.getUrl());
		}
				
		System.out.println("Enumeration : "+c.getUrl());
		System.out.println(c.getResourcePropertyDocument());
		assertTrue(c.getNumberOfResults()==N);


		// multi-job getStatus
		Map<String,StatusType.Enum>res=tss.getJobsStatus(Arrays.asList(ids));
		assertEquals(3,res.size());
		
		System.out.println("Job stati: "+res);

		// multi-job delete
		tss.deleteJobs(Arrays.asList(ids));
		
		assertTrue(c.getNumberOfResults()==0);

		System.out.println("Remaining budget: "+tss.getComputeTimeBudget());
		tss.destroy();
		Thread.sleep(5000);
	}

	@Override
	protected void onFinish(JobClient job) throws Exception{
		//do not cleanup the job
	}
	
}
