package eu.unicore.xnjs.fts;

import java.io.File;

import org.json.JSONObject;
import org.junit.Test;

import eu.unicore.persist.util.UUID;
import eu.unicore.xnjs.ems.EMSTestBase;
import eu.unicore.xnjs.io.IFileTransferEngine;

public class TestFTSProcessing extends EMSTestBase {

	@Test
	public void testFTS1() throws Exception {
		JSONObject j = new JSONObject();
		j.put("file", "test.txt");
		j.put("source", "inline://foo");
		j.put("data", "test123");
		j.put("workdir", new File("target").getAbsolutePath());
		
		IFileTransferEngine e = xnjs.get(IFileTransferEngine.class);
		assert e!=null;
		String id=(String)mgr.add(xnjs.makeAction(j, "FTS", UUID.newUniqueID()),null);
		waitUntilDone(id);
		mgr.getAction(id).printLogTrace();
		System.out.println(mgr.getAction(id).getResult().toString());
	}

}
