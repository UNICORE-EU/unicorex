package de.fzj.unicore.xnjs.fts;

import java.io.File;

import org.json.JSONObject;
import org.junit.Test;

import de.fzj.unicore.xnjs.ems.EMSTestBase;
import de.fzj.unicore.xnjs.io.IFileTransferEngine;

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
		System.out.println(e.getClass().getName());
		String id=(String)mgr.add(xnjs.makeAction(j, "FTS",  java.util.UUID.randomUUID().toString()),null);
		waitUntilDone(id);
		mgr.getAction(id).printLogTrace();
		System.out.println(mgr.getAction(id).getResult().toString());
	}

}
