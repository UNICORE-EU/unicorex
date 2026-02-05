package eu.unicore.uas.fts.uftp;
	
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import eu.unicore.services.Kernel;
import eu.unicore.uas.UAS;
import eu.unicore.uas.UASProperties;

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

	@AfterAll
	public static void shutdown() throws Exception {
		uftpd1.stop();
		uftpd2.stop();
		if(kernel!=null)kernel.shutdown();
	}

	@BeforeAll
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
		kernel = uas.getKernel();
		makePathsAbsolute();
		uas.startSynchronous();
		System.out.println("Startup time: "
				+ (System.currentTimeMillis() - start) + " ms.");
		
		new UFTPStartupTask(kernel).run();
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
	public void test1() {
		LogicalUFTPServer connector = kernel.getAttribute(LogicalUFTPServer.class);
		System.out.println(connector.getStatusDescription());
		assertEquals(2, connector.getConfiguredServers());
	}
}
