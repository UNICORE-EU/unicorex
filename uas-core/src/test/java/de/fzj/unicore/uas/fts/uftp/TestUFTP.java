package de.fzj.unicore.uas.fts.uftp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.CreationFlagEnumeration;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.DataStagingType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionType;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.unigrids.services.atomic.types.GridFileType;
import org.unigrids.services.atomic.types.ProtocolType;
import org.unigrids.x2006.x04.services.tss.SubmitDocument;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.UASProperties;
import de.fzj.unicore.uas.client.FileTransferClient;
import de.fzj.unicore.uas.client.HttpFileTransferClient;
import de.fzj.unicore.uas.client.JobClient;
import de.fzj.unicore.uas.client.StorageClient;
import de.fzj.unicore.uas.client.StorageFactoryClient;
import de.fzj.unicore.uas.client.TSFClient;
import de.fzj.unicore.uas.client.TSSClient;
import de.fzj.unicore.uas.client.TransferControllerClient;
import de.fzj.unicore.uas.client.UFTPConstants;
import de.fzj.unicore.uas.client.UFTPFileTransferClient;
import de.fzj.unicore.wsrflite.Kernel;
import eu.unicore.bugsreporter.annotation.FunctionalTest;
import eu.unicore.uftp.dpc.Utils;
import junit.framework.Assert;

/**
 * Tests the UFTP integration into UNICORE/X
 */
public class TestUFTP {

	static UFTPDServerRunner uftpd = new UFTPDServerRunner();
	
	static StorageClient sms;

	static TSSClient tss;

	static Kernel kernel;
	
	protected static String getConfigPath() {
		return "src/test/resources/uas.config";
	}

	@AfterClass
	public static void shutdown() throws Exception {
		uftpd.stop();
		kernel.shutdown();
	}

	@BeforeClass
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
		UFTPProperties cfg = kernel.getAttribute(UFTPProperties.class);
		cfg.setProperty(UFTPProperties.PARAM_CLIENT_LOCAL, "true");
		cfg.setProperty(UFTPProperties.PARAM_CLIENT_HOST, "localhost");
		cfg.setProperty(UFTPProperties.PARAM_SERVER_HOST, "localhost");
		cfg.setProperty(UFTPProperties.PARAM_SERVER_PORT, "" + uftpd.srvPort);
		cfg.setProperty(UFTPProperties.PARAM_COMMAND_PORT, "" + uftpd.jobPort);
		cfg.setProperty(UFTPProperties.PARAM_COMMAND_HOST, "localhost");
		cfg.setProperty(UFTPProperties.PARAM_COMMAND_SSL_DISABLE, "true");
		// create a storage
		EndpointReferenceType epr = EndpointReferenceType.Factory.newInstance();
		epr.addNewAddress().setStringValue("http://localhost:65321/services/StorageFactory?res=default_storage_factory");
		StorageFactoryClient smf = new StorageFactoryClient(epr, kernel.getClientConfiguration());
		sms = smf.createSMS();
		importTestFile(sms, "test", 16384);
		EndpointReferenceType epr2 = EndpointReferenceType.Factory.newInstance();
		epr2.addNewAddress().setStringValue(
						"http://localhost:65321/services/TargetSystemFactoryService?res=default_target_system_factory");
		TSFClient tsf = new TSFClient(epr2, kernel.getClientConfiguration());
		tss = tsf.createTSS();
	}

	@FunctionalTest(id = "testUFTPStageIn", description = "Tests file staging in using UFTP")
	@Test
	public void testStageIn() throws Exception {
		doStageIn(false);
	}

	@FunctionalTest(id = "testUFTPStageInEncrypt", description = "Tests file staging in using UFTP with encrypted data")
	@Test
	public void testStageInEncrypt() throws Exception {
		doStageIn(true);
	}

	private void doStageIn(boolean encrypt) throws Exception {
		UFTPProperties cfg = kernel.getAttribute(UFTPProperties.class);
		cfg.setProperty(UFTPProperties.PARAM_ENABLE_ENCRYPTION, Boolean.toString(encrypt));
		SubmitDocument in = SubmitDocument.Factory.newInstance();
		in.addNewSubmit().setJobDefinition(getStageInJob());
		JobClient jc = tss.submit(in);
		jc.waitUntilReady(60000);
		jc.start();
		jc.waitUntilDone(15000);
		GridFileType result = jc.getUspaceClient().listProperties(
				"test-staged-in");
		Assert.assertEquals(16384, result.getSize());
		cfg.setProperty(UFTPProperties.PARAM_ENABLE_ENCRYPTION, "false");
	}
	
	@FunctionalTest(id = "testUFTPStageOut", description = "Tests file stage out using UFTP")
	@Test
	public void testStageOut() throws Exception {
		doStageOut(false);
	}

	@FunctionalTest(id = "testUFTPStageOutEncrypt", description = "Tests file stage out using UFTP with encrypted data")
	@Test
	public void testStageOutEncrypt() throws Exception {
		doStageOut(true);
	}
	
	private void doStageOut(boolean encrypt) throws Exception {
		UFTPProperties cfg = kernel.getAttribute(UFTPProperties.class);
		cfg.setProperty(UFTPProperties.PARAM_ENABLE_ENCRYPTION, String.valueOf(encrypt));
		SubmitDocument in = SubmitDocument.Factory.newInstance();
		in.addNewSubmit().setJobDefinition(getStageOutJob());
		JobClient jc = tss.submit(in);
		jc.waitUntilReady(3000);
		// import a file
		StorageClient uspace=jc.getUspaceClient();
		importTestFile(uspace, "stage-out-file", 16384);
		jc.start();
		jc.waitUntilDone(15000);
		GridFileType result = sms.listProperties("test-staged-out");
		Assert.assertEquals(16384, result.getSize());
		cfg.setProperty(UFTPProperties.PARAM_ENABLE_ENCRYPTION, "false");
		String orig = Utils.md5(new File(uspace.getFileSystem().getMountPoint(),"stage-out-file"));
		String exported = Utils.md5(new File(sms.getFileSystem().getMountPoint(),"test-staged-out"));
		Assert.assertEquals(orig, exported);
	}
	
	@FunctionalTest(id = "testUFTPImportFile", description = "Tests file import using UFTP")
	@Test
	public void testImportFile() throws Exception {
		doImportFile(false, false);
	}

	@FunctionalTest(id = "testUFTPImportFileEncrypted", description = "Tests file import using UFTP with data encryption")
	@Test
	public void testImportFileEncrypt() throws Exception {
		doImportFile(true, false);
	}

	private void doImportFile(boolean encrypt, boolean compress) throws Exception {
		String secret = "test123";
		UFTPFileTransferClient ftc = UFTPFileTransferClient.createImport(
				"test-import", sms, false, "localhost", 2, secret, encrypt, compress);
		Assert.assertNotNull(ftc);
		String rpDoc=ftc.getResourcePropertyDocument();
		System.out.println(rpDoc);
		assertEquals(encrypt,rpDoc.contains(UFTPConstants.PARAM_ENCRYPTION_KEY));
		assertTrue(rpDoc.contains(UFTPConstants.PARAM_ENABLE_COMPRESSION));
		File testFile = new File("target/testfiles/data-"+System.currentTimeMillis());
		int size = 1024;
		int n = 100;
		makeTestFile(testFile, size, n);
		InputStream source = new FileInputStream(testFile);
		try {
			ftc.writeAllData(source);
		} finally {
			source.close();
		}
		
		Logger.getLogger("").info("Finished.");
		Thread.sleep(1000);
		// check that file has been written...
		GridFileType gft = sms.listProperties("test-import");
		Assert.assertNotNull(gft);
		Assert.assertEquals(size * n, gft.getSize());
		Assert.assertEquals(Utils.md5(testFile), Utils.md5(new File(sms.getFileSystem().getMountPoint(),"test-import")));
	}
	
	
    @Test
	public void testImportFileUsingStorageClient() throws Exception {
		Map<String,String>params=new HashMap<String,String>();
		params.put(UFTPConstants.PARAM_SECRET, "test123");
		params.put(UFTPConstants.PARAM_CLIENT_HOST, "localhost");
		
		FileTransferClient ftc = sms.getImport("test-import", false, params, ProtocolType.UFTP);
		Assert.assertTrue(ftc instanceof UFTPFileTransferClient);
		
		Assert.assertNotNull(ftc);
		String rpDoc=ftc.getResourcePropertyDocument();
		
		assertEquals(false,rpDoc.contains(UFTPConstants.PARAM_ENCRYPTION_KEY));
		File testFile = new File("target/testfiles/data-"+System.currentTimeMillis());
		int size = 1024;
		int n = 100;
		makeTestFile(testFile, size, n);
		InputStream source = new FileInputStream(testFile);
		try {
			ftc.writeAllData(source);
		} finally {
			source.close();
		}
		
		Logger.getLogger("").info("Finished.");
		Thread.sleep(1000);
		// check that file has been written...
		GridFileType gft = sms.listProperties("test-import");
		Assert.assertNotNull(gft);
		Assert.assertEquals(size * n, gft.getSize());
	}
    
    @Test
   	public void testImportFileUsingStorageClientSessionMode() throws Exception {
   		Map<String,String>params=new HashMap<String,String>();
   		params.put(UFTPConstants.PARAM_SECRET, "test123");
   		params.put(UFTPConstants.PARAM_CLIENT_HOST, "localhost");
   		params.put(UFTPConstants.PARAM_USE_SESSION, "true");
   		
   		FileTransferClient ftc = sms.getImport("test-import", false, params, ProtocolType.UFTP);
   		Assert.assertNotNull(ftc);
   		Assert.assertTrue(ftc instanceof UFTPFileTransferClient);
   		System.out.println(ftc.getResourcePropertiesDocument());
   		Boolean isSession = Boolean.parseBoolean(ftc.getProtocolDependentRPs().get(UFTPConstants.PARAM_USE_SESSION));
   		Assert.assertTrue(isSession);
   		String rpDoc=ftc.getResourcePropertyDocument();
   		
   		assertEquals(false,rpDoc.contains(UFTPConstants.PARAM_ENCRYPTION_KEY));
   		File testFile = new File("target/testfiles/data-"+System.currentTimeMillis());
   		int size = 1024;
   		int n = 100;
   		makeTestFile(testFile, size, n);
   		InputStream source = new FileInputStream(testFile);
   		try {
   			ftc.writeAllData(source);
   		} finally {
   			source.close();
   		}
   		
   		Logger.getLogger("").info("Finished.");
   		Thread.sleep(1000);
   		// check that file has been written...
   		GridFileType gft = sms.listProperties("test-import");
   		Assert.assertNotNull(gft);
   		Assert.assertEquals(size * n, gft.getSize());
   	}
	
	@FunctionalTest(id = "testUFTPExportFile", description = "Tests file export using UFTP")
	@Test
	public void testExportFile() throws Exception {
		doExportFile(false, false);
	}

	

	@FunctionalTest(id = "testUFTPExportFileEncrypt", description = "Tests file export using UFTP with encrypted data")
	@Test
	public void testExportFileEncrypt() throws Exception {
		doExportFile(true, false);
	}

	private void doExportFile(boolean encrypt, boolean compress) throws Exception {
		String secret = "abc_TEST_123";
		// import first via BFT
		HttpFileTransferClient c = (HttpFileTransferClient) sms.getImport(
				"export_test", ProtocolType.BFT);
		File testFile = new File("target/testfiles/data-"
				+ System.currentTimeMillis());
		int size = 1024;
		int n = 100;
		makeTestFile(testFile, size, n);
		c.writeAllData(new FileInputStream(testFile));

		// now do export
		UFTPFileTransferClient ftc = UFTPFileTransferClient.createExport(
				"export_test", sms, "localhost", 1, secret, encrypt, compress);
		Assert.assertNotNull(ftc);
		String rpDoc=ftc.getResourcePropertyDocument();
		assertEquals(encrypt,rpDoc.contains(UFTPConstants.PARAM_ENCRYPTION_KEY));
		File testTarget = new File("target/testfiles/export-"
				+ System.currentTimeMillis());
		OutputStream target = new FileOutputStream(testTarget);
		try {
			ftc.readAllData(target);
		} finally {
			target.close();
		}
		Assert.assertTrue(testTarget.exists());
		Assert.assertEquals(size * n, testTarget.length());
		Assert.assertEquals(Utils.md5(testTarget), Utils.md5(testFile));
	}
	
	@FunctionalTest(id = "testUFTPServerServer", description = "Tests server-server copy using UFTP")
	@Test
	public void testServerServer() throws Exception {
		doReceiveFile(false, false);
	}
	
	@FunctionalTest(id = "testUFTPServerServerEncrypt", description = "Tests server-server copy using UFTP with data encryption")
	@Test
	public void testServerServerEncrypt() throws Exception {
		doReceiveFile(true, false);
	}
	
	private void doReceiveFile(boolean encrypt, boolean compress) throws Exception {
		UFTPProperties cfg = kernel.getAttribute(UFTPProperties.class);
		cfg.setProperty(UFTPProperties.PARAM_ENABLE_ENCRYPTION, String.valueOf(encrypt));
		SubmitDocument in = SubmitDocument.Factory.newInstance();
		in.addNewSubmit().setJobDefinition(getSimpleJob());
		JobClient jc = tss.submit(in);
		jc.waitUntilReady(3000);
		StorageClient uspace=jc.getUspaceClient();
		importTestFile(uspace, "file-to-copy", 16384);
		jc.start();
		jc.waitUntilDone(15000);
		String source = "UFTP:"+uspace.getUrl()+"#/file-to-copy";
		Map<String,String>extraParameters = new HashMap<>();
		extraParameters.put("uftp.encryption", String.valueOf(encrypt));
		TransferControllerClient tcc = sms.fetchFile(source, "copied-file", extraParameters);
		tcc.setUpdateInterval(500);
		while(!tcc.isComplete()){
			Thread.sleep(1000);
		}
		
		GridFileType result = sms.listProperties("copied-file");
		Assert.assertEquals(16384, result.getSize());
		cfg.setProperty(UFTPProperties.PARAM_ENABLE_ENCRYPTION, "false");
		String orig = Utils.md5(new File(uspace.getFileSystem().getMountPoint(),"file-to-copy"));
		String copy = Utils.md5(new File(sms.getFileSystem().getMountPoint(),"copied-file"));
		Assert.assertEquals(orig, copy);
	}
	
	private JobDefinitionType getStageInJob() {
		JobDefinitionDocument jdd = JobDefinitionDocument.Factory.newInstance();
		jdd.addNewJobDefinition().addNewJobDescription().addNewApplication()
				.setApplicationName("Date");
		DataStagingType dst = jdd.getJobDefinition().getJobDescription()
				.addNewDataStaging();
		dst.addNewSource().setURI("UFTP:" + sms.getUrl() + "#/test");
		dst.setFileName("test-staged-in");
		dst.setCreationFlag(CreationFlagEnumeration.OVERWRITE);
		return jdd.getJobDefinition();
	}

	private JobDefinitionType getStageOutJob() {
		JobDefinitionDocument jdd = JobDefinitionDocument.Factory.newInstance();
		jdd.addNewJobDefinition().addNewJobDescription().addNewApplication()
				.setApplicationName("Date");
		DataStagingType dst = jdd.getJobDefinition().getJobDescription()
				.addNewDataStaging();
		dst.setFileName("stage-out-file");
		dst.addNewTarget().setURI("UFTP:" + sms.getUrl() + "#/test-staged-out");
		dst.setCreationFlag(CreationFlagEnumeration.OVERWRITE);
		return jdd.getJobDefinition();
	}
	
	private JobDefinitionType getSimpleJob() {
		JobDefinitionDocument jdd = JobDefinitionDocument.Factory.newInstance();
		jdd.addNewJobDefinition().addNewJobDescription();
		return jdd.getJobDefinition();
	}


	private static void importTestFile(StorageClient sms, String filename,
			int size) throws Exception {
		byte[] buf = new byte[size];
		Random r = new Random();
		r.nextBytes(buf);
		try (FileTransferClient ft = sms.upload(filename)){
			ft.write(buf);
		}
	}

	private static void makeTestFile(File file, int chunkSize, int chunks)
			throws IOException {
		
		try(FileOutputStream fos = new FileOutputStream(file)){
			Random r = new Random();
			byte[] buf = new byte[chunkSize];
			for (int i = 0; i < chunks; i++) {
				r.nextBytes(buf);
				fos.write(buf);
			}
		}
	}
}
