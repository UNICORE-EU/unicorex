package de.fzj.unicore.xnjs.fts;

import org.json.JSONObject;
import org.junit.Test;

import de.fzj.unicore.xnjs.ems.EMSTestBase;

public class TestFTSProcessing extends EMSTestBase {

	@Test
	public void testFTS1() throws Exception {
		JSONObject j = new JSONObject();
		j.put("file", "test.txt");
		j.put("source", "inline://foo");
		j.put("Data", "test123");
		
		String id=(String)mgr.add(xnjs.makeAction(j, "FTS",  java.util.UUID.randomUUID().toString()),null);
		waitUntilDone(id);
		assertSuccessful(id);
	}

}
