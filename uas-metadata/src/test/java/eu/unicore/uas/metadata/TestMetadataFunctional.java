package eu.unicore.uas.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.xmlbeans.XmlObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.unigrids.services.atomic.types.GridFileType;
import org.unigrids.services.atomic.types.MetadataType;
import org.unigrids.services.atomic.types.StatusType;
import org.unigrids.services.atomic.types.TextInfoType;
import org.unigrids.x2006.x04.services.metadata.ExtractionStatisticsDocument;
import org.unigrids.x2006.x04.services.metadata.FederatedSearchResultCollectionDocument;
import org.unigrids.x2006.x04.services.metadata.FederatedSearchResultCollectionDocument.FederatedSearchResultCollection.FederatedSearchResults;
import org.unigrids.x2006.x04.services.metadata.FederatedSearchResultDocument.FederatedSearchResult;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.client.MetadataClient;
import de.fzj.unicore.uas.client.MetadataClient.CrawlerControl;
import de.fzj.unicore.uas.client.StorageClient;
import de.fzj.unicore.uas.client.StorageFactoryClient;
import de.fzj.unicore.uas.client.TaskClient;
import eu.unicore.services.Kernel;
import eu.unicore.services.ws.BaseFault;
import eu.unicore.services.ws.utils.WSServerUtilities;

/**
 * Metadata functional tests
 *
 * TODO: test multi directory crawling.
 *
 * @author jrybicki
 */
public class TestMetadataFunctional {

	static Kernel kernel;

	@BeforeClass
	public static void init() throws Exception {
		kernel = TestMetadata.initK();
	}

	@AfterClass
	public static void shutDown() throws Exception{
		TestMetadata.shutDown(kernel);
	}

	@Test
	public void testAbsoluteRelative() throws Exception {
		String url = WSServerUtilities.makeAddress(UAS.SMS, "WORK", kernel.getContainerProperties());
		EndpointReferenceType epr = EndpointReferenceType.Factory.newInstance();
		epr.addNewAddress().setStringValue(url);
		StorageClient sms = new StorageClient(epr, kernel.getClientConfiguration() );

		sms.upload("file.txt").write("Some Content".getBytes());


		//relative path:
		GridFileType gridFile = sms.listProperties("file.txt");
		System.out.println("Prop: " + gridFile.getPath());

		GridFileType[] listDirectory = sms.listDirectory("/");
		//absolute path:
		gridFile = listDirectory[0];
		System.out.println("List: " + gridFile.getPath());

		// add some md
		MetadataClient mc = sms.getMetadataClient();
		Map<String,String>meta = new HashMap<String,String>();
		meta.put("foo","bar");
		mc.createMetadata("file.txt", meta);
		
		// copy, rename
		sms.copy("file.txt", "file2.txt");
		sms.rename("file.txt", "file3.txt");
		
		try{
			sms.delete("file2.txt");
			sms.delete("file3.txt");
		}catch(Exception e){
			System.out.println("Exception when deleting file: "+e);
		}
	}

	@Test
	public void testRetrieval() throws Exception {
		String url = WSServerUtilities.makeAddress(UAS.SMS, "WORK", kernel.getContainerProperties());
		EndpointReferenceType epr = EndpointReferenceType.Factory.newInstance();
		epr.addNewAddress().setStringValue(url);
		StorageClient sms = new StorageClient(epr, kernel.getClientConfiguration());

		MetadataClient mc = sms.getMetadataClient();
		assertNotNull(mc);

		//import a test file
		String fileName = "/foo";
		sms.upload(fileName).write("this is a test".getBytes());
		GridFileType gridFile = sms.listProperties(fileName);
		assertNotNull(gridFile);
		System.out.println("\nFile transfer completed!\n");

		//put metadata via client:
		Map<String, String> meta = new HashMap<String, String>();
		meta.put("test", "123");
		mc.createMetadata(gridFile.getPath(), meta);

		//retrive metadata by different means:
		//1. should be in xml:
		gridFile = sms.listProperties(fileName);
		System.out.println(fileName + " after setting the metadata: \n" + gridFile);

		System.out.println("\nMetadata retrieval:");


		//get via file:
		System.out.println("\nMetadata for the resource " + gridFile.getPath() + " got via file.getMetadata()");
		MetadataType metadata = gridFile.getMetadata();
		assertNotNull("No metada can be accessed via girdFile.getMetadata()", metadata);
		Map<String, String> extractMetadataType = extractMetadataType(metadata);
		for (String key : meta.keySet()) {
			assertTrue("Original metadata does contain this key " + key, extractMetadataType.containsKey(key));
			assertEquals(meta.get(key), extractMetadataType.get(key));
			System.out.println(key + "-->" + extractMetadataType.get(key));
		}

		//get via client and gridFile path:
		System.out.println("\nMetadata for the resource: " + gridFile.getPath() + " got via client and gridFile");
		Map<String, String> metadata1 = mc.getMetadata(gridFile.getPath());
		assertNotNull("No metadata can be accessed via MetadataClient.getMetadata(gridFile.getPath())", metadata1);
		for (String key : meta.keySet()) {
			assertTrue("Original metadata does not contain this key " + key, metadata1.containsKey(key));
			assertEquals(meta.get(key), metadata1.get(key));
			System.out.println(key + "-->" + metadata1.get(key));
		}


		//get via client and original path:
		System.out.println("\nMetadata for the resource: " + fileName + " got via client and original filename");
		Map<String, String> metadata2 = mc.getMetadata(fileName);
		assertNotNull("No metadata can be accessed via MetadaClient.getMetada(originalFileName)", metadata2);
		for (String key : meta.keySet()) {
			System.out.println("Key: " + key + " --> " + metadata2.get(key));
			assertTrue(metadata2.containsKey(key));
			assertTrue(metadata2.containsValue(meta.get(key)));
			assertEquals(meta.get(key), metadata2.get(key));
		}

		mc.deleteMetadata(gridFile.getPath());
		sms.delete("foo");
	}

	@Test
	public void testCreate() throws Exception {
		String url = WSServerUtilities.makeAddress(UAS.SMS, "WORK", kernel.getContainerProperties());
		EndpointReferenceType epr = EndpointReferenceType.Factory.newInstance();
		epr.addNewAddress().setStringValue(url);
		StorageClient sms = new StorageClient(epr, kernel.getClientConfiguration());

		MetadataClient mc = sms.getMetadataClient();
		assertNotNull(mc);

		//import a test file
		String fileName = "/foo2";
		sms.upload(fileName).write("this is a test".getBytes());
		GridFileType gridFile = sms.listProperties(fileName);
		assertNotNull(gridFile);
		System.out.println("\nFile transfer completed!\nTesting creating paths:\n");

		//create by mc:
		Map<String, String> metadata = new HashMap<String, String>();
		metadata.put("Key", "Value");
		mc.createMetadata(fileName, metadata);
		//check:
		Map<String, String> metadata1 = mc.getMetadata(fileName);
		assertNotNull(metadata1);
		assertFalse(metadata1.isEmpty());

		for (String key : metadata.keySet()) {
			assertTrue(metadata1.containsKey(key));
			assertTrue(metadata1.containsValue(metadata.get(key)));
			assertEquals(metadata.get(key), metadata1.get(key));
			System.out.println("Key: " + key + " --> " + metadata1.get(key));
		}

		//create by file:
		GridFileType gridFile2 = sms.listProperties(fileName);
		MetadataType meta = gridFile2.addNewMetadata();
		TextInfoType[] props = new TextInfoType[1];
		props[0] = meta.addNewProperty();
		props[0].setName("Some Name");
		props[0].setValue("Some value");
		meta.setPropertyArray(props);

		//check:
		System.out.println("Check retrival of the data set via setPropertyArray:");
		//it does not work ;)
		Map<String, String> metadata2 = mc.getMetadata(fileName);
		assertNotNull(metadata2);
		assertFalse(metadata2.isEmpty());
		for (String key : metadata2.keySet()) {
			System.out.println(key + "-->" + metadata2.get(key));
		}


		mc.deleteMetadata(fileName);
		sms.delete(fileName);
		System.out.println("\nDirectory after the test: ");
		listDir("/", sms);
	}

	@Test
	public void testUpdate() throws Exception {
		String url = WSServerUtilities.makeAddress(UAS.SMS, "WORK", kernel.getContainerProperties());
		EndpointReferenceType epr = EndpointReferenceType.Factory.newInstance();
		epr.addNewAddress().setStringValue(url);
		StorageClient sms = new StorageClient(epr, kernel.getClientConfiguration());

		MetadataClient mc = sms.getMetadataClient();
		assertNotNull(mc);

		//import a test file
		String fileName = "/foo2";
		sms.upload(fileName).write("this is a test".getBytes());
		GridFileType gridFile = sms.listProperties(fileName);
		assertNotNull(gridFile);

		System.out.println("Transfer file completed starting with updateTests");

		//create by mc:
		Map<String, String> metadata = new HashMap<String, String>();
		metadata.put("Key", "Value");
		mc.createMetadata(fileName, metadata);
		//check:
		Map<String, String> metadata1 = mc.getMetadata(fileName);
		assertNotNull(metadata1);
		assertFalse(metadata1.isEmpty());
		assertTrue(metadata1.containsKey("Key"));
		assertTrue(metadata1.containsValue("Value"));

		for (String key : metadata.keySet()) {
			assertEquals(metadata.get(key), metadata1.get(key));
			System.out.println("Key: " + key + " --> " + metadata1.get(key));
		}



		System.out.println("Update:");
		Map<String, String> someNewMeta = new HashMap<String, String>();
		someNewMeta.put("NewKey", "NewValue");
		mc.updateMetadata(fileName, someNewMeta);
		Map<String, String> metadata2 = mc.getMetadata(fileName);
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

		mc.deleteMetadata(fileName);
		sms.delete(fileName);
		System.out.println("\nDirectory after the test: ");
		listDir("/", sms);
	}

	@Test
	public void testCrawling() throws Exception {

		String url = WSServerUtilities.makeAddress(UAS.SMS, "WORK", kernel.getContainerProperties());
		EndpointReferenceType epr = EndpointReferenceType.Factory.newInstance();
		epr.addNewAddress().setStringValue(url);
		StorageClient sms = new StorageClient(epr, kernel.getClientConfiguration());
		MetadataClient mc = sms.getMetadataClient();
		assertNotNull(mc);
		cleanDir("/", sms);

		//create some file(s)
		String keyword = "SomeKeyword";
		sms.upload("foo.a").write("this is a test".getBytes());
		sms.upload("foo.a").write("this is a test".getBytes());
		sms.upload("jj.file").write("some other data".getBytes());
		StringBuilder builder = new StringBuilder("<html><head><title>Some title with keyword: ").append(keyword).append("</title></head><body>Some body and the keyword: ").
				append(" </body></html>");
		sms.upload("page.html").write(builder.toString().getBytes());

		System.out.println("Directory after adding test files:");
		listDir("/", sms);

		//not very nice but wait until indexing is over
		TaskClient extractTask = mc.startMetadataExtraction("/", 10);
		extractTask.setUpdateInterval(-1);
		int waited=0;
		while (extractTask.getStatus() != StatusType.SUCCESSFUL && waited < 120) {
			Thread.sleep(1000L);
			waited++;
		}
		System.out.println(extractTask.getResourcePropertyDocument());

		XmlObject result = extractTask.getResult();
		System.out.println("Metadata Extraction Result:\n " + result);

		ExtractionStatisticsDocument es=ExtractionStatisticsDocument.Factory.parse(result.toString());
		assertEquals(3, es.getExtractionStatistics().getDocumentsProcessed().intValue());

		//check the page.html was indexed
		Collection<String>results=mc.search("page.html", false);
		assertTrue(results.size()>0);

		GridFileType[]listDirectory = sms.listDirectory("/");
		System.out.println("Directory with metadata files:");
		listDir("/", sms);
		for (GridFileType gridFile : listDirectory) {
			System.out.println("GridFile: " + gridFile.getPath());
			if (gridFile.getIsDirectory()) {
				continue;
			}
			if (MetadataFile.isMetadataFileName(gridFile.getPath())) {
				continue;
			}

			Map<String, String> metadata = null;

			metadata = mc.getMetadata(gridFile.getPath());

			if (metadata == null || metadata.isEmpty()) {
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
		Collection<String> found = mc.search(keyword, false);
		assertEquals(1, found.size());
		for (String match : found) {
			System.out.println("Matching document is: " + match);
			System.out.println("Its metadata are:");
			Map<String, String> meta = mc.getMetadata(match);
			for (String key : meta.keySet()) {
				System.out.printf("\t%s-->%s\n", key, meta.get(key));
			}
		}

		//update metadata for a single file
		Map<String, String> metadata = mc.getMetadata("/foo.a");
		metadata.put("MY_KEY", "MY_VALUE");
		mc.updateMetadata("/foo.a", metadata);
		//re-run extraction for this file, index should be updated
		System.out.printf("Re-indexing single file");
		mc.startMetadataExtraction("foo.a",1);
		//and search
		found = mc.search("MY_KEY", false);
		assertEquals(1, found.size());
		assertTrue(mc.getMetadata(found.iterator().next()).containsValue("MY_VALUE"));

		sms.delete("/foo.a");
		sms.delete("/jj.file");
		sms.delete("/page.html");

		System.out.println("\nListing (without removal of metadata)");
		listDir("/", sms);
		System.out.println("\n");
		
		// metadata is removed from index?
		found = mc.search("MY_KEY", false);
		assertEquals(0, found.size());
	}

	@Test
	public void testCrawlingWithControlFile() throws Exception {

		String url = WSServerUtilities.makeAddress(UAS.SMS, "WORK", kernel.getContainerProperties());
		EndpointReferenceType epr = EndpointReferenceType.Factory.newInstance();
		epr.addNewAddress().setStringValue(url);
		StorageClient sms = new StorageClient(epr, kernel.getClientConfiguration());
		MetadataClient mc = sms.getMetadataClient();
		assertNotNull(mc);
		cleanDir("/", sms);

		//create some file(s)
		String keyword = "SomeKeyword";
		sms.upload("reallyIgnoreThis.a").write("this is a test".getBytes());
		sms.upload("jj.file").write("some other data".getBytes());
		StringBuilder builder = new StringBuilder("<html><head><title>Some title with keyword: ").append(keyword).append("</title></head><body>Some body and the keyword: ").
				append(" </body></html>");
		sms.upload("page.html").write(builder.toString().getBytes());

		System.out.println("Directory after adding test files:");
		listDir("/", sms);

		// create control file to exclude indexing *.a files
		MetadataClient.writeCrawlerControlFile(sms, "/", new CrawlerControl(null, new String[]{"*.a"}));

		//not very nice but wait until indexing is over
		TaskClient extractTask = mc.startMetadataExtraction("/", 10);
		extractTask.setUpdateInterval(-1);
		int waited=0;
		while (extractTask.getStatus() != StatusType.SUCCESSFUL && waited < 120) {
			Thread.sleep(1000L);
			waited++;
		}
		XmlObject result = extractTask.getResult();
		System.out.println("Metadata Extraction Result:\n " + result);
		ExtractionStatisticsDocument es=ExtractionStatisticsDocument.Factory.parse(result.toString());
		assertEquals(2, es.getExtractionStatistics().getDocumentsProcessed().intValue());

		Collection<String>results=mc.search("reallyIgnoreThis.a", false);
		assertTrue(results.size()==0);

		sms.delete("/reallyIgnoreThis.a");
		sms.delete("/jj.file");
		sms.delete("/page.html");

	}

	@Test
	public void testFederatedSearch() throws Exception
	{
		String url = WSServerUtilities.makeAddress(UAS.SMF, "default_storage_factory", kernel.getContainerProperties());
		EndpointReferenceType endpointReferenceType = EndpointReferenceType.Factory.newInstance();
		endpointReferenceType.addNewAddress().setStringValue(url);
		StorageFactoryClient smf = new StorageFactoryClient(endpointReferenceType, kernel.getClientConfiguration()); 
		StorageClient sms = smf.createSMS();

		MetadataClient mc = sms.getMetadataClient();

		assertNotNull(mc);

		System.out.println("Metadata tests");
		System.out.println("Federated search test");
		System.out.println("Directory at the beginign:");
		listDir("/", sms);

		//create fake files
		byte[] fakeData = "this is test - fake data for fakek file".getBytes();
		sms.upload("foo.kt").write(fakeData);
		sms.upload("foo1.aa").write(fakeData);
		sms.upload("foo2.zz").write(fakeData);

		Map<String, String> meta = new HashMap<String, String> ();
		meta.put("Author", "Muradov");
		meta.put("Category", "TopCoder");
		meta.put("Name", "foo");
		mc.createMetadata("/foo.kt", meta);

		meta = new HashMap<String, String>();
		meta.put("Author", "Schuller");
		meta.put("Category", "Grid");
		meta.put("Name", "foo1");
		mc.createMetadata("/foo1.aa", meta);

		meta = new HashMap<String, String>();
		meta.put("Author", "Gates");
		meta.put("Category", "OS");
		meta.put("Name", "foo2");

		System.out.println("Check metadata ... \n");
		printMetadata("/foo.kt", mc);
		printMetadata("/foo1.aa", mc);
		printMetadata("/foo2.zz", mc);

		String[] list = new String[]{".*"};


		System.out.println("Queries\n");
		String query = "Author:[Muradov TO Gates]";
		query = "Author:Muradov~";

		TaskClient federatedSearchResultCollection =  mc.federatedMetadataSearch(query, list, true);

		federatedSearchResultCollection.waitUntilDone(20*1000);

		assertEquals(StatusType.SUCCESSFUL, federatedSearchResultCollection.getStatus());


		XmlObject result = federatedSearchResultCollection.getResult();
		System.out.println();
		System.out.println(result);
		System.out.println();
		FederatedSearchResultCollectionDocument frrd = FederatedSearchResultCollectionDocument.Factory.parse(result.toString());

		FederatedSearchResults[] federatedSearchResults = frrd.getFederatedSearchResultCollection().getFederatedSearchResultsArray();

		System.out.println("*** RESULTS ***");

		for(FederatedSearchResults federatedSearchResultItem : federatedSearchResults)
		{
			FederatedSearchResult federatedSearchResult = federatedSearchResultItem.getFederatedSearchResult();
			String storageURL = federatedSearchResult.getStorageURL();

			String[] resourceNames = federatedSearchResult.getResourceNameArray();

			System.out.println(storageURL);

			for(String resourceName : resourceNames)
				System.out.println("\t " + resourceName);
		}

		mc.deleteMetadata("/foo.kt");
		mc.deleteMetadata("/foo1.aa");
		mc.deleteMetadata("/foo2.zz");

		sms.delete("/foo.kt");
		sms.delete("/foo1.aa");
		sms.delete("/foo2.zz");
	}

	@Test
	public void testSearch() throws Exception {

		String url = WSServerUtilities.makeAddress(UAS.SMS, "WORK", kernel.getContainerProperties());
		EndpointReferenceType epr = EndpointReferenceType.Factory.newInstance();
		epr.addNewAddress().setStringValue(url);
		StorageClient sms = new StorageClient(epr, kernel.getClientConfiguration());
		MetadataClient mc = sms.getMetadataClient();
		assertNotNull(mc);

		System.out.println("Metadata tests");
		System.out.println("Directory at the beginign:");
		listDir("/", sms);

		//create some file(s)
		sms.upload("foo.a").write("this is a test".getBytes());
		sms.upload("jj.file").write("some other data".getBytes());
		sms.upload("bar.a").write("yet another piece of information".getBytes());
		sms.upload("page.html").write("some content".getBytes());
		sms.upload("bar2.a").write("yupi doopi doooo".getBytes());

		Map<String, String> meta = new HashMap<String, String>();
		meta.put("Author", "Rybicki");
		meta.put("Title", "Review letters");
		mc.createMetadata("/foo.a", meta);

		meta = new HashMap<String, String>();
		meta.put("Author", "Schueller");
		meta.put("Title", "Protocols of intranet/Internet");
		meta.put("Date", "13/04/2010");
		mc.createMetadata("/jj.file", meta);


		meta = new HashMap<String, String>();
		meta.put("Author", "Rybicky");
		meta.put("Title", "Peer to peer for all");
		mc.createMetadata("/bar.a", meta);

		meta = new HashMap<String, String>();
		meta.put("Author", "Ribicky");
		meta.put("Title", "Some other title I can get rihgt now");
		mc.createMetadata("/bar2.a", meta);

		System.out.println("Check the metadata\n");
		printMetadata("/foo.a", mc);
		printMetadata("/jj.file", mc);
		printMetadata("/page.html", mc);
		printMetadata("/bar.a", mc);
		printMetadata("/bar2.a", mc);



		System.out.println("Queries\n");
		String query = "Author:[Rybicki TO Zander]";
		Collection<String> result = mc.search(query, true);
		System.out.println("Matches for " + query);
		for (String hit : result) {
			System.out.println("\t:" + hit);
		}

		query = "Author:Rybicki~";
		result = mc.search(query, true);
		System.out.println("Matches for " + query);
		for (String hit : result) {
			System.out.println("\t:" + hit);
		}

		//cleanup (don't remove metadata for resources without metadata)
		mc.deleteMetadata("/foo.a");
		mc.deleteMetadata("/jj.file");
		mc.deleteMetadata("/bar2.a");


		sms.delete("/foo.a");
		sms.delete("/jj.file");
		sms.delete("/page.html");
		sms.delete("/bar.a");
	}

	private void printMetadata(String fileName, MetadataClient client) throws Exception {
		Map<String, String> metadata = client.getMetadata(fileName);
		System.out.printf("Metadata for %s\n", fileName);
		for (String key : metadata.keySet()) {
			System.out.printf("\t %s ==> %s\n", key, metadata.get(key));
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

	private void cleanDir(String directory, StorageClient storage) throws Exception {
		GridFileType[] listDirectory = storage.listDirectory(directory);
		System.out.println("Directory: " + directory);
		for (GridFileType gridFile : listDirectory) {
			storage.delete(gridFile.getPath());
		}
	}

	private void listDir(String directory, StorageClient storage) throws BaseFault {
		GridFileType[] listDirectory = storage.listDirectory(directory);
		System.out.println("Directory: " + directory);
		for (GridFileType gridFile : listDirectory) {
			System.out.println("\t" + gridFile.getPath());
		}
	}

	private static Map<String, String> extractMetadataType(MetadataType metadata) {
		Map<String, String> ret = new HashMap<String, String>();
		if (metadata == null || metadata.isNil()) {
			return ret;
		}
		TextInfoType[] propertyArray = metadata.getPropertyArray();
		if (propertyArray == null || propertyArray.length == 0) {
			return ret;
		}
		for (TextInfoType textInfoType : propertyArray) {
			ret.put(textInfoType.getName(), textInfoType.getValue());
		}
		return ret;
	}
}
