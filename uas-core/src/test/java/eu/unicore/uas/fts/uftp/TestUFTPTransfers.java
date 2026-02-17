package eu.unicore.uas.fts.uftp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.CoreClient;
import eu.unicore.client.core.FileList.FileListEntry;
import eu.unicore.client.core.JobClient;
import eu.unicore.client.core.SiteClient;
import eu.unicore.client.core.SiteFactoryClient;
import eu.unicore.client.core.StorageClient;
import eu.unicore.client.core.StorageFactoryClient;
import eu.unicore.client.data.FiletransferClient;
import eu.unicore.client.data.HttpFileTransferClient;
import eu.unicore.client.data.UFTPConstants;
import eu.unicore.client.data.UFTPFileTransferClient;
import eu.unicore.services.Kernel;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.services.restclient.UsernamePassword;
import eu.unicore.uas.UAS;
import eu.unicore.uas.UASProperties;
import eu.unicore.uftp.dpc.Utils;

/**
 * Tests the RESTFul implementation of UFTP file transfer
 */
public class TestUFTPTransfers {

	static UFTPDServerRunner uftpd = new UFTPDServerRunner();

	static StorageClient sms;
	static SiteClient tss;

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
		kernel = uas.getKernel();
		makePathsAbsolute();
		uas.startSynchronous();
		System.out.println("Startup time: "
				+ (System.currentTimeMillis() - start) + " ms.");
		kernel.getAttribute(UASProperties.class).setProperty(UASProperties.SMS_TRANSFER_FORCEREMOTE, "true");
		Properties cfg = kernel.getContainerProperties().getRawProperties();
		cfg.setProperty("coreServices.uftp."+UFTPProperties.PARAM_ENABLE_UFTP, "true");
		cfg.setProperty("coreServices.uftp."+UFTPProperties.PARAM_CLIENT_LOCAL, "true");
		cfg.setProperty("coreServices.uftp."+UFTPProperties.PARAM_CLIENT_HOST, "localhost");
		cfg.setProperty("coreServices.uftp."+UFTPProperties.PARAM_SERVER_HOST, "localhost");
		cfg.setProperty("coreServices.uftp."+UFTPProperties.PARAM_SERVER_PORT, String.valueOf(uftpd.srvPort));
		cfg.setProperty("coreServices.uftp."+UFTPProperties.PARAM_COMMAND_PORT, String.valueOf(uftpd.jobPort));
		cfg.setProperty("coreServices.uftp."+UFTPProperties.PARAM_COMMAND_HOST, "localhost");
		cfg.setProperty("coreServices.uftp."+UFTPProperties.PARAM_COMMAND_SSL_DISABLE, "true");
		new UFTPStartupTask(kernel).run();
		// create a storage
		String url = kernel.getContainerProperties().getContainerURL()+"/rest/core";
		Endpoint ep = new Endpoint(url+"/storagefactories/DEFAULT");
		StorageFactoryClient smf = new StorageFactoryClient(ep, kernel.getClientConfiguration(), getAuth());
		sms = smf.createStorage();
		importTestFile(sms, "test", 1024);
		for(int i=1;i<=3;i++)
		{
			importTestFile(sms, "/dir/test"+i, 1024);
		}
		Endpoint ep1 = new Endpoint(url+"/factories/default_target_system_factory");
		SiteFactoryClient tsf = new SiteFactoryClient(ep1, kernel.getClientConfiguration(), getAuth());
		tss = tsf.getOrCreateSite();
	}

	private static IAuthCallback getAuth() {
		return new UsernamePassword("demouser", "test123");
	}

	static void makePathsAbsolute() throws Exception{
		Properties props = kernel.getContainerProperties().getRawProperties();
		for(Object o: props.keySet()) {
			String key = String.valueOf(o);
			if(key.endsWith(".path") && (key.contains(".sms.") || key.contains(".storage."))) {
				String val = props.getProperty(key);
				val = new File(val).getAbsolutePath();
				props.setProperty(key, val);
			}
		}
		kernel.setAttribute(UASProperties.class, new UASProperties(props));
	}

	@Test
	public void testUFTPAvail() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest/core";
		Endpoint ep1 = new Endpoint(url);
		CoreClient cc = new CoreClient(ep1, kernel.getClientConfiguration(), null);
		JSONObject status = cc.getProperties();
		JSONObject ext = status.getJSONObject("server").getJSONObject("externalConnections");
		System.out.println(ext.toString(2));
		assertEquals("OK", ext.get("UFTPD localhost:62435"));
	}

	@Test
	public void testStageIn() throws Exception {
		doStageIn(false);
	}

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
		assertEquals(1024, result.size);
		cfg.setProperty(UFTPProperties.PARAM_ENABLE_ENCRYPTION, "false");
	}
	
	@Test
	public void testMultiStageIn() throws Exception {
		doMultiStageIn(false);
	}
	
	private void doMultiStageIn(boolean encrypt) throws Exception {
		UFTPProperties cfg = kernel.getAttribute(UFTPProperties.class);
		cfg.setProperty(UFTPProperties.PARAM_ENABLE_ENCRYPTION, Boolean.toString(encrypt));
		
		JobClient jc = tss.submitJob(getMultiStageInJob());
		while(!jc.isFinished()) {
			Thread.sleep(1000);
		}
		FileListEntry result = jc.getWorkingDirectory().stat(
				"dir/test1");
		assertEquals(1024, result.size);
		cfg.setProperty(UFTPProperties.PARAM_ENABLE_ENCRYPTION, "false");
	}

	@Test
	public void testStageOut() throws Exception {
		doStageOut(false);
	}

	@Test
	public void testStageOutEncrypt() throws Exception {
		doStageOut(true);
	}

	private void doStageOut(boolean encrypt) throws Exception {
		UFTPProperties cfg = kernel.getAttribute(UFTPProperties.class);
		cfg.setProperty(UFTPProperties.PARAM_ENABLE_ENCRYPTION, String.valueOf(encrypt));
		JobClient jc = tss.submitJob(getStageOutJob());
		StorageClient uspace=jc.getWorkingDirectory();
		while(uspace.getMountPoint()==null)Thread.sleep(1000);
		importTestFile(uspace, "stage-out-file", 1024);
		jc.start();
		while(!jc.isFinished()) {
			Thread.sleep(1000);
		}
		System.out.println(jc.getProperties().toString(2));
		FileListEntry result = sms.stat("test-staged-out");
		assertEquals(1024, result.size);
		cfg.setProperty(UFTPProperties.PARAM_ENABLE_ENCRYPTION, "false");
		String orig = Utils.md5(new File(uspace.getMountPoint(),"stage-out-file"));
		String exported = Utils.md5(new File(sms.getMountPoint(),"test-staged-out"));
		assertEquals(orig, exported);
	}
	
	@Test
	public void testMultiStageOut() throws Exception {
		doMultiStageOut(false);
	}

	private void doMultiStageOut(boolean encrypt) throws Exception {
		UFTPProperties cfg = kernel.getAttribute(UFTPProperties.class);
		cfg.setProperty(UFTPProperties.PARAM_ENABLE_ENCRYPTION, String.valueOf(encrypt));
		JobClient jc = tss.submitJob(getMultiStageOutJob());
		// import a file
		StorageClient uspace=jc.getWorkingDirectory();
		while(uspace.getMountPoint()==null)Thread.sleep(1000);
		for(int i=1;i<=3;i++)
		{
			importTestFile(uspace, "/out/test"+i, 1024);
		}
		jc.start();
		while(!jc.isFinished()) {
			Thread.sleep(1000);
		}
		System.out.println(jc.getProperties().toString(2));
		FileListEntry result = sms.stat("out/test1");
		assertEquals(1024, result.size);
		cfg.setProperty(UFTPProperties.PARAM_ENABLE_ENCRYPTION, "false");
		String orig = Utils.md5(new File(uspace.getMountPoint(),"out/test2"));
		String exported = Utils.md5(new File(sms.getMountPoint(),"out/test2"));
		assertEquals(orig, exported);
	}
	
	@Test
	public void testImportFile() throws Exception {
		doImportFile(false, false);
	}

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

		UFTPFileTransferClient ftc = (UFTPFileTransferClient)sms.createImport("test-import",false, -1, "UFTP", ep);
		assertNotNull(ftc);
		System.out.println(ftc.getProperties());

		File testFile = new File("target/testfiles/data-"+System.currentTimeMillis());
		int size = 1024;
		int n = 100;
		makeTestFile(testFile, size, n);
		try(InputStream source = new FileInputStream(testFile)) {
			ftc.write(source);
		}
		Thread.sleep(1000);
		// check that file has been written...
		FileListEntry gft = sms.stat("test-import");
		assertNotNull(gft);
		assertEquals(size * n, gft.size);
		assertEquals(gft.size, ftc.getTransferredBytes());
		assertEquals(Utils.md5(testFile), Utils.md5(new File(sms.getMountPoint(),"test-import")));
	}
	
    @Test
   	public void testImportFileUsingStorageClient() throws Exception {
   		Map<String,String>params=new HashMap<String,String>();
   		params.put(UFTPConstants.PARAM_SECRET, "test123");
   		params.put(UFTPConstants.PARAM_CLIENT_HOST, "localhost");
   		FiletransferClient ftc = sms.createImport("test-import", false, -1, "UFTP", params);
   		assertNotNull(ftc);
   		assertTrue(ftc instanceof UFTPFileTransferClient);
   		System.out.println(ftc.getProperties());
   		File testFile = new File("target/testfiles/data-"+System.currentTimeMillis());
   		int size = 1024;
   		int n = 100;
   		makeTestFile(testFile, size, n);
   		try (InputStream source = new FileInputStream(testFile)){
   			((UFTPFileTransferClient)ftc).write(source);//testFile.length());
   		}
   		Thread.sleep(1000);
   		// check that file has been written...
   		FileListEntry gft = sms.stat("test-import");
   		assertNotNull(gft);
   		assertEquals(size * n, gft.size);
   	}
	
	@Test
	public void testExportFile() throws Exception {
		doExportFile(false, false);
	}

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
		try(InputStream is = new FileInputStream(testFile)){
			c.write(is);
		}
		// now do export via UFTP
		UFTPFileTransferClient c2 = (UFTPFileTransferClient)sms.createExport("export_test", "UFTP", ep);
		File testTarget = new File("target/testfiles/export-" + System.currentTimeMillis());
		try (OutputStream target = new FileOutputStream(testTarget)){
			c2.readFully(target);
		}
		assertTrue(testTarget.exists());
		assertEquals(size * n, testTarget.length());
		assertEquals(Utils.md5(testTarget), Utils.md5(testFile));
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
	
	private JSONObject getMultiStageInJob() throws JSONException {
		JSONObject jdd = new JSONObject();
		jdd.put("ApplicationName", "Date");
		JSONArray imports = new JSONArray();
		imports.put(new JSONObject("{"
				+ "From: 'UFTP:" + sms.getEndpoint().getUrl() + "/files/dir/',"
				+ "To: 'dir/'"
				+ "}"));
		jdd.put("Imports", imports);
		return jdd;
	}

	private JSONObject getMultiStageOutJob() throws JSONException {
		JSONObject jdd = new JSONObject();
		jdd.put("ApplicationName", "Date");
		jdd.put("haveClientStageIn", "true");
		JSONArray exports = new JSONArray();
		JSONObject e = new JSONObject();
		e.put("From", "out/");
		e.put("To", "UFTP:" + sms.getEndpoint().getUrl() + "/files/out/");
		exports.put(e);
		jdd.put("Exports", exports);
		return jdd;
	}
	
	private static void importTestFile(StorageClient sms, String filename,
			int size) throws Exception {
		byte[] buf = new byte[size];
		new Random().nextBytes(buf);
		sms.upload(filename).write(new ByteArrayInputStream(buf));
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
