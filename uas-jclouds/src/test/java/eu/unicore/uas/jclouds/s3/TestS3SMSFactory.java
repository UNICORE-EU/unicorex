package eu.unicore.uas.jclouds.s3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import eu.unicore.client.core.StorageClient;
import eu.unicore.client.core.StorageFactoryClient;
import eu.unicore.client.data.HttpFileTransferClient;
import eu.unicore.services.restclient.RESTException;
import eu.unicore.uas.Base;
import eu.unicore.uas.UAS;

public class TestS3SMSFactory extends Base {

	@Test
	public void testCreateS3() throws Exception {
		StorageFactoryClient smf = getStorageFactory("S3");
		Map<String,String>params = new HashMap<>();
		params.put("bucket", "testing.unicore.eu");
		StorageClient sms = smf.createStorage("myS3", params, null);
		System.out.println(sms.getProperties().toString(2));
		sms.mkdir("/data");
		assertNotNull(sms.stat("/data"));
		System.out.println(sms.ls("/"));
		System.out.println(sms.stat("/data"));
		byte[] data = "some test data".getBytes();
		HttpFileTransferClient ft = ((HttpFileTransferClient) sms.createImport("/data/testdata",
				false, data.length, "BFT", null));
		ft.write("some test data".getBytes());
		ft.delete();
		System.out.println(sms.ls("/"));
	}

	@Test
	public void testCreateS3WithParams() throws Exception {
		StorageFactoryClient smf = getStorageFactory("S3");
		JSONObject smfProps = smf.getProperties();
		System.out.println(smfProps.toString(2));
		for(String key: Arrays.asList("accessKey", "secretKey", "path", "bucket", "validate")) {
			assertTrue(smfProps.getJSONObject("parameters").has(key));
		}
		final Map<String,String>params = new HashMap<>();
		String accessKey = "test123";
		params.put("accessKey", accessKey);
		params.put("bucket", "test");
		StorageClient sms = smf.createStorage("my s3", params, null);
		String url = sms.getEndpoint().getUrl();
		String uid = url.substring(url.lastIndexOf("/")+1);
		// check the SMS model contains our parameters
		S3Model model = (S3Model)(kernel.getHome(UAS.SMS).get(uid).getModel());
		String effectiveAccessKey = model.getAccessKey();
		assertEquals(accessKey, effectiveAccessKey);
		
		// secret key should be the one from config
		String effectiveSecretKey = model.getSecretKey();
		assertEquals("site-default-secretkey", effectiveSecretKey);
		
		// try with custom endpoint : should fail because it is forbidden in config
		params.clear();
		String endpoint = "my_ep";
		params.put("endpoint", endpoint);
		assertThrows(RESTException.class, ()->smf.createStorage("my s3", params, null));
	}
}
