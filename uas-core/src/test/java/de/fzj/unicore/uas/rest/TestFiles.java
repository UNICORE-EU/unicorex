package de.fzj.unicore.uas.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.junit.Test;

import de.fzj.unicore.uas.Base;
import eu.unicore.services.rest.client.BaseClient;

public class TestFiles extends Base {

	@Test
	public void testModifyFile() throws Exception {
		String storage = createStorage();
		BaseClient client = new BaseClient(storage,kernel.getClientConfiguration());
		String file = storage + "/files/test.txt";
		putContent("test data", client, file);
		client.setURL(file);
		HttpResponse res = client.get(ContentType.APPLICATION_JSON);
		System.out.println(Arrays.asList(res.getAllHeaders()));
		assertTrue(res.containsHeader("Accept-Ranges"));
		assertEquals("bytes", res.getFirstHeader("Accept-Ranges").getValue());
		EntityUtils.consume(res.getEntity());
		
		String props = client.getJSON().toString(2);
		System.out.println(props);
		JSONObject mod = new JSONObject();
		mod.put("unixPermissions", "rw-------");
		mod.put("group", "somegroup");
		JSONObject md = new JSONObject();
		md.put("foo", "bar");
		mod.put("metadata", md);
		client.putQuietly(mod);
		JSONObject prop2 = client.getJSON();
		System.out.println(prop2.toString(2));
		// check metadata was set
		assertEquals("bar",prop2.getJSONObject("metadata").getString("foo"));
		
		// update md
		md.put("foo", "spam");
		md.put("Content-Type", "text/plain");

		client.put(mod);
		prop2 = client.getJSON();
		// check metadata was updated
		assertEquals("spam",prop2.getJSONObject("metadata").getString("foo"));
		assertEquals("text/plain",prop2.getJSONObject("metadata").getString("Content-Type"));
		
		// check that GET returns the proper Content-Type
		res = getFile(client, file);
		System.out.println(Arrays.asList(res.getAllHeaders()));
		assertTrue(res.containsHeader("Content-Type"));
		assertEquals("text/plain", res.getFirstHeader("Content-Type").getValue());
		EntityUtils.consume(res.getEntity());

		// trigger auto-extract
		JSONObject settings = new JSONObject();
		client.setURL(file+"/actions/extract");
		client.postQuietly(settings);
	}
	
	@Test(expected = Exception.class)
	public void testDeleteFile() throws Exception {
		String storage = createStorage();
		String file = storage + "/deleteme.txt";
		BaseClient client = new BaseClient(file, kernel.getClientConfiguration());
		putContent("test data", client, file);
		client.delete();
		client.getJSON();
	}
	
	
	@Test
	public void testCreateDirectory() throws Exception {
		String storage = createStorage();
		String file = storage + "/files/new_dir";
		BaseClient client = new BaseClient(file, kernel.getClientConfiguration());
		client.postQuietly(null);
		System.out.println(client.getJSON());
	}

	@Test
	public void testRename() throws Exception {
		String storage = createStorage();
		BaseClient client = new BaseClient(storage,kernel.getClientConfiguration());
		String file = storage + "/files/test.txt";
		putContent("test data", client, file);
		String renameURL = storage + "/actions/rename";
		JSONObject rename = new JSONObject();
		rename.put("from","test.txt");
		rename.put("to","test_new.txt");
		client.setURL(renameURL);
		client.post(rename);
		String renamed = storage + "/files/test_new.txt";
		client.setURL(renamed);
		System.out.println(client.getJSON());
	}
	
	@Test
	public void testCopy() throws Exception {
		String storage = createStorage();
		String file = storage + "/files/test.txt";
		BaseClient client = new BaseClient(file, kernel.getClientConfiguration());
		putContent("test data", client, file);
		
		String copyURL = storage + "/actions/rename";
		JSONObject copy = new JSONObject();
		copy.put("from","test.txt");
		copy.put("to","test_copy.txt");
		client.setURL(copyURL);
		client.post(copy);
		
		String renamed = storage + "/files/test_copy.txt";
		client.setURL(renamed);
		System.out.println(client.getJSON());
	}
	
	
	@Test
	public void testGetFile() throws Exception {
		String storage = createStorage();
		BaseClient client = new BaseClient(storage,kernel.getClientConfiguration());
		System.out.println("Accessing "+storage);
		String file = storage + "/files/test.txt";
		putContent("test data", client, file);
		String content = getContent(client, file);
		assertEquals("test data", content);
		
		String partial = getPartialContent(client, file, 5, 4);
		assertEquals("data", partial);
		partial = getPartialContent(client, file, 0, 4);
		assertEquals("test", partial);
		
		partial = getPartialContent(client, file, 1, -1);
		assertEquals("est data", partial);
		
	}

	@Test
	public void testPutFile() throws Exception {
		String storage = createStorage();
		System.out.println("Accessing "+storage);
		String file = storage + "/files/test.txt";
		BaseClient client = new BaseClient(file,kernel.getClientConfiguration());
		byte[] testData ="test data".getBytes();
		InputStream is = new ByteArrayInputStream(testData);
		putContent(is, client, file);

		// check properties
		JSONObject properties = client.getJSON();
		System.out.println("*** File properties:");
		System.out.println(properties.toString(2));
		int len = properties.getInt("size");
		assertEquals("Wrong length: "+len,testData.length, len);

		client.delete();
		assertEquals("Got: "+client.getLastStatus(),HttpStatus.SC_NO_CONTENT, client.getLastHttpStatus());

		// upload with size param set
		file = storage + "/files/test_2.txt?size="+testData.length;
		is = new ByteArrayInputStream(testData);
		putContent(is, client, file);
		// check properties
		properties = client.getJSON();
		len = properties.getInt("size");
		assertEquals("Wrong length: "+len,testData.length, len);

		client.delete();
		assertEquals("Got: "+client.getLastStatus(),HttpStatus.SC_NO_CONTENT, client.getLastHttpStatus());
	}

	/**
	 * creates a new empty storage and returns its URL
	 */
	private String createStorage() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest";
		String resource  = url+"/core/storages";
		BaseClient client = new BaseClient(resource,kernel.getClientConfiguration());
		System.out.println("Accessing "+resource);
		JSONObject task = new JSONObject();
		HttpResponse response = client.post(task);
		int status = client.getLastHttpStatus();
		assertEquals("Got: "+client.getLastStatus(),201, status);
		String storage = response.getFirstHeader("Location").getValue();
		System.out.println("created: "+storage);
		EntityUtils.consumeQuietly(response.getEntity());
		return storage;
	}

	private String getContent(BaseClient client, String file) throws Exception {
		client.setURL(file);
		HttpResponse response = client.get(ContentType.APPLICATION_OCTET_STREAM);
		int status = client.getLastHttpStatus();
		assertEquals("Got: "+client.getLastStatus(),HttpStatus.SC_OK, status);
		return EntityUtils.toString(response.getEntity());
	}
	
	private String getPartialContent(BaseClient client, String file, long offset, long length) throws Exception {
		String range = "bytes="+offset+"-";
		if(length>-1)range+=String.valueOf(length+offset-1);
		
		Map<String,String>headers = new HashMap<>();
		headers.put("Range", range);
		client.setURL(file);
		HttpResponse response = client.get(ContentType.APPLICATION_OCTET_STREAM, headers);
		int status = client.getLastHttpStatus();
		assertEquals("Got: "+client.getLastStatus(),HttpStatus.SC_PARTIAL_CONTENT, status);
		return EntityUtils.toString(response.getEntity());
	}
	
	private HttpResponse getFile(BaseClient client, String file) throws Exception {
		client.setURL(file);
		HttpResponse response = client.get(ContentType.APPLICATION_OCTET_STREAM);
		int status = client.getLastHttpStatus();
		assertEquals("Got: "+client.getLastStatus(),HttpStatus.SC_OK, status);
		return response;
	}

	private void putContent(String data, BaseClient client, String url) throws Exception {
		putContent(new ByteArrayInputStream(data.getBytes()), client, url);
	}
	
	private void putContent(InputStream is, BaseClient client, String url) throws Exception {
		client.setURL(url);
		client.put(is, ContentType.APPLICATION_OCTET_STREAM);
		int status = client.getLastHttpStatus();
		assertEquals("Got: "+client.getLastStatus(),HttpStatus.SC_NO_CONTENT, status);
	}

}
