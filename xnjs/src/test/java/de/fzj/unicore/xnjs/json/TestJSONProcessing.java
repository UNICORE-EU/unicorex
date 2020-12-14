package de.fzj.unicore.xnjs.json;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.fzj.unicore.xnjs.ems.Action;
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
	
	private static String[] pre_post_jobs = { 
					"src/test/resources/json/precommand-fail.json",
			};	

	@Test
	public void testPreCmdFail() throws Exception {
		String j = pre_post_jobs[0];
		System.out.println("Running job: "+j);
		String id=(String)mgr.add(xnjs.makeAction(loadJSONObject(j)),null);
		doRun(id);
		Action a = mgr.getAction(id);
		a.printLogTrace();
		assertTrue(a.getLog().toString().contains("Command exited with <127>"));
	}

	private static String[] sweep_jobs = { 
			"src/test/resources/json/parameter_sweep_values.json",
	        "src/test/resources/json/parameter_sweep_range.json",
			"src/test/resources/json/parameter_sweep_files.json",
	};	

	@Test
	public void testSweepJobs() throws Exception {
		for(String j: sweep_jobs) {
			System.out.println("Running job: "+j);
			String id=(String)mgr.add(xnjs.makeAction(loadJSONObject(j)),null);
			try {
				doRun(id);
				assertSuccessful(id);
			}finally {
				mgr.getAction(id).printLogTrace();
			}
		}
	}
}
