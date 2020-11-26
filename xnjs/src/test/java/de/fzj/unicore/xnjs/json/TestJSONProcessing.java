package de.fzj.unicore.xnjs.json;

import org.junit.Test;

import de.fzj.unicore.xnjs.ems.EMSTestBase;

public class TestJSONProcessing extends EMSTestBase {

	private static String[] jobs = { 
		 "src/test/resources/json/date.json",
		 "src/test/resources/json/date_with_inline_data.json",
		 "src/test/resources/json/nonzero_exit_code.json",
		 "src/test/resources/json/unsupported_staging.json",
	};	
	
	@Test
	public void testJSONJobs() throws Exception {
		for(String j: jobs) {
			System.out.println("Running job: "+j);
			String id=(String)mgr.add(xnjs.makeAction(loadJSONObject(j)),null);
			doRun(id);
			assertSuccessful(id);
		}
	}
	
	@Test
	public void testSingleJSONJob() throws Exception {
		String j = jobs[3];
		System.out.println("Running job: "+j);
		String id=(String)mgr.add(xnjs.makeAction(loadJSONObject(j)),null);
		doRun(id);
		assertSuccessful(id);
		mgr.getAction(id).printLogTrace();
	}
}
