package eu.unicore.uas.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

}
