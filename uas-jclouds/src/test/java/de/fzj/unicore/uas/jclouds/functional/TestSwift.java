package de.fzj.unicore.uas.jclouds.functional;

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

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.UASProperties;
import eu.unicore.client.Endpoint;
import eu.unicore.client.core.FileList;
import eu.unicore.client.core.FileList.FileListEntry;
import eu.unicore.client.core.StorageClient;
import eu.unicore.client.core.StorageFactoryClient;
import eu.unicore.client.data.HttpFileTransferClient;
import eu.unicore.services.Kernel;

/**
 * this requires access to a Swift installation
 * 
 * @author schuller
 */
public class TestSwift {

	static final String configPath="src/test/resources/uas.config";
	
	public static void initDirectories() {
		//clear data directory
		FileUtils.deleteQuietly(new File("target","data"));
		File testsRoot = new File("target", "unicorex-test");
		FileUtils.deleteQuietly(testsRoot);
		testsRoot.mkdirs();
		new File(testsRoot, "smf-TEST").mkdir();
		new File(testsRoot, "storagefactory").mkdir();
		new File(testsRoot, "teststorage").mkdir();
	}
	
	protected static UAS uas;
	
	protected static Kernel kernel;
	
	@BeforeClass
	public static void startUNICORE() throws Exception{
		
		long start=System.currentTimeMillis();
		System.out.println("Starting UNICORE/X...");
		
		initDirectories();
		uas=new UAS(configPath);
		kernel=uas.getKernel();
		uas.startSynchronous();
		
		System.err.println("UNICORE/X startup time: "+(System.currentTimeMillis()-start)+" ms.");
		
	}

	@AfterClass
	public static void stopUNICORE() throws Exception{
		kernel.shutdown();
	}
	
	Properties swiftProps; 

	public void testStorageFactory()throws Exception {
		swiftProps = loadSwiftProps();
		UASProperties uasProps = kernel.getAttribute(UASProperties.class);
		uasProps.setProperty("sms.factory.SWIFT.path",swiftProps.getProperty("path"));
		
		String url = kernel.getContainerProperties().getContainerURL()+"/rest/core/storagefactories/default_storage_factory";
		StorageFactoryClient smf = new StorageFactoryClient(new Endpoint(url), kernel.getClientConfiguration(), null);
		StorageClient sms = createSMS(smf);
		checkNewStorage(sms);
		sms.delete();
	}

	private Properties loadSwiftProps()throws Exception{
		// read settings for local Swift server
		String settingsFile = System.getProperty("user.home")+"/.unicore/swift.settings";
		try (InputStream is = new FileInputStream(new File(settingsFile))){
			Properties swiftProps = new Properties();
			swiftProps.load(is);
			return swiftProps;
		}
	}

	private StorageClient createSMS(StorageFactoryClient smf)throws Exception{
		String type="SWIFT";
		Map<String,String> settings = new HashMap<>();
		settings.put("username",swiftProps.getProperty("username"));
		settings.put("password",swiftProps.getProperty("password"));
		settings.put("endpoint",swiftProps.getProperty("endpoint"));
		settings.put("region",swiftProps.getProperty("region"));
		return smf.createStorage(type, "mySwift", settings, null);
	}

	private void checkNewStorage(StorageClient sms)throws Exception{
		System.out.println(sms.getProperties().toString(2));
		//check if the created SMS is OK...
		FileList files=sms.ls("/");
		//should be empty
		assertTrue(files.list().size()==0);

		uploadDownloadCheck(sms);

		// check ls now
		List<FileListEntry> ls = files.list();
		assertTrue(ls.size()==1);
		System.out.println(ls.get(0));

		sms.mkdir("/testdir");
		ls = files.list();
		assertTrue(ls.size()==2);
		// rmdir
		sms.getFileClient("/testdir").delete();
		ls = files.list();
		assertTrue("Have "+ls.size(),ls.size()==1);
		
		// upload larger data set
		//byte[]data = new byte[1024*1024+1];
		//new Random().nextBytes(data);
		//sms.getImport("bigtest").write(data);
		//files=sms.listDirectory("/");
		//assertTrue(1==files.length);
	}

	private void uploadDownloadCheck(StorageClient sms) throws Exception {
		System.out.println("**** UPLOAD ****");
		String testdata = "some testdata";
		// upload some data
		HttpFileTransferClient ftc = sms.upload("test");
		ftc.writeAllData(new ByteArrayInputStream(testdata.getBytes()));
		// and download it again
		System.out.println("**** DOWNLOAD ****");
		ByteArrayOutputStream os=new ByteArrayOutputStream();
		ftc = sms.download("test");
		System.out.println(ftc.getProperties().toString(2));
		ftc.readAllData(os);
		assertTrue("Got: "+os.toString(),testdata.equals(os.toString()));
	}

}
