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
import org.unigrids.services.atomic.types.GridFileType;
import org.unigrids.services.atomic.types.ProtocolType;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.StorageFactory;
import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.UASProperties;
import de.fzj.unicore.uas.client.BaseUASClient;
import de.fzj.unicore.uas.client.FileTransferClient;
import de.fzj.unicore.uas.client.StorageClient;
import de.fzj.unicore.uas.client.StorageFactoryClient;
import eu.unicore.services.ContainerProperties;
import eu.unicore.services.Kernel;
import eu.unicore.services.ws.client.RegistryClient;
import eu.unicore.services.ws.sg.Registry;

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
	
	protected EndpointReferenceType findFirstAccessibleService(List<EndpointReferenceType>eprs){
		for(EndpointReferenceType epr: eprs){
			try{
				BaseUASClient c=new BaseUASClient(epr,uas.getKernel().getClientConfiguration());
				c.getCurrentTime();
				return epr;
			}catch(Exception e){}
		}
		return null;
	}
	
	Properties swiftProps; 

	public void testStorageFactory()throws Exception {
		swiftProps = loadSwiftProps();
		UASProperties uasProps = kernel.getAttribute(UASProperties.class);
		uasProps.setProperty("sms.factory.SWIFT.path",swiftProps.getProperty("path"));
		
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
		return smf.createSMS(type, "mySwift", settings, null);
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
