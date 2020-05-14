package de.fzj.unicore.uas.testsuite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.unigrids.services.atomic.types.GridFileType;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.Base;
import de.fzj.unicore.uas.StorageFactory;
import de.fzj.unicore.uas.client.BaseClientWithStatus;
import de.fzj.unicore.uas.client.StorageClient;
import de.fzj.unicore.uas.client.StorageFactoryClient;
import de.fzj.unicore.wsrflite.ContainerProperties;
import de.fzj.unicore.wsrflite.xmlbeans.client.RegistryClient;
import de.fzj.unicore.wsrflite.xmlbeans.sg.Registry;
import eu.unicore.bugsreporter.annotation.FunctionalTest;

public class TestStorageFactory extends Base {
	String url;
	EndpointReferenceType epr;

	@Test
	@FunctionalTest(id="StorageFactoryTest", description="Tests StorageFactory service")
	public void testStorageFactory()throws Exception{
		url=kernel.getContainerProperties().getValue(ContainerProperties.WSRF_BASEURL);
		epr=EndpointReferenceType.Factory.newInstance();
		epr.addNewAddress().setStringValue(url+"/"+Registry.REGISTRY_SERVICE+"?res=default_registry");
		RegistryClient reg=new RegistryClient(epr,kernel.getClientConfiguration());
		//find a StorageFactory
		List<EndpointReferenceType> tsfs=reg.listServices(StorageFactory.SMF_PORT);
		assertTrue(tsfs!=null && tsfs.size()>0);
		EndpointReferenceType tsfepr=findFirstAccessibleService(tsfs);
		assertTrue(tsfepr!=null);
		System.out.println("Using StorageFactory at "+tsfepr.getAddress().getStringValue());

		StorageFactoryClient smf=new StorageFactoryClient(tsfepr,kernel.getClientConfiguration());
		System.out.println(smf.getResourcePropertyDocument());
		checkBasic(smf);
		checkCreateWithCustomPath(smf);
		checkCreateWithCustomName(smf);
		checkCreateWithCustomType(smf);
	}

	private void checkBasic(StorageFactoryClient smf)throws Exception{
		System.out.println("Storage factory at "+smf.getUrl());
		waitUntilReady(smf);
		int n=smf.getStorages().size();
		assertEquals("Should have no accessible storage",0,n);

		StorageClient sms=smf.createSMS();
		smf.setUpdateInterval(-1);
		
		List<EndpointReferenceType>st=smf.getStorages();
		assertTrue(st!=null);

		n=st.size();
		assertEquals(1,n);

		checkNewStorage(sms);

		//now destroy the sms and check if the factory properties are OK
		sms.destroy();
		n=smf.getStorages().size();
		assertEquals(0,n);
	}

	private void checkCreateWithCustomName(StorageFactoryClient smf)throws Exception{
		String name="MyNewStorage";
		StorageClient sms=smf.createSMS(null, name, null);
		String finalName = sms.getStorageName();
		assertEquals(name, finalName);
	}

	private void checkCreateWithCustomType(StorageFactoryClient smf)throws Exception{
		String name="MyNewStorage";
		String type="TEST";

		StorageClient sms=smf.createSMS(type, name, null);
		assertTrue(name.equals(sms.getStorageName()));
	}

	private void checkCreateWithCustomPath(StorageFactoryClient smf)throws Exception{
		String dir = new File(".").getAbsolutePath()+"/target/smfdir-"+System.currentTimeMillis();
		String name="MyNewStorage";
		String type="TEST";
		Map<String,String>settings = new HashMap<>();
		settings.put("path", dir);
		StorageClient sms = smf.createSMS(type, name, settings, null);
		String mountPoint = sms.getFileSystem().getMountPoint();
		System.out.println("Mount point: "+mountPoint);
		assertEquals(new File(dir).getAbsolutePath(),new File(mountPoint).getAbsolutePath());
	}
	
	private void checkNewStorage(StorageClient sms)throws Exception{
		// file system RP
		System.out.println(sms.getFileSystem());
		//check if the created SMS is OK...
		GridFileType[] files=sms.listDirectory("/");
		//should be empty
		assertTrue(files.length==0);
		//upload some data
		sms.upload("test").write("some testdata".getBytes());
		//and download it again
		ByteArrayOutputStream os=new ByteArrayOutputStream();
		sms.download("test").readAllData(os);
		assertTrue("some testdata".equals(os.toString()));
		files=sms.listDirectory("/");
		assertTrue(1==files.length);
	}

	private void waitUntilReady(BaseClientWithStatus client)throws Exception{
		int c=0;
		String lastStatus=null;
		while(c<10){
			String s=client.getServiceStatus();
			if(!s.equals(lastStatus)){
				lastStatus=s;
				System.out.println("Service status is: "+s);	
			}
			Thread.sleep(1000);
			c++;
			if("READY".equals(s))break;
		}
	}
}
