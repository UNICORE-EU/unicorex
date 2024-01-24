package eu.unicore.uas.fts.uftp;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import eu.unicore.services.Kernel;
import eu.unicore.uas.UAS;

/**
 * Tests the UFTP integration into UNICORE/X
 */
public class TestUFTPSetup {

	static UFTPDServerRunner uftpd1 = new UFTPDServerRunner(62434, 62435);
	
	static UFTPDServerRunner uftpd2 = new UFTPDServerRunner(62436, 62437);

	static Kernel kernel;
	
	protected static String getConfigPath() {
		return "src/test/resources/uas-uftpcluster.config";
	}

	@AfterClass
	public static void shutdown() throws Exception {
		uftpd1.stop();
		uftpd2.stop();
		if(kernel!=null)kernel.shutdown();
	}

	@BeforeClass
	public static void init() throws Exception {
		uftpd1.start();
		uftpd2.start();

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
		LogicalUFTPServer connector = new LogicalUFTPServer(kernel);
		kernel.setAttribute(LogicalUFTPServer.class, connector);
	}

	@Test
	public void test1() {
		LogicalUFTPServer connector = kernel.getAttribute(LogicalUFTPServer.class);
		System.out.println(connector.getStatusDescription());
		Assert.assertEquals(2, connector.getConfiguredServers());
	}
}
