package eu.unicore.uas.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.HashMap;
import java.util.Map;

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
	public void testCommentParse()throws JSONException{
		JSONObject o=JSONUtil.read(
				"{\n" +
		        "#\n" +
				"#this is a comment \n" +
				"#\n" +
			    "   # also a comment \n" +
				"foo : \"bar#nocomment\" }");
		assertNotNull(o);
		assertEquals("bar#nocomment", o.getString("foo"));
		
		o=JSONUtil.read(
					"{\n" +
		 "From: \"url/#xx\"," + 
		"\n}");
		assertNotNull(o);
		assertEquals("url/#xx", o.getString("From"));
	}
	
	
	@Test
	public void testErrorReporting(){
		String json="{ foo : bar \n \n ";
		try{
			new JSONObject(json);
			fail("Expected parse exception");
		}catch(JSONException ex){
			String msg=JSONUtil.makeParseErrorMessage(json, ex);
			assertTrue(msg.contains("line 3"));
		}
	}
	
	@Test
	public void testErrorReporting2(){
		String json="#foocomment\n{ foo : bar \n \n }";
		try{
			new JSONObject(json);
			fail("Expected parse exception");
		}catch(JSONException ex){
			String msg=JSONUtil.makeParseErrorMessage(json, ex);
			assertTrue(msg.contains("must begin with"));
		}
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
}
