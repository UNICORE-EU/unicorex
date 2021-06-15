package de.fzj.unicore.client.functional.cdmi;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.unigrids.services.atomic.types.GridFileType;
import org.unigrids.services.atomic.types.ProtocolType;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.Base;
import de.fzj.unicore.uas.StorageFactory;
import de.fzj.unicore.uas.UASProperties;
import de.fzj.unicore.uas.cdmi.CDMIClient;
import de.fzj.unicore.uas.cdmi.KeystoneAuth;
import de.fzj.unicore.uas.client.FileTransferClient;
import de.fzj.unicore.uas.client.StorageClient;
import de.fzj.unicore.uas.client.StorageFactoryClient;
import eu.emi.security.authn.x509.helpers.BinaryCertChainValidator;
import eu.unicore.services.ContainerProperties;
import eu.unicore.services.rest.client.BaseClient;
import eu.unicore.services.ws.client.RegistryClient;
import eu.unicore.services.ws.sg.Registry;
import eu.unicore.util.httpclient.ClientProperties;

public class CDMITest extends Base {

	Properties cdmiProps; 

	@Test
	public void testStorageFactory()throws Exception {
		cdmiProps = loadCDMIProps();
		String tokenEndpoint = cdmiProps.getProperty("tokenEndpoint");
		String username = cdmiProps.getProperty("username");
		String password = cdmiProps.getProperty("password");
		UASProperties uasProps = kernel.getAttribute(UASProperties.class);
		uasProps.setProperty("sms.factory.CDMI.path",cdmiProps.getProperty("path"));
		testCreateToken(tokenEndpoint, username, password);
		String url = kernel.getContainerProperties().getValue(ContainerProperties.EXTERNAL_URL);
		EndpointReferenceType epr = EndpointReferenceType.Factory.newInstance();
		epr.addNewAddress().setStringValue(url+"/services/"+Registry.REGISTRY_SERVICE+"?res=default_registry");
		RegistryClient reg=new RegistryClient(epr,kernel.getClientConfiguration());
		//find a StorageFactory
		List<EndpointReferenceType> tsfs = reg.listServices(StorageFactory.SMF_PORT);
		assertTrue(tsfs!=null && tsfs.size()>0);
		EndpointReferenceType factory = findFirstAccessibleService(tsfs);
		assertTrue(factory != null);
		System.out.println("Using StorageFactory at "+factory.getAddress().getStringValue());
		StorageFactoryClient smf=new StorageFactoryClient(factory,kernel.getClientConfiguration());
		StorageClient sms = createSMS(smf);
		checkNewStorage(sms);
		sms.destroy();
	}

	private void testCreateToken(String endpoint, String username, String password) throws Exception {
		KeystoneAuth kauth = new KeystoneAuth(endpoint, username, password, kernel);
		String token = kauth.getToken();
		System.out.println("Obtained Keystone token: "+token);
		assertTrue(token.length()>0);
		testAccess(kauth);
	}

	private void testAccess(KeystoneAuth kauth)throws Exception {
		// small test whether we can access...
		String cdmiEndpoint = cdmiProps.getProperty("endpoint");
		String path = cdmiProps.getProperty("path");
		ClientProperties cp = kernel.getClientConfiguration().clone();
		cp.setSslAuthn(false);
		cp.setSslEnabled(cdmiEndpoint.startsWith("https"));
		/* disable server cert verification */		
		cp.setValidator(new BinaryCertChainValidator(true));
		BaseClient client = new BaseClient(cdmiEndpoint, cp, null);
		client.setAuthCallback(kauth);
		String url = cdmiEndpoint+path;
		client.setURL(url);
		HttpResponse res = client.get(CDMIClient.CDMI_CONTAINER);
		if(res.getStatusLine().getStatusCode() != 200){
			System.out.println(res.getStatusLine());
		}
		System.out.println("*** "+url);
		System.out.println(EntityUtils.toString(res.getEntity()));
	}

	private Properties loadCDMIProps()throws Exception{
		// read settings for local CDMI server
		String settingsFile = System.getProperty("user.home")+"/.unicore/cdmi.settings";
		try (InputStream is = new FileInputStream(new File(settingsFile))){
			Properties cdmiProps = new Properties();
			cdmiProps.load(is);
			return cdmiProps;
		}
	}

	private StorageClient createSMS(StorageFactoryClient smf)throws Exception{
		String type="CDMI";
		Map<String,String> settings = new HashMap<>();
		settings.put("username",cdmiProps.getProperty("username"));
		settings.put("password",cdmiProps.getProperty("password"));
		settings.put("endpoint",cdmiProps.getProperty("endpoint"));
		settings.put("tokenEndpoint",cdmiProps.getProperty("tokenEndpoint"));
		return smf.createSMS(type, "myCDMI", settings, null);
	}

	private void checkNewStorage(StorageClient sms)throws Exception{
		// file system RP
		System.out.println(sms.getFileSystem());
		//check if the created SMS is OK...
		GridFileType[] files=sms.listDirectory("/");
		//should be empty
		assertTrue(files.length==0);

		uploadDownloadCheck(sms, ProtocolType.BFT);

		// check ls now
		files=sms.listDirectory("/");
		assertTrue(files.length==1);
		System.out.println(files[0]);

		// mkdir
		sms.createDirectory("/testdir");
		files=sms.listDirectory("/");
		assertTrue(files.length==2);
		// rmdir
		sms.delete("/testdir");
		files=sms.listDirectory("/");
		assertTrue("Have "+files.length,files.length==1);
		
		// upload larger data set
		//byte[]data = new byte[1024*1024+1];
		//new Random().nextBytes(data);
		//sms.getImport("bigtest").write(data);
		//files=sms.listDirectory("/");
		//assertTrue(1==files.length);
	}

	private void uploadDownloadCheck(StorageClient sms, ProtocolType.Enum protocol) throws Exception {
		System.out.println("**** UPLOAD ****");
		String testdata = "some testdata";
		// upload some data
		FileTransferClient ftc = sms.getImport("test", protocol);
		ftc.writeAllData(new ByteArrayInputStream(testdata.getBytes()));
		// and download it again
		System.out.println("**** DOWNLOAD ****");
		ByteArrayOutputStream os=new ByteArrayOutputStream();
		ftc = sms.getExport("test", protocol);
		System.out.println(ftc.getResourcePropertyDocument());
		ftc.readAllData(os);
		assertTrue("Got: "+os.toString(),testdata.equals(os.toString()));
	}

}
