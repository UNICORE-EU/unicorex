package de.fzj.unicore.uas.testsuite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.ggf.schemas.jsdl.x2005.x11.jsdl.FileSystemType;
import org.junit.Test;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.Base;
import de.fzj.unicore.uas.StorageManagement;
import de.fzj.unicore.uas.client.StorageClient;
import de.fzj.unicore.wsrflite.ContainerProperties;
import de.fzj.unicore.wsrflite.xmlbeans.client.RegistryClient;
import de.fzj.unicore.wsrflite.xmlbeans.sg.Registry;
import eu.unicore.bugsreporter.annotation.FunctionalTest;

/**
 * shared SMS tests
 * @author schuller
 */
public class TestSharedSMS extends Base {

	@FunctionalTest(id="SharedSMSTest", description="Tests the shared storages (deployment etc)")
	@Test
	public void testSharedSMS()throws Exception{
		String url=kernel.getContainerProperties().getValue(ContainerProperties.WSRF_BASEURL);
		EndpointReferenceType registryEpr=EndpointReferenceType.Factory.newInstance();
		registryEpr.addNewAddress().setStringValue(url+"/"+Registry.REGISTRY_SERVICE+"?res=default_registry");
		RegistryClient reg=new RegistryClient(registryEpr,kernel.getClientConfiguration());
		List<EndpointReferenceType> smsEPRs = reg.listServices(StorageManagement.SMS_PORT);
		assertTrue(smsEPRs.size()>0);
		for(EndpointReferenceType epr: smsEPRs){
			String address = epr.getAddress().getStringValue();
			System.out.println("Have storage at "+address);
			StorageClient sms=new StorageClient(epr,kernel.getClientConfiguration());
			FileSystemType fst = sms.getFileSystem();
			assertNotNull(fst);
			if(address.endsWith("WORK")){
				System.out.println(sms.getResourcePropertiesDocument());
				assertTrue(fst.getMountPoint().contains("target/unicorex-test"));
				assertEquals("WORK",fst.getName());
			}
		}
	}

}
