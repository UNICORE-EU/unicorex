package de.fzj.unicore.uas;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.client.BaseUASClient;
import eu.unicore.services.Kernel;

/**
 * base class for functional tests. Starts a "clean" UNICORE/X server.
 */
public abstract class SecuredBase{

	static final String configPath="src/test/resources/secure/uas.config";
	
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
