package eu.unicore.uas.fts.uftp;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Properties;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.FileList.FileListEntry;
import eu.unicore.client.core.StorageClient;
import eu.unicore.client.core.StorageFactoryClient;
import eu.unicore.client.data.TransferControllerClient;
import eu.unicore.services.Kernel;
import eu.unicore.uas.UAS;
import eu.unicore.uas.UASProperties;

/**
 * Tests the RESTFul implementation of UFTP server-to-server file transfer
 */
public class TestUFTPServerServerTransfer {

	static UFTPDServerRunner uftpd = new UFTPDServerRunner();
	
	static StorageClient sms1;
	static StorageClient sms2;

	static Kernel kernel;
	
	protected static String getConfigPath() {
		return "src/test/resources/uas.config";
	}

	@AfterAll
	public static void shutdown() throws Exception {
		uftpd.stop();
		kernel.shutdown();
	}

	@BeforeAll
	public static void init() throws Exception {
		
		uftpd.start();

		// start UNICORE
		long start = System.currentTimeMillis();
		// clear data directories
		FileUtils.deleteQuietly(new File("target", "data"));
		FileUtils.deleteQuietly(new File("target", "testfiles"));
		File f = new File("target/testfiles");
		if (!f.exists())f.mkdirs();
		UAS uas = new UAS(getConfigPath());
		uas.startSynchronous();
		System.out.println("Startup time: "
				+ (System.currentTimeMillis() - start) + " ms.");
		kernel = uas.getKernel();
		kernel.getAttribute(UASProperties.class).setProperty(UASProperties.SMS_TRANSFER_FORCEREMOTE, "true");
		Properties cfg = kernel.getContainerProperties().getRawProperties();
		cfg.setProperty("coreServices.uftp."+UFTPProperties.PARAM_CLIENT_LOCAL, "true");
		cfg.setProperty("coreServices.uftp."+UFTPProperties.PARAM_CLIENT_HOST, "localhost");
		cfg.setProperty("coreServices.uftp."+UFTPProperties.PARAM_SERVER_HOST, "localhost");
		cfg.setProperty("coreServices.uftp."+UFTPProperties.PARAM_SERVER_PORT, String.valueOf(uftpd.srvPort));
		cfg.setProperty("coreServices.uftp."+UFTPProperties.PARAM_COMMAND_PORT, String.valueOf(uftpd.jobPort));
		cfg.setProperty("coreServices.uftp."+UFTPProperties.PARAM_COMMAND_HOST, "localhost");
		cfg.setProperty("coreServices.uftp."+UFTPProperties.PARAM_COMMAND_SSL_DISABLE, "true");
		new UFTPStartupTask(kernel).run();
		// create a storage
		Endpoint ep = new Endpoint("http://localhost:65321/rest/core/storagefactories/default_storage_factory");
		StorageFactoryClient smf = new StorageFactoryClient(ep, kernel.getClientConfiguration(), null);
		sms1 = smf.createStorage();
		importTestFile(sms1, "test", 1024);
		sms2 = smf.createStorage();
	}

	@Test
	public void testSend() throws Exception {
		TransferControllerClient tcc = sms1.sendFile("test", 
				sms2.getEndpoint().getUrl()+"/files/test-sent", "UFTP");
		int c=0;
		while(!tcc.isComplete() && c<30) {
			Thread.sleep(3000);
			c++;
		}
		assertEquals(TransferControllerClient.Status.DONE, tcc.getStatus());
		FileListEntry result = sms2.stat("test-sent");
		assertEquals(1024, result.size);
	}
	
	@Test
	public void testReceive() throws Exception {
		TransferControllerClient tcc = sms2.fetchFile(sms1.getEndpoint().getUrl()+"/files/test",
				"test-received", "UFTP");
		int c=0;
		while(!tcc.isComplete() && c<30) {
			Thread.sleep(3000);
			c++;
		}
		assertEquals(TransferControllerClient.Status.DONE, tcc.getStatus());
		FileListEntry result = sms2.stat("test-received");
		assertEquals(1024, result.size);
	}

	private static void importTestFile(StorageClient sms, String filename,
			int size) throws Exception {
		byte[] buf = new byte[size];
		new Random().nextBytes(buf);
		sms.upload(filename).writeAllData(new ByteArrayInputStream(buf));
	}

}
