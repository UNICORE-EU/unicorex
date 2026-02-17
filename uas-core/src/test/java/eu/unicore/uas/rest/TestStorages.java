package eu.unicore.uas.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.hc.core5.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.CoreClient;
import eu.unicore.client.core.EnumerationClient;
import eu.unicore.client.core.FileList.FileListEntry;
import eu.unicore.client.core.StorageClient;
import eu.unicore.client.core.StorageFactoryClient;
import eu.unicore.client.data.FiletransferClient;
import eu.unicore.client.data.HttpFileTransferClient;
import eu.unicore.client.data.TransferControllerClient;
import eu.unicore.client.data.TransferControllerClient.Status;
import eu.unicore.services.restclient.BaseClient;
import eu.unicore.services.restclient.RESTException;
import eu.unicore.uas.Base;
import eu.unicore.uas.fts.FiletransferOptions.ReadStream;

public class TestStorages extends Base {

	final static String testdata = "test data";

	@BeforeAll
	public static void createTestFile() throws Exception{
		FileUtils.write(new File("target/unicorex-test/test.txt"), testdata, "UTF-8");
	}

	@Test
	public void testCreateSMS() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest";
		String resource  = url+"/core/storages";
		System.out.println("Accessing "+resource);
		BaseClient client = new BaseClient(resource, kernel.getClientConfiguration(), getAuth());
		JSONObject smsDesc = new JSONObject();
		smsDesc.put("name", "my new SMS");
		String smsUrl = client.create(smsDesc);
		StorageClient sms = new StorageClient(new Endpoint(smsUrl), kernel.getClientConfiguration(), getAuth());
		sms.setUpdateInterval(-1);
		System.out.println("created: "+smsUrl);
		JSONObject p = new JSONObject();
		p.put("umask", "022");
		p.put("description", "Just a test");
		JSONObject r = sms.setProperties(p);
		assertEquals("OK", r.getString("umask"));
		assertEquals("OK", r.getString("description"));
		assertEquals("022", sms.getProperties().getString("umask"));
		JSONObject smsProps = sms.getProperties();
		System.out.println(smsProps.toString(2));
		sms.delete();
	}

	@Test
	public void testDirectoryScan()throws Exception{
		String url = kernel.getContainerProperties().getContainerURL()
				+"/rest/core/storagefactories/DEFAULT";
		StorageFactoryClient smf = new StorageFactoryClient(new Endpoint(url), 
				kernel.getClientConfiguration(), getAuth());
		Map<String,String>params = new HashMap<>();
		params.put("enableTrigger", "true");
		StorageClient sms = smf.createStorage("test123", params, null);
		Thread.sleep(1000);
		System.out.println(sms.getProperties().toString(2));
	}
	
	
	@Test
	public void testFindFactories() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest/core";
		CoreClient client = new CoreClient(new Endpoint(url), kernel.getClientConfiguration(), getAuth());
		List<StorageFactoryClient>sfcs = client.getStorageFactories();
		assertTrue(sfcs.size()>0);
	}

	@Test
	public void testFactory() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+
				"/rest/core/storagefactories/DEFAULT";
		Endpoint resource  = new Endpoint(url);
		System.out.println("Accessing "+url);
		StorageFactoryClient smf = new StorageFactoryClient(resource, kernel.getClientConfiguration(), getAuth());
		smf.setUpdateInterval(-1);
		JSONObject props = smf.getProperties();
		System.out.println("Factory properties: \n"+props.toString(2));
		StorageClient sms = smf.createStorage("my new SMS", Collections.emptyMap(), null);
		System.out.println("New Storage in <"+sms.getMountPoint()+">");
		sms.delete();
		props = smf.getProperties();
	}

	@Test
	@SuppressWarnings("deprecation")
	public void testFactoryErrorType() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+
				"/rest/core/storagefactories/default_storage_factory";
		Endpoint resource  = new Endpoint(url);
		System.out.println("Accessing "+url);
		StorageFactoryClient smf = new StorageFactoryClient(resource, kernel.getClientConfiguration(), getAuth());
		try{
			smf.createStorage("non-existing-type","my new SMS", null, null);
		}catch(RESTException e) {
			assertTrue(500 == e.getStatus());
			assertTrue(e.getErrorMessage().contains("non-existing-type"));
		}
	}

	@Test
	public void testFactoryErrorPath() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+
				"/rest/core/storagefactories/DEFAULT";
		Endpoint resource  = new Endpoint(url);
		System.out.println("Accessing "+url);
		StorageFactoryClient smf = new StorageFactoryClient(resource, kernel.getClientConfiguration(), getAuth());
		JSONObject props = smf.getProperties();
		System.out.println(props.toString(2));
		try{
			var params = new HashMap<String,String>();
			params.put("path", "some/path/");
			smf.createStorage("my new SMS", params, null);
		}catch(RESTException e) {
			assertTrue(500 == e.getStatus());
			assertTrue(e.getErrorMessage().contains("not allowed"));
		}
	}

	@Test
	public void testStorageAccess() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest";
		String resource  = url+"/core/storages";
		System.out.println("Accessing "+resource);
		BaseClient client = new BaseClient(resource, kernel.getClientConfiguration(), getAuth());
		JSONObject storageList = client.getJSON();
		int status = client.getLastHttpStatus();
		assertEquals(HttpStatus.SC_OK, status);
		System.out.println("*** storages:");
		System.out.println(storageList.toString(2));

		String storage = url + "/core/storages/WORK";
		client.setURL(storage);
		JSONObject storageProps = client.getJSON();
		System.out.println("*** storage "+storage+":");
		System.out.println(storageProps.toString(2));

		// directory
		client.setURL(storage+"/files");
		JSONObject dirListing = client.getJSON();
		System.out.println("*** Directory listing '/':");
		System.out.println(dirListing.toString(2));
		assertTrue(dirListing.getBoolean("isDirectory"));

		// chunked
		client.setURL(storage+"/files?offset=0&num=2");
		dirListing = client.getJSON();
		System.out.println("*** Chunk of directory listing '/':");
		System.out.println(dirListing.toString(2));
		assertTrue(dirListing.getBoolean("isDirectory"));
		assertEquals(2, dirListing.getJSONObject("content").length());

		// single file
		client.setURL(storage+"/files/test.txt");
		JSONObject fileListing = client.getJSON();
		System.out.println("*** File properties '/test.txt':");
		System.out.println(fileListing.toString(2));
		assertFalse(fileListing.getBoolean("isDirectory"));
	}

	@Test
	public void testImportFile() throws Exception {
		StorageClient sms = createStorage();
		HttpFileTransferClient fts = (HttpFileTransferClient)sms.createImport("test123", false, -1, "BFT", null);
		String content = "uploaded via RESTful interface and BFT";
		fts.write(content.getBytes());
		FileListEntry newFile = sms.stat("test123");
		assertEquals(content.length(), newFile.size);
		fts.setUpdateInterval(-1);
		assertEquals(content.length(), fts.getTransferredBytes());
	}

	@Test
	public void testImportError1() throws Exception {
		StorageClient sms = createStorage();
		assertThrows(IllegalArgumentException.class,
				()->sms.createImport("test123", false, -1, "NOSUCHPROTOCOL", null));
		BaseClient bc = new BaseClient(sms.getEndpoint().getUrl()+"/imports", sms.getSecurityConfiguration(), getAuth());
		JSONObject data = new JSONObject();
		data.put("protocol", "NOSUCHPROTOCOL");
		data.put("file", "foo");
		RESTException re = assertThrows(RESTException.class,
				()->bc.post(data));
		assertTrue(re.getMessage().contains("not available"));
	}

	@Test
	public void testUpload() throws Exception {
		StorageClient sms = createStorage();
		HttpFileTransferClient fts = sms.upload("test123");
		String content = "uploaded via RESTful interface and BFT";
		fts.write(content.getBytes());
		FileListEntry newFile = sms.stat("test123");
		assertEquals(content.length(), newFile.size);
		// need new file transfer for appending
		String appended = "appended stuff";
		fts = (HttpFileTransferClient)sms.createImport("test123", true, -1, "BFT", null);
		fts.write(appended.getBytes());
		newFile = sms.stat("test123");
		assertEquals(content.length()+appended.length(), newFile.size);
	}

	@Test
	public void testExportFile() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest";
		Endpoint storage = new Endpoint(url + "/core/storages/WORK");
		StorageClient sms = new StorageClient(storage, kernel.getClientConfiguration(), getAuth());
		FiletransferClient fts = sms.createExport("test.txt", "BFT", null);
		assertEquals("BFT", fts.getProtocol());
		assertTrue(fts instanceof ReadStream);
		assertEquals(testdata, IOUtils.toString(((ReadStream)fts).getInputStream(), "UTF-8"));
	}

	@Test
	public void testExportError1() throws Exception {
		StorageClient sms = createStorage();
		assertThrows(IllegalArgumentException.class,
				()->sms.createExport("test123", "NOSUCHPROTOCOL", null));
		BaseClient bc = new BaseClient(sms.getEndpoint().getUrl()+"/exports", sms.getSecurityConfiguration());
		JSONObject data = new JSONObject();
		data.put("protocol", "NOSUCHPROTOCOL");
		data.put("file", "foo");
		RESTException re = assertThrows(RESTException.class,
				()->bc.post(data));
		assertTrue(re.getMessage().contains("not available"));
	}

	@Test
	public void testDownload() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest";
		Endpoint storage = new Endpoint(url + "/core/storages/WORK");
		StorageClient sms = new StorageClient(storage, kernel.getClientConfiguration(), getAuth());
		HttpFileTransferClient fts = sms.download("test.txt");
		String content = IOUtils.toString(fts.getInputStream(), "UTF-8");
		assertEquals(testdata, content);
	}

	@Test
	public void testDownloadPartial() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest";
		Endpoint storage = new Endpoint(url + "/core/storages/WORK");
		StorageClient sms = new StorageClient(storage, kernel.getClientConfiguration(), getAuth());
		HttpFileTransferClient fts = sms.download("test.txt");
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		fts.read(4, testdata.length()-4, os);
		assertEquals(testdata.substring(4), os.toString("UTF-8"));
		os = new ByteArrayOutputStream();
		long n = fts.readTail(4, os);
		assertEquals("data", os.toString("UTF-8"));
		assertEquals(4, n);
	}

	@Test
	public void testTransferFile() throws Exception {
		StorageClient sms = createStorage();
		String sourceURL = kernel.getContainerProperties().getContainerURL()+
				"/rest/core/storages/WORK/files/test.txt";
		TransferControllerClient tcc = sms.fetchFile(sourceURL, "test.txt", null, "BFT");
		tcc.setTags(Arrays.asList("test"));
		tcc.poll(Status.DONE, 20);
		System.out.println(tcc.getProperties().toString(2));
		assertEquals(Status.DONE, tcc.getStatus());
		assertEquals("test", tcc.getProperties().getJSONArray("tags").get(0));
	}

	@Test
	public void testSearch() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest/core/storages/WORK";
		StorageClient sms = new StorageClient(new Endpoint(url), kernel.getClientConfiguration(), getAuth());
		List<String> fileListing = sms.searchMetadata("foo");
		System.out.println(fileListing);
	}

	@Test
	public void testChmodNonExistentFile() throws Exception {
		Endpoint ep = new Endpoint(kernel.getContainerProperties().getContainerURL()+"/rest/core/storages/WORK");
		StorageClient client = new StorageClient(ep, kernel.getClientConfiguration(), getAuth());
		assertThrows(RESTException.class, ()->{
			client.chmod("nonexistentpath", "rw-");
		});
	}

	@Test
	public void testListStorages()throws Exception {
		String baseUrl = kernel.getContainerProperties().getContainerURL()+
				"/rest/core/";
		CoreClient base = new CoreClient(new Endpoint(baseUrl), kernel.getClientConfiguration(), getAuth());
		base.getSiteClient(); // make sure all per-user storages are created
		EnumerationClient ec = base.getStoragesList();
		ec.setUpdateInterval(-1);
		JSONArray p = ec.getProperties().getJSONArray("storages");
		int l1 = p.length();
		runJob();
		p = ec.getProperties().getJSONArray("storages");
		assertEquals(l1, p.length(), "working dirs should not be listed");
		ec.setFilter("all");
		p = ec.getProperties().getJSONArray("storages");
		System.out.println(p);
		assertTrue(p.length()>l1, "working dirs should be listed");
	}

	// runs empty job to create a working directory
	private void runJob() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest/core/jobs";
		System.out.println("Accessing "+url);
		BaseClient client = new BaseClient(url, kernel.getClientConfiguration(), getAuth());
		JSONObject task = new JSONObject();
		String jobUrl = client.create(task);
		System.out.println("created: "+jobUrl);
	}

	/**
	 * creates a new empty storage and return a client for it
	 */
	private StorageClient createStorage() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+
				"/rest/core/storagefactories/default_storage_factory";
		Endpoint resource  = new Endpoint(url);
		System.out.println("Accessing "+url);
		StorageFactoryClient smf = new StorageFactoryClient(resource, kernel.getClientConfiguration(),  getAuth());
		return smf.createStorage();
	}
	
}
