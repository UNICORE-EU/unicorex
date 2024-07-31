package eu.unicore.uas.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
	public void testConvertRESTURLs(){
		String[] rest = {
				"https://somehttp/files/a/b/c.txt",
				"https://foo:8080/TEST/rest/core/storages/default_storage/files/a/b/c.txt",
				"https://foo:8080/TEST/rest/core/storages/default_storage/files/foo/files/c.txt",
				"http://nosecurity/rest/core/storages/default_storage/files/a/b/c.txt",
				};
		String[] wsrf = {
				"https://somehttp/files/a/b/c.txt",
				"BFT:https://foo:8080/TEST/services/StorageManagement?res=default_storage#/a/b/c.txt",
				"BFT:https://foo:8080/TEST/services/StorageManagement?res=default_storage#/foo/files/c.txt",
				"BFT:http://nosecurity/services/StorageManagement?res=default_storage#/a/b/c.txt",
				};
		for(int i=0; i<rest.length; i++){
			assertEquals(wsrf[i], Builder.convertRESTToWSRF(rest[i]));
		}
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
