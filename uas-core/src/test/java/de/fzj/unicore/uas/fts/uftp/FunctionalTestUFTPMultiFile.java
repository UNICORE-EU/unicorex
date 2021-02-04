package de.fzj.unicore.uas.fts.uftp;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Properties;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.CreationFlagEnumeration;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.DataStagingType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionDocument;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionType;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.unigrids.services.atomic.types.GridFileType;
import org.unigrids.x2006.x04.services.tss.SubmitDocument;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.UASProperties;
import de.fzj.unicore.uas.client.FileTransferClient;
import de.fzj.unicore.uas.client.JobClient;
import de.fzj.unicore.uas.client.StorageClient;
import de.fzj.unicore.uas.client.StorageFactoryClient;
import de.fzj.unicore.uas.client.TSFClient;
import de.fzj.unicore.uas.client.TSSClient;
import de.fzj.unicore.wsrflite.Kernel;
import eu.unicore.bugsreporter.annotation.FunctionalTest;

/**
 * Tests the UFTP 2.0 multi-file transfers in "non-local mode", i.e. 
 * using an external uftp.sh executable
 */
public class FunctionalTestUFTPMultiFile {

	static UFTPDServerRunner uftpd = new UFTPDServerRunner();
	
	static StorageClient sms;

	static TSSClient tss;

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

		// start UNICORE
		long start = System.currentTimeMillis();
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
		cfg.setProperty("coreServices.uftp."+UFTPProperties.PARAM_CLIENT_LOCAL, "false");
		cfg.setProperty("coreServices.uftp."+UFTPProperties.PARAM_CLIENT_EXECUTABLE, "/opt/unicore-servers/uftpd/bin/uftp.sh");
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
		EndpointReferenceType epr = EndpointReferenceType.Factory.newInstance();
		epr.addNewAddress().setStringValue("http://localhost:65321/services/StorageFactory?res=default_storage_factory");
		StorageFactoryClient smf = new StorageFactoryClient(epr, kernel.getClientConfiguration());
		sms = smf.createSMS();
		
		// setup test files for stage-in
		for(int i=1;i<=N_files;i++)
		{
			importTestFile(sms, "/test/test"+i, i*8192);
		}
		
		EndpointReferenceType epr2 = EndpointReferenceType.Factory.newInstance();
		epr2.addNewAddress().setStringValue(
						"http://localhost:65321/services/TargetSystemFactoryService?res=default_target_system_factory");
		TSFClient tsf = new TSFClient(epr2, kernel.getClientConfiguration());
		tss = tsf.createTSS();
	}

	@FunctionalTest(id = "testUFTPStageInMultiFile", description = "Tests multi-file staging in using UFTP")
	@Test
	public void testStageInMultiFile() throws Exception {
		doStageIn(false);
	}

	@FunctionalTest(id = "testUFTPStageInMultiFileEncrypt", description = "Tests multi-file staging in using UFTP with encrypted data")
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
		
		SubmitDocument in = SubmitDocument.Factory.newInstance();
		in.addNewSubmit().setJobDefinition(getStageInJob());
		JobClient jc = tss.submit(in);
		jc.waitUntilReady(0);//60000);
		jc.start();
		jc.waitUntilDone(15000);
		StorageClient uspace=jc.getUspaceClient();
		
		for(int i=1; i<=N_files; i++){
			GridFileType result = uspace.listProperties(
					"/dir/test"+i);
			Assert.assertEquals(i*8192, result.getSize());
		}
		cfg.setProperty(UFTPProperties.PARAM_ENABLE_ENCRYPTION, "false");
		
		// check the number of filetransfer instances after the stage-out
		long after=UFTPFileTransferImpl.instancesCreated.get();
		// we expect only a single filetransfer
		assertEquals("Wrong number of filetransfers", 1, after-before);
	}
	
	@FunctionalTest(id = "testUFTPStageOutMultiFile", description = "Tests multifile file stage out using UFTP")
	@Test
	public void testStageOutMultiFile() throws Exception {
		doStageOut(false);
	}

	@FunctionalTest(id = "testUFTPStageOutEncryptMultiFile", description = "Tests multifile stage out using UFTP with encrypted data")
	@Test
	@Ignore
	public void testStageOutEncryptMultiFile() throws Exception {
		doStageOut(true);
	}

	private void doStageOut(boolean encrypt) throws Exception {
		UFTPProperties cfg = kernel.getAttribute(UFTPProperties.class);
		cfg.setProperty(UFTPProperties.PARAM_ENABLE_ENCRYPTION, String.valueOf(encrypt));
		SubmitDocument in = SubmitDocument.Factory.newInstance();
		in.addNewSubmit().setJobDefinition(getStageOutJob());
		JobClient jc = tss.submit(in);
		jc.waitUntilReady(3000);

		StorageClient uspace=jc.getUspaceClient();
		// setup test files for stage-out
		for(int i=1;i<=N_files;i++)
		{
			importTestFile(uspace, "/out/test"+i, i*8192);
		}
		jc.start();
		jc.waitUntilDone(0);
		for(int i=1;i<=N_files;i++)
		{
			GridFileType result = sms.listProperties("out/test"+i);
			Assert.assertEquals(i*8192, result.getSize());
		}
		cfg.setProperty(UFTPProperties.PARAM_ENABLE_ENCRYPTION, "false");
	}
	
	private JobDefinitionType getStageInJob() {
		JobDefinitionDocument jdd = JobDefinitionDocument.Factory.newInstance();
		jdd.addNewJobDefinition().addNewJobDescription().addNewApplication()
				.setApplicationName("Date");
		DataStagingType dst = jdd.getJobDefinition().getJobDescription()
				.addNewDataStaging();
		dst.addNewSource().setURI("UFTP:" + sms.getUrl() + "#/test/");
		dst.setFileName("dir/");
		dst.setCreationFlag(CreationFlagEnumeration.OVERWRITE);
		return jdd.getJobDefinition();
	}

	private JobDefinitionType getStageOutJob() {
		JobDefinitionDocument jdd = JobDefinitionDocument.Factory.newInstance();
		jdd.addNewJobDefinition().addNewJobDescription().addNewApplication()
				.setApplicationName("Date");
		DataStagingType dst = jdd.getJobDefinition().getJobDescription()
				.addNewDataStaging();
		dst.setFileName("out/");
		dst.addNewTarget().setURI("UFTP:" + sms.getUrl() + "#/out");
		dst.setCreationFlag(CreationFlagEnumeration.OVERWRITE);
		return jdd.getJobDefinition();
	}

	private static void importTestFile(StorageClient sms, String filename,
			int size) throws Exception {
		byte[] buf = new byte[size];
		Random r = new Random();
		r.nextBytes(buf);
		try(FileTransferClient ft = sms.upload(filename)){
			ft.write(buf);
		}
	}
}
