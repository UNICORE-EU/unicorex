package eu.unicore.uas.json;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

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
	public void testFoo() throws Exception {
		String foo = "{ Environment: { Memory: \"${1024*1024}\"\n}}";
		JSONObject j = new JSONObject(foo);
		JSONArray a = j.optJSONArray("Environment");
		assertNull(a);
		JSONObject o = j.optJSONObject("Environment");
		assertNotNull(o);
	}
	
}
