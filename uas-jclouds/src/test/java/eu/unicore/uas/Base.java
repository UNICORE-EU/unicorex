package eu.unicore.uas;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.StorageFactoryClient;
import eu.unicore.client.registry.RegistryClient;
import eu.unicore.services.ContainerProperties;
import eu.unicore.services.Kernel;
import eu.unicore.services.restclient.BaseClient;
import eu.unicore.services.restclient.IAuthCallback;
/**
 * base class for tests that need a running UNICORE/X server
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
		long start=System.currentTimeMillis();
		System.out.println("Starting UNICORE/X...");
		initDirectories();
		uas=new UAS(configPath);
		kernel=uas.getKernel();
		uas.startSynchronous();
		System.err.println("UNICORE/X startup time: "+(System.currentTimeMillis()-start)+" ms.");
	}

	@AfterAll
	public static void stopUNICORE() throws Exception{
		kernel.shutdown();
	}
	
	protected StorageFactoryClient getStorageFactory() throws Exception {
		String url=kernel.getContainerProperties().getValue(ContainerProperties.EXTERNAL_URL)
				+"/rest/registries/default_registry";
		IAuthCallback auth = null;
		RegistryClient reg = new RegistryClient(url, kernel.getClientConfiguration(), auth);
		//find a StorageFactory
		List<Endpoint> eps = reg.listEntries("StorageFactory");
		assertTrue(eps!=null && eps.size()>0);
		Endpoint smf = findFirstAccessibleService(eps);
		assertTrue(smf!=null);
		System.out.println("Using StorageFactory at "+smf.getUrl());
		return new StorageFactoryClient(smf, kernel.getClientConfiguration(), auth);
	}
	
	protected Endpoint findFirstAccessibleService(List<Endpoint>eps){
		for(Endpoint ep: eps){
			try{
				BaseClient c = new BaseClient(ep.getUrl(), uas.getKernel().getClientConfiguration(), null);
				c.getJSON();
				return ep;
			}catch(Exception e){}
		}
		return null;
	}
}
