package de.fzj.unicore.uas.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import de.fzj.unicore.uas.Base;
import eu.unicore.client.Endpoint;
import eu.unicore.client.core.FileList.FileListEntry;
import eu.unicore.client.core.StorageClient;
import eu.unicore.client.core.StorageFactoryClient;
import eu.unicore.client.data.HttpFileTransferClient;
import eu.unicore.client.data.TransferControllerClient;
import eu.unicore.client.data.TransferControllerClient.Status;
import eu.unicore.services.rest.client.BaseClient;
import eu.unicore.services.rest.client.RESTException;
import eu.unicore.services.rest.client.UsernamePassword;

public class TestStorages extends Base {

	@BeforeClass
	public static void createTestFile() throws Exception{
		try{
			FileUtils.write(new File("target/unicorex-test/test.txt"), "test data", "UTF-8");
		}catch(IOException e){throw new RuntimeException(e);}
	}
	
	@Test
	public void testFactory() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest";
		String resource  = url+"/core/storages";
		System.out.println("Accessing "+resource);
		BaseClient client = new BaseClient(resource, kernel.getClientConfiguration());
		// create a new SMS
		JSONObject smsDesc = new JSONObject();
		smsDesc.put("name", "my new SMS");
		String smsUrl = client.create(smsDesc);
		System.out.println("created: "+smsUrl);
		// get SMS properties
		client.setURL(smsUrl);
		JSONObject smsProps = client.getJSON();
		System.out.println(smsProps.toString(2));	
	}

	@Test
	public void testFactoryError() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+
				"/rest/core/storagefactories/default_storage_factory";
		Endpoint resource  = new Endpoint(url);
		System.out.println("Accessing "+url);
		StorageFactoryClient smf = new StorageFactoryClient(resource, kernel.getClientConfiguration(), null);
		try{
			smf.createStorage("non-existing-type","my new SMS", null, null);
		}catch(RESTException e) {
			assertTrue(500 == e.getStatus());
			assertTrue(e.getErrorMessage().contains("non-existing-type"));
		}
	}
	
	@Test
	public void testStorageAccess() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest";
		String resource  = url+"/core/storages";
		System.out.println("Accessing "+resource);
		BaseClient client = new BaseClient(resource, kernel.getClientConfiguration());
		JSONObject storageList = client.getJSON();
		int status = client.getLastHttpStatus();
		assertEquals("Got: "+client.getLastStatus(),HttpStatus.SC_OK, status);
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
		assertEquals("Wrong file length", content.length(), newFile.size);
	}

	@Test
	public void testExportFile() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest";
		Endpoint storage = new Endpoint(url + "/core/storages/WORK");
		StorageClient sms = new StorageClient(storage, kernel.getClientConfiguration(), null);
		HttpFileTransferClient fts = (HttpFileTransferClient)sms.createExport("test.txt", "BFT", null);
		String content = IOUtils.toString(fts.getInputStream(), "UTF-8");
		System.out.println(content);
	}

	@Test
	public void testTransferFile() throws Exception {
		StorageClient sms = createStorage();
		String sourceURL = kernel.getContainerProperties().getContainerURL()+
				"/rest/core/storages/WORK/files/test.txt";
		TransferControllerClient tcc = sms.fetchFile(sourceURL, "test.txt", "BFT");
		int c = 0;
		while(!tcc.isComplete() && c<20){
			Thread.sleep(1000);
			c++;
		}
		System.out.println(tcc.getProperties().toString(2));
		assertEquals("Transfer failed", Status.DONE, tcc.getStatus());
	}
	
	@Test
	public void testSearch() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest/core/storages/WORK";
		StorageClient sms = new StorageClient(new Endpoint(url), kernel.getClientConfiguration(), null);
		List<String> fileListing = sms.searchMetadata("foo");
		System.out.println(fileListing);
	}

	@Test(expected = RESTException.class)
	public void testChmodNonExistentFile() throws Exception {
		Endpoint ep = new Endpoint(kernel.getContainerProperties().getContainerURL()+"/rest/core/storages/WORK");
		StorageClient client = new StorageClient(ep,
				kernel.getClientConfiguration(),
				new UsernamePassword("demouser",  "test123"));
		client.chmod("nonexistentpath", "rw-");
		FileListEntry e = client.stat("nonexistentpath");
		System.out.println(e);
	}

	/**
	 * creates a new empty storage and return a client for it
	 */
	private StorageClient createStorage() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+
				"/rest/core/storagefactories/default_storage_factory";
		Endpoint resource  = new Endpoint(url);
		System.out.println("Accessing "+url);
		StorageFactoryClient smf = new StorageFactoryClient(resource, kernel.getClientConfiguration(), null);
		return smf.createStorage();
	}
	
}
