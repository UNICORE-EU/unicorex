package eu.unicore.uas.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.FileList.FileListEntry;
import eu.unicore.client.core.StorageClient;
import eu.unicore.client.core.StorageFactoryClient;
import eu.unicore.client.data.FileClient;
import eu.unicore.client.utils.TaskClient;
import eu.unicore.services.restclient.BaseClient;
import eu.unicore.uas.Base;
import eu.unicore.uas.util.MockMetadataManager;

/**
 * runs some tests on the metadata support
 * 
 * @author schuller
 */
public class TestMetadata extends Base {

	public static final String CONTENT_TYPE="Content-Type";
	
	public static final String CONTENT_MD5="Content-MD5";

	@Test
	public void testMetadata()throws Exception{
		String url = kernel.getContainerProperties().getContainerURL()+"/rest";
		String resource  = url+"/core/storagefactories/default_storage_factory";
		var auth = getAuth();
		StorageFactoryClient smfClient = new StorageFactoryClient(new Endpoint(resource),
				kernel.getClientConfiguration(), auth);
		StorageClient sms = smfClient.createStorage();
		assertTrue(sms.supportsMetadata());

		// stage in a file
		String in = "this is a test";
		ByteArrayInputStream bis = new ByteArrayInputStream(in.getBytes());
		String md5 = MockMetadataManager.computeMD5(bis);
		sms.upload("foo").write(in.getBytes());

		// fill some metadata and see we can retrieve them
		Map<String,String>metadata = new HashMap<>();
		metadata.put("ham","spam");
		metadata.put("test","123");
		FileClient fc = sms.getFileClient("/foo");
		fc.putMetadata(metadata);

		// check md5 is attached to listProperties() result
		FileListEntry file = sms.stat("foo");
		assertTrue(file!=null);
		assertTrue(md5.equals(file.metadata.get(CONTENT_MD5)));

		// get via client
		Map<String,String>map = fc.getMetadata();
		assertTrue(map.get("ham").equals("spam"));

		//update
		map.put("newkey", "newvalue");
		fc.putMetadata(map);
		map = fc.getMetadata();
		assertTrue("newvalue".equals(map.get("newkey")));

		// delete
		fc.putMetadata(new HashMap<>());
		map = fc.getMetadata();
		assertTrue(map.isEmpty());

		// create
		map.put("test","test-value");
		fc.putMetadata(map);
		map = fc.getMetadata();
		assertTrue(map.size()>0);
		assertTrue("test-value".equals(map.get("test")));

		// search
		List<String>results = sms.searchMetadata("test-value");
		System.out.println(results);
		assertEquals(1, results.size());
		assertTrue(results.get(0).contains("foo"));

		// federated search
		BaseClient bc = new BaseClient(url+"/core/storages/search", kernel.getClientConfiguration(), auth);
		JSONObject fedSearchJob = new JSONObject();
		fedSearchJob.put("query", "test");
		fedSearchJob.put("resources", new JSONArray("[]"));
		String taskURL = bc.create(fedSearchJob);
		TaskClient searchTask = new TaskClient(new Endpoint(taskURL), kernel.getClientConfiguration(), auth);
		searchTask.poll(null);
		System.out.println("Search task : "+searchTask.getStatus());
		System.out.println("Search results : "+searchTask.getResult());

		// extraction
		FileClient baseDir = sms.getFileClient("/");
		JSONObject reply = baseDir.executeAction("extract", new JSONObject());
		System.out.println(reply.toString(2));
	}

}