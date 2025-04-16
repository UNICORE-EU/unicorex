package eu.unicore.uas.jclouds.s3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import de.fzj.unicore.uas.UAS;
import eu.unicore.client.core.StorageClient;
import eu.unicore.client.core.StorageFactoryClient;
import eu.unicore.client.data.HttpFileTransferClient;
import eu.unicore.uas.Base;
import eu.unicore.util.Log;

public class TestS3SMSFactory extends Base {

	@Test
	public void testCreateS3() throws Exception {
		StorageFactoryClient smf = getStorageFactory();
		StorageClient sms = smf.createStorage("S3","myS3", null, null);
		System.out.println(sms.getProperties().toString(2));
		sms.mkdir("testing.unicore.eu");
		assertNotNull(sms.stat("testing.unicore.eu"));
		sms.mkdir("testing.unicore.eu/data");
		assertNotNull(sms.stat("testing.unicore.eu/data"));
		System.out.println(sms.ls("testing.unicore.eu"));
		System.out.println(sms.stat("testing.unicore.eu/data"));
		HttpFileTransferClient ft = sms.upload("testing.unicore.eu/data/testdata");
		ft.writeAllData(new ByteArrayInputStream("some test data".getBytes()));
		ft.delete();
		System.out.println(sms.ls("testing.unicore.eu"));
	}

	@Test
	public void testCreateS3WithParams() throws Exception {
		StorageFactoryClient smf = getStorageFactory();
		Map<String,String>params = new HashMap<>();
		String accessKey = "test123";
		params.put("accessKey", accessKey);
		StorageClient sms = smf.createStorage("S3","my s3", params, null);
		String url = sms.getEndpoint().getUrl();
		String uid = url.substring(url.lastIndexOf("/")+1);
		System.out.println(uid);
		// check the SMS model contains our parameters
		S3Model model = (S3Model)(kernel.getHome(UAS.SMS).get(uid).getModel());
		String effectiveAccessKey = model.getAccessKey();
		assertEquals(accessKey, effectiveAccessKey);
		
		// secret key should be the one from config
		String effectiveSecretKey = model.getSecretKey();
		assertEquals("site-default-secretkey", effectiveSecretKey);
		
		// try with custom endpoint : should fail because it is forbidden in config
		params = new HashMap<>();
		String endpoint = "my_ep";
		params.put("endpoint", endpoint);
		try{
			smf.createStorage("S3", "my s3", params, null);
		}catch(Exception ex){
			// OK
			System.out.println(Log.createFaultMessage("As expected", ex));
		}
	}
}
