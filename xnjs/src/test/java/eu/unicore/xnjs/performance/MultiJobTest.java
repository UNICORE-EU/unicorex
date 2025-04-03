package eu.unicore.xnjs.performance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import eu.unicore.xnjs.ConfigurationSource;
import eu.unicore.xnjs.ems.ActionStatus;
import eu.unicore.xnjs.ems.BasicManager;
import eu.unicore.xnjs.ems.EMSTestBase;
import eu.unicore.xnjs.persistence.IActionStore;
import eu.unicore.xnjs.tsi.local.LocalExecution;

public class MultiJobTest extends EMSTestBase {

	private static String json = "src/test/resources/json/date.json";
	
	@Override
	protected void addProperties(ConfigurationSource cs){
		super.addProperties(cs);
		Properties props = cs.getProperties();
		System.setProperty(IActionStore.CLEAR_ON_STARTUP,"true");
		props.put("XNJS.autosubmit", "true");
		props.put("XNJS.numberofworkers", "4");
	}

	protected int getNumberOfTasks(){
		return 512;
	}

	@Test
	public void testMultipleJobs()throws Exception{
		((BasicManager)mgr).getActionStore().removeAll();
		LocalExecution.reset();

		int n = getNumberOfTasks(); //how many jobs
		assertTrue(xnjs.getXNJSProperties().isAutoSubmitWhenReady());

		long start,end;
		List<String> ids=new ArrayList<>();
		start=System.currentTimeMillis();
		JSONObject job = loadJSONObject(json);
		String id;
		for(int i=0;i<n;i++){
			if(i%50==0 && i>0)System.out.println("Submitted "+i+" jobs.");
				id=(String)mgr.add(xnjs.makeAction(job),null);
			ids.add(id);
		}

		end=System.currentTimeMillis();
		float time=(0.0f+(end-start)/1000);
		System.out.println("All "+n+" jobs submitted in "+time+" secs.");
		System.out.println("Submission rate: "+(n/time+0.0f)+" per sec.");
		System.out.println("Using "+xnjs.getXNJSProperties().getWorkerCount()+" worker threads.");
		int p=0;
		int q=0;
		int c=0;
		int p_new=0;
		int timeout=60;
		try{
			do{
				p_new=((BasicManager)mgr).getDoneJobs();
				if(p==p_new){
					c++; 
				}else c=0;

				if(c>timeout){
					throw new InterruptedException("Timeout: job processing did not make progress for "
							+timeout+" seconds.");
				}

				p=p_new;
				q=((BasicManager)mgr).getAllJobs();
				int queued=((BasicManager)mgr).getActionStore().size(ActionStatus.QUEUED);
				int pending=((BasicManager)mgr).getActionStore().size(ActionStatus.PENDING);
				if(p>0)System.out.println("Processed "+p+" jobs (of "+q+", pending="+
						pending+", queued="+queued+") in "
						+(System.currentTimeMillis()-start)+ "ms.");
				Thread.sleep(1000);
			}while(p<n);
		}catch(InterruptedException e){
			e.printStackTrace();
			System.out.println("ERROR processing jobs.");
			System.out.println((((BasicManager)mgr).getActionStore()).printDiagnostics());
			Set<String> jobs=LocalExecution.getRunningJobs();
			System.out.println("Jobs in execution table: "+jobs.size());
			for(String s: jobs)System.out.println(s);
		}
		end=System.currentTimeMillis();
		time=(0.0f+(end-start)/1000);
		if(xnjs.getTargetSystemInterface(null, null).isLocal()){
			System.out.println("Tasks executed: "+LocalExecution.getCompletedTasks());
			System.out.println("Rejected tasks: "+LocalExecution.getNumberOfRejectedTasks());
		}

		System.out.println("Took: "+(time)+" secs.");
		float rate = n/time+0.0f;
		System.out.println("Rate: "+rate+" per sec.");

		assertEquals(n, LocalExecution.getCompletedTasks());
	}
}
