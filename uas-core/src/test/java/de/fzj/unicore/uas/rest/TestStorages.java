package de.fzj.unicore.uas.rest;

import static junit.framework.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import de.fzj.unicore.uas.Base;
import eu.unicore.services.rest.client.BaseClient;

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
		HttpResponse response = client.post(smsDesc);
		int status = client.getLastHttpStatus();
		assertEquals("Got: "+client.getLastStatus(),201, status);
		String smsUrl = response.getFirstHeader("Location").getValue();
		System.out.println("created: "+smsUrl);
		EntityUtils.consumeQuietly(response.getEntity());

		// get SMS properties
		client.setURL(smsUrl);
		JSONObject smsProps = client.getJSON();
		System.out.println(smsProps.toString(2));	
	}

	@Test
	public void testFactory2() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest";
		String resource  = url+"/core/storagefactories/default_storage_factory";
		System.out.println("Accessing "+resource);
		BaseClient client = new BaseClient(resource, kernel.getClientConfiguration());
		
		// get factory properties
		JSONObject smfProps = client.getJSON();
		System.out.println(smfProps.toString(2));
		
		// create a new SMS
		JSONObject smsDesc = new JSONObject();
		smsDesc.put("name", "my new SMS");
		HttpResponse response = client.post(smsDesc);
		int status = client.getLastHttpStatus();
		assertEquals("Got: "+client.getLastStatus(),201, status);
		String smsUrl = response.getFirstHeader("Location").getValue();
		System.out.println("created: "+smsUrl);
		EntityUtils.consumeQuietly(response.getEntity());

		// get SMS properties
		client.setURL(smsUrl);
		JSONObject smsProps = client.getJSON();
		System.out.println(smsProps.toString(2));
	}
	
	@Test
	public void testFactory3() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest";
		String resource  = url+"/core/storagefactories/default_storage_factory";
		System.out.println("Accessing "+resource);
		BaseClient client = new BaseClient(resource, kernel.getClientConfiguration());
		
		// create a new SMS with a non existent type
		JSONObject smsDesc = new JSONObject();
		smsDesc.put("name", "my new SMS");
		smsDesc.put("type", "non-existing-type");
		HttpResponse response = client.post(smsDesc);
		int status = client.getLastHttpStatus();
		assertEquals("Got: "+client.getLastStatus(),500, status);
		String errorMsg = EntityUtils.toString(response.getEntity(),"UTF-8");
		System.out.println(new JSONObject(errorMsg).toString(2));
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
		assertEquals("true", dirListing.getString("isDirectory"));
		
		
		// chunked
		client.setURL(storage+"/files?offset=0&num=2");
		dirListing = client.getJSON();
		System.out.println("*** Chunk of directory listing '/':");
		System.out.println(dirListing.toString(2));
		assertEquals("true", dirListing.getString("isDirectory"));
		assertEquals(2, dirListing.getJSONObject("content").length());
			
		// single file
		client.setURL(storage+"/files/test.txt");
		JSONObject fileListing = client.getJSON();
		System.out.println("*** File properties '/test.txt':");
		System.out.println(fileListing.toString(2));
		assertEquals("false", fileListing.getString("isDirectory"));
	}
	
	@Test
	public void testImportFile() throws Exception {
		String storage = createStorage() ;
		BaseClient client = new BaseClient(storage,kernel.getClientConfiguration());
		JSONObject fileImport = new JSONObject();
		fileImport.put("file", "test123");
		client.setURL(storage+"/imports");
		HttpResponse response = client.post(fileImport);
		int status = client.getLastHttpStatus();
		assertEquals("Got: "+client.getLastStatus(),HttpStatus.SC_OK, status);
		String transferUrl = response.getFirstHeader("Location").getValue();
		System.out.println("created: "+transferUrl);

		JSONObject ftProps = client.asJSON(response);
		System.out.println(ftProps.toString(2));

		String bftAccessURL = ftProps.getString("accessURL");

		String content = "uploaded via RESTful interface and BFT";
		putContent(new ByteArrayInputStream(content.getBytes()), client, bftAccessURL);

		String newFile = storage + "/files/test123";
		client.setURL(newFile);
		JSONObject newFileProperties = client.getJSON();
		System.out.println(newFileProperties.toString(2));

		assertEquals("Wrong file length", content.length(), newFileProperties.getLong("size"));
	}

	@Test
	public void testExportFile() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest";
		String storage = url + "/core/storages/WORK";
		BaseClient client = new BaseClient(storage,kernel.getClientConfiguration());
		JSONObject fileExport = new JSONObject();
		fileExport.put("file", "test.txt");
		client.setURL(storage+"/exports");
		HttpResponse response = client.post(fileExport);
		int status = client.getLastHttpStatus();
		assertEquals("Got: "+client.getLastStatus(),HttpStatus.SC_OK, status);
		String transferUrl = response.getFirstHeader("Location").getValue();
		System.out.println("created: "+transferUrl);

		JSONObject ftProps = client.asJSON(response);
		System.out.println(ftProps.toString(2));

		String bftAccessURL = ftProps.getString("accessURL");

		String content = getContent(client, bftAccessURL);
		System.out.println(content);

	}

	@Test
	public void testTransferFile() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest";
		String resource  = createStorage() + "/transfers/" ;
		BaseClient client = new BaseClient(resource, kernel.getClientConfiguration());
		
		JSONObject transferTask = new JSONObject();

		transferTask.put("source",url+"/core/storages/WORK"
				+"/files/test.txt");
		transferTask.put("file","/test.txt");

		HttpResponse response = client.post(transferTask);
		int status = client.getLastHttpStatus();
		assertEquals("Got: "+client.getLastStatus(),HttpStatus.SC_CREATED, status);

		String transfer = response.getFirstHeader("Location").getValue();
		client.setURL(transfer);
		System.out.println("created: "+transfer);
		EntityUtils.consumeQuietly(response.getEntity());
		String ftStatus = null;
		while(true){
			Thread.sleep(1000);
			ftStatus = client.getJSON().getString("status");
			if("DONE".equals(ftStatus) || "FAILED".equals(ftStatus))break;
		}

		// print properties
		System.out.println(client.getJSON().toString(2));
		assertEquals("Transfer failed", "DONE", ftStatus);
	}
	
	@Test
	public void testSearch() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest";
		BaseClient client = new BaseClient(url,kernel.getClientConfiguration());
		
		String storage = url + "/core/storages/WORK";
		client.setURL(storage+"/search?q=foo");
		JSONObject fileListing = client.getJSON();
		System.out.println(fileListing.toString(2));
		
	}

	
	private String getContent(BaseClient client, String file) throws Exception {
		client.setURL(file);
		HttpResponse response = client.get(ContentType.APPLICATION_OCTET_STREAM);
		int status = client.getLastHttpStatus();
		assertEquals("Got: "+client.getLastStatus(),HttpStatus.SC_OK, status);
		return EntityUtils.toString(response.getEntity());
	}

	private void putContent(InputStream is, BaseClient client, String url) throws Exception {
		client.setURL(url);
		client.put(is, ContentType.APPLICATION_OCTET_STREAM);
		int status = client.getLastHttpStatus();
		assertEquals("Got: "+client.getLastStatus(),HttpStatus.SC_NO_CONTENT, status);
	}
	

	/**
	 * creates a new empty storage and returns its URL
	 */
	private String createStorage() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest";
		String resource  = url+"/core/storages";
		System.out.println("Accessing "+resource);
		BaseClient client = new BaseClient(resource, kernel.getClientConfiguration());
		JSONObject task = new JSONObject();
		HttpResponse response = client.post(task);
		int status = client.getLastHttpStatus();
		assertEquals("Got: "+client.getLastStatus(),201, status);
		String storage = response.getFirstHeader("Location").getValue();
		System.out.println("created: "+storage);
		EntityUtils.consumeQuietly(response.getEntity());
		return storage;
	}
	
}
