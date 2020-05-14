package de.fzj.unicore.uas.testsuite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.unigrids.services.atomic.types.GridFileType;
import org.unigrids.services.atomic.types.MetadataType;
import org.unigrids.services.atomic.types.StatusType;
import org.unigrids.x2006.x04.services.metadata.ExtractionStatisticsDocument;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.Base;
import de.fzj.unicore.uas.StorageFactory;
import de.fzj.unicore.uas.client.MetadataClient;
import de.fzj.unicore.uas.client.StorageClient;
import de.fzj.unicore.uas.client.StorageFactoryClient;
import de.fzj.unicore.uas.client.TaskClient;
import de.fzj.unicore.uas.util.MockMetadataManager;
import de.fzj.unicore.wsrflite.ContainerProperties;
import de.fzj.unicore.wsrflite.xmlbeans.client.RegistryClient;
import de.fzj.unicore.wsrflite.xmlbeans.sg.Registry;
import eu.unicore.bugsreporter.annotation.FunctionalTest;

/**
 * runs some tests on the metadata support
 * 
 * @author schuller
 */
public class TestMetadata extends Base {

	@FunctionalTest(id="MetadataTest", description="Tests basic metadata support.")
	@Test
	public void testMetadata()throws Exception{
		String url=kernel.getContainerProperties().getValue(ContainerProperties.WSRF_BASEURL);
		EndpointReferenceType epr=EndpointReferenceType.Factory.newInstance();
		epr.addNewAddress().setStringValue(url+"/"+Registry.REGISTRY_SERVICE+"?res=default_registry");
		RegistryClient reg=new RegistryClient(epr,kernel.getClientConfiguration());
		//find a StorageFactory
		List<EndpointReferenceType> smf=reg.listServices(StorageFactory.SMF_PORT);
		assertTrue(smf!=null && smf.size()>0);
		EndpointReferenceType smfepr = findFirstAccessibleService(smf);
		assertTrue(smfepr !=null);
		System.out.println("Using StorageFactory at "+smfepr .getAddress().getStringValue());

		StorageFactoryClient smfClient=new StorageFactoryClient(smfepr ,kernel.getClientConfiguration());
		StorageClient sms=smfClient.createSMS();
		
		System.out.println(sms.getResourcePropertiesDocument());

		//check for metadata support
		assertTrue(sms.supportsMetadata());

		//stage in a file
		String in = "this is a test";
		ByteArrayInputStream bis=new ByteArrayInputStream(in.getBytes());
		String md5=MockMetadataManager.computeMD5(bis);
		sms.upload("foo").write(in.getBytes());

		//get metadata client
		MetadataClient mc=sms.getMetadataClient();
		assertTrue(mc!=null);

		//fill some metadata and see we can retrieve them
		Map<String,String>metadata=new HashMap<String,String>();
		metadata.put("ham","spam");
		metadata.put("test","123");
		mc.createMetadata("/foo", metadata);

		//check md is attached to listProperties() result
		GridFileType file=sms.listProperties("foo");
		assertTrue(file!=null);
		MetadataType meta=file.getMetadata();
		assertTrue(meta!=null);
		assertTrue(md5.equals(meta.getContentMD5()));

		//get
		Map<String,String>map=mc.getMetadata("/foo");
		assertTrue(map.get("ham").equals("spam"));

		//update
		map.put("newkey", "newvalue");
		mc.updateMetadata("/foo", map);
		map=mc.getMetadata("/foo");
		assertTrue("newvalue".equals(map.get("newkey")));

		//delete
		mc.deleteMetadata("/foo");
		map=mc.getMetadata("/foo");
		assertTrue(0==map.size());

		//create
		map.put("test","test-value");
		mc.createMetadata("/foo", map);
		map=mc.getMetadata("/foo");
		assertTrue(0<map.size());
		assertTrue("test-value".equals(map.get("test")));

		//search
		Collection<String>results=mc.search("foo", false);
		assertEquals(1,results.size());
		assertTrue(results.contains("foo"));

		//federated search
		TaskClient searchTask=mc.federatedMetadataSearch("foo", new String[0], false);
		while( StatusType.SUCCESSFUL != searchTask.getStatus() && StatusType.FAILED != searchTask.getStatus() ){
			Thread.sleep(2000);
		}
		System.out.println("Search task : "+searchTask.getStatus());
		System.out.println("Search results : "+searchTask.getResult());

		//extraction
		TaskClient tc=mc.startMetadataExtraction("/", 4321);
		while( StatusType.SUCCESSFUL != tc.getStatus() && StatusType.FAILED != tc.getStatus() ){
			Thread.sleep(2000);
		}
		System.out.println("Extraction task : "+tc.getStatus());
		ExtractionStatisticsDocument es=ExtractionStatisticsDocument.Factory.parse(tc.getResult().newInputStream());
		int docs=es.getExtractionStatistics().getDocumentsProcessed().intValue();
		assertTrue(docs>0);

		//extraction 2
		tc=mc.startMetadataExtraction(Arrays.asList(new String[]{"foo"}),null);
		while( StatusType.SUCCESSFUL != tc.getStatus() && StatusType.FAILED != tc.getStatus() ){
			Thread.sleep(2000);
		}
		System.out.println("Extraction task 2: "+tc.getStatus());
		es=ExtractionStatisticsDocument.Factory.parse(tc.getResult().newInputStream());
		docs=es.getExtractionStatistics().getDocumentsProcessed().intValue();
		assertEquals(1,docs);
	}

}
