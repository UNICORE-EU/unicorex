package eu.unicore.uas.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.CoreClient;
import eu.unicore.client.core.FileList;
import eu.unicore.client.core.FileList.FileListEntry;
import eu.unicore.client.core.StorageClient;
import eu.unicore.client.data.FileClient;
import eu.unicore.client.data.Metadata;
import eu.unicore.client.utils.TaskClient;
import eu.unicore.services.Kernel;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.services.restclient.UsernamePassword;

/**
 * Metadata functional tests
 *
 * TODO: test multi directory crawling.
 *
 * @author jrybicki
 */
public class TestMetadataFunctional {

	static Kernel kernel;

	@BeforeAll
	public static void init() throws Exception {
		kernel = TestMetadata.initK();
	}

	@AfterAll
	public static void shutDown() throws Exception{
		TestMetadata.shutDown(kernel);
	}

	@Test
	public void testAbsoluteRelative() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest/core/storages/WORK";
		StorageClient sms = new StorageClient(new Endpoint(url), kernel.getClientConfiguration(), null);
		String mDir = sms.getProperties().getString("mountPoint");
		FileUtils.forceMkdir(new File(mDir));
		FileUtils.cleanDirectory(new File(mDir));
		sms.upload("file.txt").write("Some Content".getBytes());

		//relative path:
		FileListEntry gridFile = sms.stat("file.txt");
		System.out.println("Prop: " + gridFile.path);

		FileList listDirectory = sms.ls("/");
		//absolute path:
		gridFile = listDirectory.list().get(0);
		System.out.println("List: " + gridFile.path);

		// add some md
		FileClient mc = sms.getFileClient("file.txt");
		Map<String,String>meta = new HashMap<>();
		meta.put("foo","bar");
		mc.putMetadata(meta);
		
		// copy, rename
		sms.copy("file.txt", "file2.txt");
		sms.rename("file.txt", "file3.txt");
		
		try{
			sms.getFileClient("file2.txt").delete();
			sms.getFileClient("file3.txt").delete();
		}catch(Exception e){
			System.out.println("Exception when deleting file: "+e);
		}
	}

	@Test
	public void testRetrieval() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest/core/storages/WORK";
		StorageClient sms = new StorageClient(new Endpoint(url), kernel.getClientConfiguration(), null);
		String mDir = sms.getProperties().getString("mountPoint");
		FileUtils.forceMkdir(new File(mDir));
		FileUtils.cleanDirectory(new File(mDir));
		//import a test file
		String fileName = "/foo";
		sms.upload(fileName).write("this is a test".getBytes());
		FileListEntry gridFile = sms.stat(fileName);

		//put metadata via client:
		Map<String, String> meta = new HashMap<>();
		meta.put("test", "123");
		sms.getFileClient(gridFile.path).putMetadata(meta);

		//retrive metadata by different means:
		gridFile = sms.stat(fileName);
		
		System.out.println("\nMetadata retrieval:");

		//get via file:
		System.out.println("\nMetadata for the resource " + gridFile.path + " got via REST API");
		Map<String, String> extractMetadataType = gridFile.metadata;
		for (String key : meta.keySet()) {
			assertTrue(extractMetadataType.containsKey(key), "Missing: "+key);
			assertEquals(meta.get(key), extractMetadataType.get(key));
			System.out.println(key + "-->" + extractMetadataType.get(key));
		}
		sms.getFileClient(fileName).putMetadata(Collections.emptyMap());
		sms.getFileClient(fileName).delete();
	}

	@Test
	public void testCreate() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest/core/storages/WORK";
		StorageClient sms = new StorageClient(new Endpoint(url), kernel.getClientConfiguration(), null);
		String mDir = sms.getProperties().getString("mountPoint");
		FileUtils.forceMkdir(new File(mDir));
		FileUtils.cleanDirectory(new File(mDir));
		//import a test file
		String fileName = "/foo2";
		sms.upload(fileName).write("this is a test".getBytes());

		//create via file client
		Map<String, String> metadata = new HashMap<>();
		metadata.put("Key", "Value");
		sms.getFileClient(fileName).putMetadata(metadata);
		
		//check:
		Map<String, String> metadata1 = sms.stat(fileName).metadata;
		assertNotNull(metadata1);
		assertFalse(metadata1.isEmpty());

		for (String key : metadata.keySet()) {
			assertTrue(metadata1.containsKey(key));
			assertTrue(metadata1.containsValue(metadata.get(key)));
			assertEquals(metadata.get(key), metadata1.get(key));
			System.out.println("Key: " + key + " --> " + metadata1.get(key));
		}
		sms.getFileClient(fileName).putMetadata(Collections.emptyMap());
		sms.getFileClient(fileName).delete();
	}

	@Test
	public void testUpdate() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest/core/storages/WORK";
		StorageClient sms = new StorageClient(new Endpoint(url), kernel.getClientConfiguration(), null);
		String mDir = sms.getProperties().getString("mountPoint");
		FileUtils.forceMkdir(new File(mDir));
		FileUtils.cleanDirectory(new File(mDir));
		//import a test file
		String fileName = "/foo2";
		sms.upload(fileName).write("this is a test".getBytes());

		FileClient fc = sms.getFileClient(fileName);
		Map<String, String> metadata = new HashMap<>();
		metadata.put("Key", "Value");
		fc.putMetadata(metadata);
		
		//check:
		Map<String, String> metadata1 = sms.stat(fileName).metadata;
		assertNotNull(metadata1);
		assertFalse(metadata1.isEmpty());
		assertTrue(metadata1.containsKey("Key"));
		assertTrue(metadata1.containsValue("Value"));

		for (String key : metadata.keySet()) {
			assertEquals(metadata.get(key), metadata1.get(key));
			System.out.println("Key: " + key + " --> " + metadata1.get(key));
		}

		System.out.println("Update:");
		Map<String, String> someNewMeta = new HashMap<>();
		someNewMeta.put("NewKey", "NewValue");
		fc.putMetadata(someNewMeta);
		Map<String, String> metadata2 = sms.stat(fileName).metadata;
		assertNotNull(metadata2);
		assertFalse(metadata2.isEmpty());

		for (String key : metadata2.keySet()) {
			System.out.println("Key: " + key + " --> " + metadata2.get(key));
		}
		assertTrue(metadata2.containsKey("NewKey"));
		assertTrue(metadata2.containsValue("NewValue"));
		//XXX: this is ignored since BaseMetadataManagementImpl do not call update metadata it does get/put instead (so that the old data is removed)
		//        assertTrue(metadata2.containsKey("Key"));
		//        assertTrue(metadata2.containsValue("Value"));

		fc.putMetadata(Collections.emptyMap());
		fc.delete();
	}
		
	@Test
	public void testCrawling() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest/core/storages/WORK";
		StorageClient sms = new StorageClient(new Endpoint(url), kernel.getClientConfiguration(), null);
		String mDir = sms.getProperties().getString("mountPoint");
		FileUtils.forceMkdir(new File(mDir));
		FileUtils.cleanDirectory(new File(mDir));
		
		//create some file(s)
		String keyword = "SomeKeyword";
		sms.upload("foo.a").write("this is a test".getBytes());
		sms.upload("foo.a").write("this is a test".getBytes());
		sms.upload("jj.file").write("some other data".getBytes());
		StringBuilder builder = new StringBuilder("<html><head><title>Some title with keyword: ").append(keyword).append("</title></head><body>Some body and the keyword: ").
				append(" </body></html>");
		sms.upload("page.html").write(builder.toString().getBytes());

		FileClient fc = sms.getFileClient("/");
		TaskClient extractTask = fc.startMetadataExtraction(1);
		while(!extractTask.isFinished()) {
			Thread.sleep(1000);
		}
		System.out.println(extractTask.getProperties().toString(2));
		
		//check the page.html was indexed
		List<String>results = sms.searchMetadata("page.html");
		assertEquals(1, results.size());

		FileList listDirectory = sms.ls("/");
		System.out.println("Directory with metadata files:");
		
		for (FileListEntry gridFile : listDirectory.list()) {
			System.out.println("remote file: " + gridFile.path);
			if (gridFile.isDirectory) {
				continue;
			}
			if (MetadataFile.isMetadataFileName(gridFile.path)) {
				continue;
			}

			Map<String, String> metadata = null;

			metadata = sms.getFileClient(gridFile.path).getMetadata();

			if (metadata.isEmpty()) {
				System.out.println("No metadata for this file");
						continue;
			} else {
				System.out.println("Metadata for this file:");
			}

			for (String key : metadata.keySet()) {
				System.out.printf("\t%s-->%s\n", key, metadata.get(key));
			}

		}

		System.out.printf("Searching for keyword %s\n", keyword);
		List<String> found = sms.searchMetadata(keyword);
		assertEquals(1, found.size());
		for (String match : found) {
			System.out.println("Matching document is: " + match);
			System.out.println("Its metadata are:");
			FileClient fc1 = new FileClient(new Endpoint(match), kernel.getClientConfiguration(), null);
			Map<String, String> meta = fc1.getMetadata();
			for (String key : meta.keySet()) {
				System.out.printf("\t%s-->%s\n", key, meta.get(key));
			}
		}

		// update metadata for a single file
		fc = sms.getFileClient("/foo.a");
		Map<String, String> metadata = fc.getMetadata();
		metadata.put("MY_KEY", "MY_VALUE");
		fc.putMetadata(metadata);

		// re-run extraction for this file, index should be updated
		System.out.printf("Re-indexing single file");
		fc.startMetadataExtraction(1);
		// TODO - task impl for REST API
		Thread.sleep(5000);
				
		found = sms.searchMetadata("MY_KEY");
		assertEquals(1, found.size());
		fc = sms.getFileClient("/foo.a");
		assertTrue(fc.getMetadata().containsValue("MY_VALUE"));

		sms.getFileClient("/foo.a").delete();
		sms.getFileClient("/jj.file").delete();
		sms.getFileClient("/page.html").delete();

		// metadata is removed from index?
		found = sms.searchMetadata("MY_KEY");
		assertEquals(0, found.size());
	}


	@Test
	public void testCrawlingWithControlFile() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest/core/storages/WORK";
		StorageClient sms = new StorageClient(new Endpoint(url), kernel.getClientConfiguration(), null);
		String mDir = sms.getProperties().getString("mountPoint");
		FileUtils.forceMkdir(new File(mDir));
		FileUtils.cleanDirectory(new File(mDir));

		//create some file(s)
		String keyword = "SomeKeyword";
		sms.upload("reallyIgnoreThis.a").write("this is a test".getBytes());
		sms.upload("jj.file").write("some other data".getBytes());
		StringBuilder builder = new StringBuilder("<html><head><title>Some title with keyword: ").append(keyword).append("</title></head><body>Some body and the keyword: ").
				append(" </body></html>");
		sms.upload("page.html").write(builder.toString().getBytes());

		// create control file to exclude indexing *.a files
		Metadata.writeCrawlerControlFile(sms, "/", new Metadata.CrawlerControl(null, new String[]{"*.a"}));

		FileClient fc = sms.getFileClient("/");
		TaskClient extractTask = fc.startMetadataExtraction(1);
		while(!extractTask.isFinished()) {
			Thread.sleep(1000);
		}
		System.out.println(extractTask.getProperties().toString(2));
		
		List<String>results = sms.searchMetadata("reallyIgnoreThis.a");
		assertTrue(results.size()==0);

		sms.getFileClient("/reallyIgnoreThis.a").delete();
		sms.getFileClient("/jj.file").delete();;
		sms.getFileClient("/page.html").delete();
	}

	@Test
	public void testFederatedSearch() throws Exception
	{
		String url = kernel.getContainerProperties().getContainerURL()+"/rest/core";
		StorageClient sms = new StorageClient(new Endpoint(url+"/storages/WORK"), kernel.getClientConfiguration(), getAuth());
		String mDir = sms.getProperties().getString("mountPoint");
		FileUtils.forceMkdir(new File(mDir));
		FileUtils.cleanDirectory(new File(mDir));
		sms.upload("foo.a").write("this is some test content...".getBytes());
		Map<String, String> meta = new HashMap<>();
		meta.put("Author", "Some One");
		meta.put("Title", "Review letters");
		sms.getFileClient("foo.a").putMetadata(meta);
		sms.upload("bar.a").write("not this one".getBytes());
		Map<String, String> meta2 = new HashMap<>();
		meta2.put("Author", "Not Me");
		meta2.put("Title", "Journal of Useless Research");
		sms.getFileClient("bar.a").putMetadata(meta2);

		CoreClient cc = new CoreClient(new Endpoint(url), kernel.getClientConfiguration(), getAuth());
		JSONObject fedSearch = new JSONObject();
		fedSearch.put("query", "Author:S*");
		JSONArray resources = new JSONArray();
		resources.put(url+"/storages/WORK");
		fedSearch.put("resources", resources);
		TaskClient tc = cc.launchFederatedSearch(fedSearch);
		tc.poll(null);
		System.out.println("Search finished. Status: "+tc.getStatus()+", status message: "+tc.getStatusMessage());
		Map<String,String> result = tc.getResult();
		System.out.println(result);
		assertEquals("1", result.get("storageCount"));
		assertEquals("1", result.get("resourceCount"));
		assertEquals("foo.a", new File(new URL(result.get("search-result-1")).getPath()).getName());
	}


	@Test
	public void testSearch() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest/core/storages/WORK";
		StorageClient sms = new StorageClient(new Endpoint(url), kernel.getClientConfiguration(), getAuth());
		String mDir = sms.getProperties().getString("mountPoint");
		FileUtils.forceMkdir(new File(mDir));
		FileUtils.cleanDirectory(new File(mDir));

		//create some file(s)
		sms.upload("foo.a").write("this is a test".getBytes());
		sms.upload("jj.file").write("some other data".getBytes());
		sms.upload("bar.a").write("yet another piece of information".getBytes());
		sms.upload("page.html").write("some content".getBytes());
		sms.upload("bar2.a").write("yupi doopi doooo".getBytes());

		Map<String, String> meta = new HashMap<>();
		meta.put("Author", "Rybicki");
		meta.put("Title", "Review letters");
		sms.getFileClient("/foo.a").putMetadata(meta);

		meta.clear();
		meta.put("Author", "Schueller");
		meta.put("Title", "Protocols of intranet/Internet");
		meta.put("Date", "13/04/2010");
		sms.getFileClient("/jj.file").putMetadata(meta);

		meta.clear();
		meta.put("Author", "Rybicky");
		meta.put("Title", "Peer to peer for all");
		sms.getFileClient("/bar.a").putMetadata(meta);
		
		meta.clear();		
		meta.put("Author", "Ribicky");
		meta.put("Title", "Some other title I can get rihgt now");
		sms.getFileClient("/bar2.a").putMetadata(meta);

		System.out.println("Check the metadata\n");
		System.out.println(sms.stat("foo.a").metadata);
		System.out.println(sms.stat("jj.file").metadata);
		System.out.println(sms.stat("page.html").metadata);
		System.out.println(sms.stat("bar.a").metadata);
		System.out.println(sms.stat("bar2.a").metadata);

		System.out.println("Queries\n");
		String query = "Author:[Rybicki TO Zander]";
		List<String> result = sms.searchMetadata(query);
		System.out.println("Matches for " + query);
		for (String hit : result) {
			System.out.println("\t:" + hit);
		}

		query = "Author:Rybicki~";
		result = sms.searchMetadata(query);
		
		System.out.println("Matches for " + query);
		for (String hit : result) {
			System.out.println("\t:" + hit);
		}
	}

	@Test
	public void testMultiDirectoryCrawler() {
		assertTrue(true);
	}

	@Test
	public void testSubdirCrawler() {
		assertTrue(true);
	}

	private IAuthCallback getAuth() {
		return new UsernamePassword("demouser", "test123");
	}
}
