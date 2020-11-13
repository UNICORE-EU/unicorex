package de.fzj.unicore.uas.jclouds;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.StorageFactory;
import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.UASProperties;
import de.fzj.unicore.uas.client.BaseUASClient;
import de.fzj.unicore.uas.client.StorageFactoryClient;
import de.fzj.unicore.wsrflite.ContainerProperties;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.xmlbeans.client.RegistryClient;
import de.fzj.unicore.wsrflite.xmlbeans.sg.Registry;

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
	
	@BeforeClass
	public static void startUNICORE() throws Exception{
		
		long start=System.currentTimeMillis();
		System.out.println("Starting UNICORE/X...");
		
		initDirectories();
		uas=new UAS(configPath);
		kernel=uas.getKernel();
		
		//make default_SMS workdir absolute 
		UASProperties uasProperties = kernel.getAttribute(UASProperties.class); 
		String smsPath=uasProperties.getValue("defaultsms.path");
		if(smsPath!=null){
			uasProperties.setProperty("defaultsms.path", new File(smsPath).getAbsolutePath());
		}
		String smsWD=uasProperties.getValue("defaultsms.workdir");
		if(smsWD!=null){
			uasProperties.setProperty("defaultsms.workdir", new File(smsWD).getAbsolutePath());
		}
		
		uas.startSynchronous();
		
		System.err.println("UNICORE/X startup time: "+(System.currentTimeMillis()-start)+" ms.");
		
	}

	@AfterClass
	public static void stopUNICORE() throws Exception{
		kernel.shutdown();
	}
	
	protected StorageFactoryClient getStorageFactory() throws Exception {
		String url=kernel.getContainerProperties().getValue(ContainerProperties.WSRF_BASEURL);
		EndpointReferenceType epr=EndpointReferenceType.Factory.newInstance();
		epr.addNewAddress().setStringValue(url+"/"+Registry.REGISTRY_SERVICE+"?res=default_registry");
		RegistryClient reg=new RegistryClient(epr,kernel.getClientConfiguration());
		//find a StorageFactory
		List<EndpointReferenceType> eprs=reg.listServices(StorageFactory.SMF_PORT);
		assertTrue(eprs!=null && eprs.size()>0);
		EndpointReferenceType tsfepr=findFirstAccessibleService(eprs);
		assertTrue(tsfepr!=null);
		System.out.println("Using StorageFactory at "+tsfepr.getAddress().getStringValue());
		return new StorageFactoryClient(tsfepr,kernel.getClientConfiguration());
	}
	
	protected EndpointReferenceType findFirstAccessibleService(List<EndpointReferenceType>eprs){
		for(EndpointReferenceType epr: eprs){
			try{
				BaseUASClient c=new BaseUASClient(epr,uas.getKernel().getClientConfiguration());
				c.getCurrentTime();
				return epr;
			}catch(Exception e){}
		}
		return null;
	}
}
