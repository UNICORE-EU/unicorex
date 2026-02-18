package eu.unicore.uas.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

public class TestJSONUtil {

	@Test
	public void testSimpleParse()throws JSONException{
		JSONObject o=new JSONObject("{ foo : bar }");
		assertNotNull(o);
		assertEquals("bar", o.getString("foo"));
	}
	
	@Test
	public void testErrorReporting(){
		String json = "{ foo : bar \n \n ";
		JSONException ex = assertThrows(JSONException.class, ()->new JSONObject(json));
		String msg = ex.getMessage();
		System.out.println(msg);
		assertTrue(msg.contains("line 3"));
	}
	
	@Test
	public void testToMap()throws Exception{
		String json="{ foo : bar, x : y } ";
		JSONObject o=new JSONObject(json);
		Map<String,String>map=JSONUtil.asMap(o);
		assertEquals("y",map.get("x"));
		assertEquals("bar",map.get("foo"));
	}
	
	@Test
	public void testToJSON()throws Exception{
		Map<String,String>map=new HashMap<String, String>();
		map.put("x", "y");
		map.put("foo", "bar");
		JSONObject o=JSONUtil.asJSON(map);
		assertEquals("y",o.getString("x"));
		assertEquals("bar",o.getString("foo"));
	}
	
	@Test
	public void testMultiLine() throws Exception {
		JSONObject j = new JSONObject();
		JSONArray a = new JSONArray();
		a.put("1");
		a.put("2");
		j.put("data", a);
		String l = JSONUtil.readMultiLine("data", null, j);
		assertEquals("1\n2\n", l);
		j.put("data", "1\n2\n");
		assertEquals("1\n2\n", JSONUtil.readMultiLine("data", null, j));
		j.remove("data");
		assertEquals("def", JSONUtil.readMultiLine("data", "def", j));
		
	}
}
