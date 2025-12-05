package eu.unicore.uas.fts.uftp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Properties;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.CoreClient;
import eu.unicore.client.core.FileList.FileListEntry;
import eu.unicore.client.core.JobClient;
import eu.unicore.client.core.SiteClient;
import eu.unicore.client.core.SiteFactoryClient;
import eu.unicore.client.core.StorageClient;
import eu.unicore.client.core.StorageFactoryClient;
import eu.unicore.services.Kernel;
import eu.unicore.uas.UAS;
import eu.unicore.uas.UASProperties;
import eu.unicore.uftp.dpc.Utils;

/**
 * Tests the RESTFul implementation of UFTP file transfer
 */
@Disabled //  for now, due to very weird issue with the JDK FtpClient
public class TestUFTPTransfersTSI {

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
		uas.startSynchronous();
		System.out.println("Startup time: "
				+ (System.currentTimeMillis() - start) + " ms.");
		kernel = uas.getKernel();
		kernel.getAttribute(UASProperties.class).setProperty(UASProperties.SMS_TRANSFER_FORCEREMOTE, "true");
		Properties cfg = kernel.getContainerProperties().getRawProperties();
		cfg.setProperty("coreServices.uftp."+UFTPProperties.PARAM_ENABLE_UFTP, "true");
		cfg.setProperty("coreServices.uftp."+UFTPProperties.PARAM_CLIENT_LOCAL, "false");
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
		sms = smf.createStorage();
		importTestFile(sms, "test", 1024);
		for(int i=1;i<=3;i++)
		{
			importTestFile(sms, "/dir/test"+i, 1024);
		}
		Endpoint ep1 = new Endpoint("http://localhost:65321/rest/core/factories/default_target_system_factory");
		SiteFactoryClient tsf = new SiteFactoryClient(ep1, kernel.getClientConfiguration(), null);
		tss = tsf.getOrCreateSite();
	}

	@Test
	public void testUFTPAvail() throws Exception {
		Endpoint ep1 = new Endpoint("http://localhost:65321/rest/core");
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
	//@Disabled //  due to very weird issue with the JDK FtpClient
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
	@Disabled //  due to very weird issue with the JDK FtpClient
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

}
