package eu.unicore.uas.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import eu.unicore.client.Job;

public class TestBuilder{
	
	@Test
	public void testComments() throws Exception{
		String foo = "{\n" +
				"#test\n" +
				"  # another test\n" +
				" Executable: \"/bin/date,\"}";
		Builder bob = new Builder(foo);
		assertNotNull(bob);
	}

	@Test
	public void testBuilder() throws Exception {
		JSONObject j = new JSONObject();
		j.put("ApplicationName", "Date");
		j.put("Parameters", new JSONObject());
		Builder b = new Builder(j.toString());
		b.setJobType(Job.Type.ON_LOGIN_NODE);
		assertEquals(1, b.getRequirements().size());
		assertNotNull(b.getParameters());
		assertEquals("Date", b.getProperty("ApplicationName"));
	}

	@Test
	public void testVersionMatch()throws Exception {
		ApplicationRequirement ar = new ApplicationRequirement("Date", "1.1");
		String[] available = new String[] { "0.9", "1", "2.0", "1.0", "1.2"};
		boolean[] expect = new boolean[] { false, true, true, false, true };
		JSONObject props = new JSONObject();
		for(int i=0; i<available.length; i++){
			JSONArray apps = new JSONArray(Arrays.asList("Date---v"+available[i]));
			props.put("applications", apps);
			boolean checkResult = ar.isFulfilled(props);
			assertEquals(expect[i], checkResult, "failed: "+props);
		}
	}
}
