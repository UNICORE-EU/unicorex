package de.fzj.unicore.uas.fts.uftp;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.UASProperties;
import de.fzj.unicore.uas.client.UFTPConstants;
import de.fzj.unicore.uas.fts.FiletransferOptions;
import de.fzj.unicore.wsrflite.Kernel;
import eu.unicore.bugsreporter.annotation.FunctionalTest;
import eu.unicore.client.Endpoint;
import eu.unicore.client.core.FileList.FileListEntry;
import eu.unicore.client.core.JobClient;
import eu.unicore.client.core.SiteClient;
import eu.unicore.client.core.SiteFactoryClient;
import eu.unicore.client.core.StorageClient;
import eu.unicore.client.core.StorageFactoryClient;
import eu.unicore.client.data.FiletransferClient;
import eu.unicore.client.data.HttpFileTransferClient;
import eu.unicore.client.data.UFTPFileTransferClient;
import eu.unicore.uftp.dpc.Utils;
import junit.framework.Assert;

/**
 * Tests the RESTFul implementation of UFTP file transfer
 */
public class TestRESTUFTP {

	static UFTPDServerRunner uftpd = new UFTPDServerRunner();
	
	static StorageClient sms;
	static SiteClient tss;

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
		Endpoint ep = new Endpoint("http://localhost:65321/rest/core/storagefactories/default_storage_factory");
		StorageFactoryClient smf = new StorageFactoryClient(ep, kernel.getClientConfiguration(), null);
		sms = smf.createStorage();
		importTestFile(sms, "test", 16384);
		Endpoint ep1 = new Endpoint("http://localhost:65321/rest/core/factories/default_target_system_factory");
		SiteFactoryClient tsf = new SiteFactoryClient(ep1, kernel.getClientConfiguration(), null);
		tss = tsf.getOrCreateSite();
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
		
		JobClient jc = tss.submitJob(getStageInJob());
		while(!jc.isFinished()) {
			Thread.sleep(1000);
		}
		FileListEntry result = jc.getWorkingDirectory().stat(
				"test-staged-in");
		Assert.assertEquals(16384, result.size);
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
		JobClient jc = tss.submitJob(getStageOutJob());
		
		// import a file
		StorageClient uspace=jc.getWorkingDirectory();
		importTestFile(uspace, "stage-out-file", 16384);
		jc.start();
		while(!jc.isFinished()) {
			Thread.sleep(1000);
		}
		System.out.println(jc.getProperties().toString(2));
		FileListEntry result = sms.stat("test-staged-out");
		Assert.assertEquals(16384, result.size);
		cfg.setProperty(UFTPProperties.PARAM_ENABLE_ENCRYPTION, "false");
		String orig = Utils.md5(new File(uspace.getMountPoint(),"stage-out-file"));
		String exported = Utils.md5(new File(sms.getMountPoint(),"test-staged-out"));
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
		
		Map<String,String> ep = new HashMap<>();
		ep.put(UFTPConstants.PARAM_SECRET, UUID.randomUUID().toString());
		ep.put(UFTPConstants.PARAM_CLIENT_HOST, "localhost");
		ep.put(UFTPConstants.PARAM_ENABLE_COMPRESSION, String.valueOf(compress));
		ep.put(UFTPConstants.PARAM_ENABLE_ENCRYPTION, String.valueOf(encrypt));
		
		FiletransferClient ftc = sms.createImport("test-import",false, -1, "UFTP", ep);
		Assert.assertNotNull(ftc);
		System.out.println(ftc.getProperties());
		
		File testFile = new File("target/testfiles/data-"+System.currentTimeMillis());
		int size = 1024;
		int n = 100;
		makeTestFile(testFile, size, n);
		try(InputStream source = new FileInputStream(testFile)) {
			((FiletransferOptions.Write)ftc).writeAllData(source);
		}
		
		Thread.sleep(1000);
		// check that file has been written...
		FileListEntry gft = sms.stat("test-import");
		Assert.assertNotNull(gft);
		Assert.assertEquals(size * n, gft.size);
		Assert.assertEquals(Utils.md5(testFile), Utils.md5(new File(sms.getMountPoint(),"test-import")));
	}
	
    @Test
   	public void testImportFileUsingStorageClientSessionMode() throws Exception {
   		Map<String,String>params=new HashMap<String,String>();
   		params.put(UFTPConstants.PARAM_SECRET, "test123");
   		params.put(UFTPConstants.PARAM_CLIENT_HOST, "localhost");
   		params.put(UFTPConstants.PARAM_USE_SESSION, "true");
   		
   		FiletransferClient ftc = sms.createImport("test-import", false, -1, "UFTP", params);
   		Assert.assertNotNull(ftc);
   		Assert.assertTrue(ftc instanceof UFTPFileTransferClient);
   		System.out.println(ftc.getProperties());
   		Boolean isSession = Boolean.parseBoolean(ftc.getProperties().getJSONObject("extraParameters").
   				getString(UFTPConstants.PARAM_USE_SESSION));
   		Assert.assertTrue(isSession);
   		
   		File testFile = new File("target/testfiles/data-"+System.currentTimeMillis());
   		int size = 1024;
   		int n = 100;
   		makeTestFile(testFile, size, n);
   		InputStream source = new FileInputStream(testFile);
   		try {
   			((UFTPFileTransferClient)ftc).writeAllData(source);//testFile.length());
   		} finally {
   			source.close();
   		}
   		
   		Logger.getLogger("").info("Finished.");
   		Thread.sleep(1000);
   		// check that file has been written...
   		FileListEntry gft = sms.stat("test-import");
   		Assert.assertNotNull(gft);
   		Assert.assertEquals(size * n, gft.size);
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
		Map<String,String> ep = new HashMap<>();
		ep.put(UFTPConstants.PARAM_SECRET, UUID.randomUUID().toString());
		ep.put(UFTPConstants.PARAM_CLIENT_HOST, "localhost");
		ep.put(UFTPConstants.PARAM_ENABLE_COMPRESSION, String.valueOf(compress));
		ep.put(UFTPConstants.PARAM_ENABLE_ENCRYPTION, String.valueOf(encrypt));
		
		// import first via BFT
		HttpFileTransferClient c = (HttpFileTransferClient)sms.createImport("export_test", false, -1, "BFT", null);
		File testFile = new File("target/testfiles/data-"
				+ System.currentTimeMillis());
		int size = 1024;
		int n = 100;
		makeTestFile(testFile, size, n);
		c.writeAllData(new FileInputStream(testFile));

		// now do export
		UFTPFileTransferClient c2 = (UFTPFileTransferClient)sms.createExport("export_test", "UFTP", ep);
		
		File testTarget = new File("target/testfiles/export-"
				+ System.currentTimeMillis());
		
		try (OutputStream target = new FileOutputStream(testTarget)){
			c2.readAllData(target);
		}
		Assert.assertTrue(testTarget.exists());
		Assert.assertEquals(size * n, testTarget.length());
		Assert.assertEquals(Utils.md5(testTarget), Utils.md5(testFile));
	}
	
	private JSONObject getStageInJob() throws JSONException {
		JSONObject jdd = new JSONObject();
		jdd.put("ApplicationName", "Date");
		JSONArray imports = new JSONArray();
		imports.put(new JSONObject("{"
				+ "From: 'UFTP:" + sms.getEndpoint().getUrl() + "/files/test',"
				+ "To: test-staged-in"
				+ "}"));
		jdd.put("Imports", imports);
		return jdd;
	}

	private JSONObject getStageOutJob() throws JSONException {
		JSONObject jdd = new JSONObject();
		jdd.put("ApplicationName", "Date");
		JSONArray xports = new JSONArray();
		xports.put(new JSONObject("{"
				+ "To: 'UFTP:" + sms.getEndpoint().getUrl() + "/files/test-staged-out',"
				+ "From: stage-out-file"
				+ "}"));
		jdd.put("Exports", xports);
		jdd.put("haveClientStageIn", true);
		return jdd;
	}

	private static void importTestFile(StorageClient sms, String filename,
			int size) throws Exception {
		byte[] buf = new byte[size];
		Random r = new Random();
		r.nextBytes(buf);
		FiletransferClient ft = sms.createImport(filename, false, -1, "BFT", null);
		((FiletransferOptions.Write)ft).writeAllData(new ByteArrayInputStream(buf));
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
