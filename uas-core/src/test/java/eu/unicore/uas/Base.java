package eu.unicore.uas;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import eu.unicore.services.Kernel;

/**
 * base class for functional tests. Starts a "clean" UNICORE/X server.
 */
public abstract class Base{

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

	@BeforeAll
	public static void startUNICORE() throws Exception{
		long start = System.currentTimeMillis();
		System.out.println("Starting UNICORE/X...");
		initDirectories();
		uas = new UAS(configPath);
		kernel = uas.getKernel();
		uas.startSynchronous();
		System.err.println("UNICORE/X startup time: "+(System.currentTimeMillis()-start)+" ms.");
	}

	@AfterAll
	public static void stopUNICORE() throws Exception{
		kernel.shutdown();
	}

}
