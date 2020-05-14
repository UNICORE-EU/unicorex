package de.fzj.unicore.uas.testsuite;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.ggf.schemas.jsdl.x2005.x11.jsdl.ApplicationType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionDocument;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.client.JobClient;
import de.fzj.unicore.uas.client.TSSClient;
import eu.unicore.bugsreporter.annotation.FunctionalTest;

public class RunDate extends AbstractJobRun{

	EndpointReferenceType tssepr;
	
	boolean doDestroy = true;
	
	@Override
	@FunctionalTest(id="RunDateTest", description="Runs a 'date' job.")
	public void testRunJob()throws Exception{
		super.testRunJob();
		runJobsMultithreaded();
	}

	@Override
	protected JobDefinitionDocument getJob() {
		JobDefinitionDocument jdd=JobDefinitionDocument.Factory.newInstance();
		ApplicationType app=jdd.addNewJobDefinition().addNewJobDescription().addNewApplication();
		app.setApplicationName("Date");
		app.setApplicationVersion("1.0");
		return jdd;
	}

	@FunctionalTest(id="RunDateTestMultithreaded", 
	                description="Runs multiple 'date' jobs using several client threads.")
	public void runJobsMultithreaded() throws Exception {
		doDestroy = false;
		initClients();
		int N = 3;
		final int jobs_per_thread = 2;
		
		List<Thread>threads = new ArrayList<Thread>();
		for(int i=0;i<N;i++){
			Thread t = new Thread(){
				public void run(){
					try{
						TSSClient ts = new TSSClient(tss.getEPR(),tss.getSecurityConfiguration());
						for(int i=0;i<jobs_per_thread;i++){
							runJob(ts);
						}
					}catch(Exception ex){
						ex.printStackTrace();
					}
				}
			};
			threads.add(t);
			t.start();
		}
		for(int i=0;i<N;i++){
			threads.get(i).join();
		}
		
		long num = tss.getJobReferenceEnumeration().getNumberOfResults();
		System.out.println("Total jobs submitted: "+num);
		assertEquals(N*jobs_per_thread,num);
	}
	
	@Override
	protected void onFinish(JobClient job) throws Exception{
		String out=getStdout(job);
		assertTrue(out!=null && out.length()>0);
		if(doDestroy())job.destroy();
	}

	protected boolean doDestroy(){
		return doDestroy;
	}

}
