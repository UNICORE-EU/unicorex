package de.fzj.unicore.uas.fts.uftp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Properties;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.UASProperties;
import eu.unicore.services.Kernel;
import eu.unicore.client.Endpoint;
import eu.unicore.client.Job;
import eu.unicore.client.core.CoreClient;
import eu.unicore.client.core.FileList.FileListEntry;
import eu.unicore.client.core.JobClient;
import eu.unicore.client.core.SiteClient;
import eu.unicore.client.core.StorageClient;
import eu.unicore.client.core.StorageFactoryClient;
import eu.unicore.client.data.HttpFileTransferClient;
import eu.unicore.services.rest.client.UsernamePassword;

/**
 * Tests the UFTP 2.0 multi-file transfers in "non-local mode", i.e. 
 * using an external uftp.sh executable
 */
public class TestUFTPNonLocalMode {

	static UFTPDServerRunner uftpd = new UFTPDServerRunner();
	
	static StorageClient sms;

	static SiteClient tss;

	static Kernel kernel;
	
	static int N_files=3;
	
	protected static String getConfigPath() {
		return "src/test/resources/uas.config";
	}

	@AfterClass
	public static void shutdown() throws Exception {
		uftpd.stop();
		if(kernel!=null)kernel.shutdown();
	}

	@BeforeClass
	public static void init() throws Exception {
		uftpd.start();
		FileUtils.writeStringToFile(new File("target/uftp.classpath"), System.getProperty("java.class.path"), "UTF-8");
		// start UNICORE
		FileUtils.deleteQuietly(new File("target", "data"));
		FileUtils.deleteQuietly(new File("target", "testfiles"));
		File f = new File("target/testfiles");
		if (!f.exists())f.mkdirs();
		UAS uas = new UAS(getConfigPath());
		uas.startSynchronous();
		kernel = uas.getKernel();
		kernel.getAttribute(UASProperties.class).setProperty(UASProperties.SMS_TRANSFER_FORCEREMOTE, "true");
		Properties cfg = kernel.getContainerProperties().getRawProperties();
		cfg.setProperty("coreServices.uftp."+UFTPProperties.PARAM_CLIENT_LOCAL, "false");
		File p = new File("src/test/resources/uftp/uftp.sh");
		cfg.setProperty("coreServices.uftp."+UFTPProperties.PARAM_CLIENT_EXECUTABLE, p.getAbsolutePath());
		cfg.setProperty("coreServices.uftp."+UFTPProperties.PARAM_CLIENT_HOST, "localhost");
		cfg.setProperty("coreServices.uftp."+UFTPProperties.PARAM_SERVER_HOST, "localhost");
		cfg.setProperty("coreServices.uftp."+UFTPProperties.PARAM_SERVER_PORT, String.valueOf(uftpd.srvPort));
		cfg.setProperty("coreServices.uftp."+UFTPProperties.PARAM_COMMAND_PORT, String.valueOf(uftpd.jobPort));
		cfg.setProperty("coreServices.uftp."+UFTPProperties.PARAM_COMMAND_HOST, "localhost");
		cfg.setProperty("coreServices.uftp."+UFTPProperties.PARAM_COMMAND_SSL_DISABLE, "true");
		UFTPProperties uProps = new UFTPProperties(cfg);
		kernel.setAttribute(UFTPProperties.class, uProps);
		LogicalUFTPServer connector = new LogicalUFTPServer(kernel);
		kernel.setAttribute(LogicalUFTPServer.class, connector);
		// create a storage
		Endpoint ep = new Endpoint("http://localhost:65321/rest/core/storagefactories/default_storage_factory");
		StorageFactoryClient smf = new StorageFactoryClient(ep,
				kernel.getClientConfiguration(),
				new UsernamePassword("demouser", "test123"));
		sms = smf.createStorage();
		
		// setup test files for stage-in
		for(int i=1;i<=N_files;i++)
		{
			importTestFile(sms, "/test/test"+i, i*8192);
		}
		CoreClient cc = new CoreClient(new Endpoint("http://localhost:65321/rest/core"), 
				kernel.getClientConfiguration(),
				new UsernamePassword("demouser", "test123"));
		tss = cc.getSiteClient();
	}

	@Test
	public void testStageInMultiFile() throws Exception {
		doStageIn(false);
	}

	@Test
	@Ignore
	public void testStageInMultiFileEncrypt() throws Exception {
		doStageIn(true);
	}

	private void doStageIn(boolean encrypt) throws Exception {
		UFTPProperties cfg = kernel.getAttribute(UFTPProperties.class);
		cfg.setProperty(UFTPProperties.PARAM_ENABLE_ENCRYPTION, Boolean.toString(encrypt));
		
		// note the current number of filetransfer instances
		long before=UFTPFileTransferImpl.instancesCreated.get();
		
		JobClient jc = tss.submitJob(getStageInJob());
		int c = 0;
		while(!jc.isFinished()&& c<30) {
			Thread.sleep(1000);
			c++;
		}
		assertTrue(jc.getLog().toString(), jc.isFinished());
		
		StorageClient uspace=jc.getWorkingDirectory();
		
		for(int i=1; i<=N_files; i++){
			FileListEntry result = uspace.stat(
					"/dir/test"+i);
			Assert.assertEquals(i*8192, result.size);
		}
		cfg.setProperty(UFTPProperties.PARAM_ENABLE_ENCRYPTION, "false");
		
		// check the number of filetransfer instances after the stage-out
		long after=UFTPFileTransferImpl.instancesCreated.get();
		// we expect only a single filetransfer
		assertEquals("Wrong number of filetransfers", 1, after-before);
	}
	
	@Test
	public void testStageOutMultiFile() throws Exception {
		doStageOut(false);
	}

	@Test
	@Ignore
	public void testStageOutEncryptMultiFile() throws Exception {
		doStageOut(true);
	}

	private void doStageOut(boolean encrypt) throws Exception {
		UFTPProperties cfg = kernel.getAttribute(UFTPProperties.class);
		cfg.setProperty(UFTPProperties.PARAM_ENABLE_ENCRYPTION, String.valueOf(encrypt));
		JobClient jc = tss.submitJob(getStageOutJob());
		
		StorageClient uspace=jc.getWorkingDirectory();
		// setup test files for stage-out
		for(int i=1;i<=N_files;i++)
		{
			importTestFile(uspace, "/out/test"+i, i*8192);
		}
		jc.start();
		int c = 0;
		while(!jc.isFinished()&& c<300) {
			Thread.sleep(1000);
			c++;
		}
		assertTrue(jc.getLog().toString(), jc.isFinished());
		
		for(int i=1;i<=N_files;i++)
		{
			FileListEntry result = sms.stat("out/test"+i);
			Assert.assertEquals("Wrong size"+result, i*8192, result.size);
		}
		cfg.setProperty(UFTPProperties.PARAM_ENABLE_ENCRYPTION, "false");
	}
	
	private JSONObject getStageInJob() throws JSONException {
		Job jdd = new Job();
		jdd.application("Date");
		jdd.stagein().from("UFTP:"+sms.getEndpoint().getUrl()+"/files/test/").to("dir/");
		return jdd.getJSON();
	}

	private JSONObject getStageOutJob() throws JSONException {
		Job jdd = new Job();
		jdd.application("Date");
		jdd.stageout().from("out/").to("UFTP:"+sms.getEndpoint().getUrl()+"/files/out/");
		jdd.wait_for_client_stage_in();
		return jdd.getJSON();
	}

	private static void importTestFile(StorageClient sms, String filename,
			int size) throws Exception {
		byte[] buf = new byte[size];
		Random r = new Random();
		r.nextBytes(buf);
		HttpFileTransferClient ft = sms.upload(filename);
		ft.write(buf);
		ft.delete();
	}
}
