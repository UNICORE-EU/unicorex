package eu.unicore.xnjs.persistence;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import eu.unicore.persist.impl.PersistImpl;
import eu.unicore.persist.util.Export;
import eu.unicore.xnjs.ConfigurationSource;
import eu.unicore.xnjs.ems.Action;
import eu.unicore.xnjs.ems.BasicManager;
import eu.unicore.xnjs.ems.EMSTestBase;
import eu.unicore.xnjs.ems.ExecutionException;

public class TestPersistActionStore extends EMSTestBase {

	@BeforeEach
	public void setUp3()throws Exception{
		System.setProperty(IActionStore.CLEAR_ON_STARTUP,"true");
	}

	//jsdl document paths
	private static String smallDoc="src/test/resources/json/date.json";
	private static String faultyJob="src/test/resources/json/staging_failure.json";
	
	@Override
	protected void addProperties(ConfigurationSource cs){
		super.addProperties(cs);
		cs.getProperties().put("XNJS.autosubmit","true");
	}
	
	@Test
	public void test1()throws Exception{
		doTest(1);
	}

	@Test
	public void testN()throws Exception{
		doTest(5);
	}
	
	@Test
	public void testFailing()throws Exception{
		doTest(5, true, false, faultyJob, 2);
	}

	@Disabled
	public void testExport()throws Exception{
		JDBCActionStore as = (JDBCActionStore)xnjs.getActionStore("JOBS");
		as.doCleanup();
		doTest(2,false,false,smallDoc,1);
		Export exporter=new Export((PersistImpl<?>)as.getDoneJobsStorage());
		exporter.doExport();
	}
	
	private void doTest(int n)throws Exception{
		doTest(n, false, false, smallDoc, 1);
	}
	
	private List<String> doTest(int n, boolean checkSuccess, boolean expectSuccess, String doc, int actionsPerJob)throws Exception{
		((JDBCActionStore)xnjs.getActionStore("JOBS")).doCleanup();
		
		long start,end;
		
		ArrayList<String> ids=new ArrayList<String>();
		JSONObject job = loadJSONObject(doc);
		start=System.currentTimeMillis();
		System.out.println("adding "+n+" jobs.");
		try {
			String id="";
			for(int i=0;i<n;i++){
				id=(String)mgr.add(
						xnjs.makeAction(job),null);
				ids.add(id);
			}
		} catch (ExecutionException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		end=System.currentTimeMillis();
		float time=(0.0f+(end-start)/1000);
		System.out.println("Took: "+(time)+" secs.");
		int c=0;
		
		System.out.println("Waiting for jobs to finish.");
		while(((BasicManager)mgr).getDoneJobs()<actionsPerJob*n && c<n){
			Thread.sleep(1000);
			c++;
		}
		System.out.println(((JDBCActionStore)xnjs.getActionStore("JOBS")).printDiagnostics());
		
		end=System.currentTimeMillis();
		time=(0.0f+(end-start)/1000);
		
		System.out.println("Took: "+(time)+" secs.");
		System.out.println("Rate: "+(n/time+0.0f)+" per sec.");
		
		int success = 0;
		if(checkSuccess){
			for(String id : ids){
				Action a = ((BasicManager)mgr).getAction(id);
				if(expectSuccess){
					if(a.getResult().isSuccessful())success++;
				}
				else{
					if(!a.getResult().isSuccessful())success++;
					else{
						System.out.println("Result for "+id+": "+a.getResult());
					}
				}
			}
			System.out.println("Check expected result: "+success+" Jobs are OK.");
			if(n!=success)fail("Some jobs are not OK: "+(n-success)+" jobs not have the expected results.");
		}
		
		return ids;
	}
}
