package de.fzj.unicore.uas;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.client.BaseUASClient;
import de.fzj.unicore.uas.impl.sms.InitSharedStorages;
import de.fzj.unicore.wsrflite.Kernel;

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
	
	@BeforeClass
	public static void startUNICORE() throws Exception{
		
		long start=System.currentTimeMillis();
		System.out.println("Starting UNICORE/X...");
		
		initDirectories();
		uas=new UAS(configPath);
		kernel=uas.getKernel();
		
		uas.startSynchronous();
		
		System.err.println("UNICORE/X startup time: "+(System.currentTimeMillis()-start)+" ms.");
		
		//create a shared SMS
		new InitSharedStorages(kernel).run();
	}

	@AfterClass
	public static void stopUNICORE() throws Exception{
		kernel.shutdown();
	}
	
	protected EndpointReferenceType findFirstAccessibleService(List<EndpointReferenceType>eprs){
		for(EndpointReferenceType epr: eprs){
			try{
				BaseUASClient c=new BaseUASClient(epr,uas.getKernel().getClientConfiguration());
				c.getCurrentTime();
				return epr;
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		return null;
	}
}
