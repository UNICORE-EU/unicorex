package eu.unicore.xnjs.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import eu.unicore.xnjs.ems.Action;
import eu.unicore.xnjs.ems.EMSTestBase;
import eu.unicore.xnjs.io.IOProperties;

public class TestJSONProcessing extends EMSTestBase {

	private static String[] jobs = { 
			"src/test/resources/json/date.json",
			"src/test/resources/json/date_with_inline_data.json",
			"src/test/resources/json/nonzero_exit_code.json",
			"src/test/resources/json/unsupported_staging.json",
			"src/test/resources/json/date_with_stagein.json",
	};	

	@Test
	public void testJSONJobs() throws Exception {
		for(String j: jobs) {
			System.out.println("Running job: "+j);
			Action a = xnjs.makeAction(loadJSONObject(j));
			String id = a.getUUID();
			mgr.add(a,createClient());
			doRun(id);
			assertSuccessful(id);
		}
	}

	@Test
	public void testSingleJSONJob() throws Exception {
		String j = jobs[1];
		System.out.println("Running job: "+j);
		Action a = xnjs.makeAction(loadJSONObject(j));
		String id = a.getUUID();
		mgr.add(a,createClient());
		a = mgr.getAction(id);
		System.out.println(new JSONObject((String)a.getAjd()).toString(2));
		doRun(id);
		assertSuccessful(id);
		a = mgr.getAction(id);
		assertEquals(3, a.getStageIns().size());
		System.out.println(new JSONObject((String)a.getAjd()).toString(2));
	}

	@Test
	public void testSingleJSONJob2() throws Exception {
		String j = "src/test/resources/json/date_with_stagein.json";
		System.out.println("Running job: "+j);
		Action a = xnjs.makeAction(loadJSONObject(j));
		String id = a.getUUID();
		mgr.add(a,createClient());
		a = mgr.getAction(id);
		System.out.println(new JSONObject((String)a.getAjd()).toString(2));
		doRun(id);
		assertSuccessful(id);
		a = mgr.getAction(id);
		assertEquals(2, a.getStageIns().size());
		System.out.println(new JSONObject((String)a.getAjd()).toString(2));
	}

	@Test
	public void testSingleJSONJob_withExtras() throws Exception {
		JSONObject j = loadJSONObject("src/test/resources/json/date_with_stagein.json");
		j.put("User precommand", "sleep 1");
		j.put("RunUserPrecommandOnLoginNode", "false");
		j.getJSONObject("Resources").put("Reservation", "test123");
		xnjs.getIOProperties().setProperty(IOProperties.STAGING_FS_WAIT, "true");
		Action a = xnjs.makeAction(j);
		String id = a.getUUID();
		mgr.add(a,createClient());
		a = mgr.getAction(id);
		System.out.println(new JSONObject((String)a.getAjd()).toString(2));
		doRun(id);
		assertSuccessful(id);
		a = mgr.getAction(id);
		System.out.println(new JSONObject((String)a.getAjd()).toString(2));
		assertEquals(2, a.getStageIns().size());
		xnjs.getIOProperties().setProperty(IOProperties.STAGING_FS_WAIT, "false");
		a.printLogTrace();
	}

	private static String[] pre_post_jobs = { 
					"src/test/resources/json/precommand-fail.json",
					"src/test/resources/json/prepost.json",
			};	

	@Test
	public void testPreCmdFail() throws Exception {
		String j = pre_post_jobs[0];
		System.out.println("Running job: "+j);
		Action a = xnjs.makeAction(loadJSONObject(j));
		String id = a.getUUID();
		mgr.add(a,createClient());
		doRun(id);
		a = mgr.getAction(id);
		assertTrue(a.getLog().toString().contains("Command exited with non-zero exit code"));
	}

	@Test
	public void testPrePost() throws Exception {
		String j = pre_post_jobs[1];
		System.out.println("Running job: "+j);
		Action a = xnjs.makeAction(loadJSONObject(j));
		String id = a.getUUID();
		mgr.add(a,createClient());
		doRun(id);
		a = mgr.getAction(id);
		assertTrue(a.getLog().toString().contains("Total:"));
	}

	private static String[] sweep_jobs = { 
		//	"src/test/resources/json/parameter_sweep_values.json",
	    //    "src/test/resources/json/parameter_sweep_range.json",
			"src/test/resources/json/parameter_sweep_files.json",
	};	

	@Test
	public void testSweepJobs() throws Exception {
		for(String j: sweep_jobs) {
			System.out.println("Running job: "+j);
			JSONObject job = loadJSONObject(j);
			System.out.println(job.toString(2));
			Action a = xnjs.makeAction(loadJSONObject(j));
			String id = a.getUUID();
			mgr.add(a,createClient());
			try {
				doRun(id);
				System.out.println("Check: "+j);
				assertSuccessful(id);
			}finally {
				mgr.getAction(id).printLogTrace();
			}
		}
	}
}
