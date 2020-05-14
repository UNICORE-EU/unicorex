package de.fzj.unicore.uas.jclouds.s3;

import static junit.framework.Assert.*;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.unigrids.services.atomic.types.ProtocolType;

import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.client.FileTransferClient;
import de.fzj.unicore.uas.client.StorageClient;
import de.fzj.unicore.uas.client.StorageFactoryClient;
import de.fzj.unicore.uas.jclouds.Base;
import de.fzj.unicore.wsrflite.xmlbeans.WSUtilities;
import de.fzj.unicore.xuudb.Log;

public class TestS3SMSFactory extends Base {

	@Test
	public void testCreateS3() throws Exception {
		StorageFactoryClient smf = getStorageFactory();
		StorageClient sms = smf.createSMS("S3","myS3",null);
		System.out.println(sms.getResourcePropertyDocument());
		sms.createDirectory("testing.unicore.eu");
		assertNotNull(sms.listProperties("testing.unicore.eu"));
		sms.createDirectory("testing.unicore.eu/data");
		assertNotNull(sms.listProperties("testing.unicore.eu/data"));
		System.out.println(Arrays.asList(sms.listDirectory("testing.unicore.eu")));
		System.out.println(sms.listProperties("testing.unicore.eu/data"));
		FileTransferClient ft = sms.getImport("testing.unicore.eu/data/testdata", ProtocolType.BFT);
		ft.writeAllData(new ByteArrayInputStream("some test data".getBytes()));
		ft.destroy();
		System.out.println(Arrays.asList(sms.listDirectory("testing.unicore.eu/")));
	}

	@Test
	public void testCreateS3WithParams() throws Exception {
		StorageFactoryClient smf = getStorageFactory();
		Map<String,String>params = new HashMap<String, String>();
		String accessKey = "test123";
		params.put("accessKey", accessKey);
		StorageClient sms = smf.createSMS("S3","my s3",params,null);
		String uid = WSUtilities.extractResourceID(sms.getUrl());
		// check the SMS model contains our parameters
		S3Model model = (S3Model)(kernel.getHome(UAS.SMS).get(uid).getModel());
		String effectiveAccessKey = model.getAccessKey();
		assertEquals(accessKey, effectiveAccessKey);
		
		// secret key should be the one from config
		String effectiveSecretKey = model.getSecretKey();
		assertEquals("site-default-secretkey", effectiveSecretKey);
		
		// try with custom endpoint : should fail because it is forbidden in config
		params = new HashMap<String, String>();
		String endpoint = "my_ep";
		params.put("endpoint", endpoint);
		try{
			smf.createSMS("S3","my s3",params,null);
		}catch(Exception ex){
			// OK
			System.out.println(Log.createFaultMessage("As expected", ex));
		}
	}
}
