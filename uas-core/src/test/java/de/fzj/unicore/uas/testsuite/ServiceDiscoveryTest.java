package de.fzj.unicore.uas.testsuite;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.oasisOpen.docs.wsrf.sg2.EntryType;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.Base;
import de.fzj.unicore.uas.StorageManagement;
import de.fzj.unicore.uas.TargetSystem;
import de.fzj.unicore.uas.TargetSystemFactory;
import de.fzj.unicore.uas.client.BaseUASClient;
import de.fzj.unicore.wsrflite.ContainerProperties;
import de.fzj.unicore.wsrflite.xmlbeans.client.RegistryClient;
import de.fzj.unicore.wsrflite.xmlbeans.sg.Registry;
import eu.unicore.bugsreporter.annotation.FunctionalTest;

/**
 * tests for the registry "list services" function
 */
public class ServiceDiscoveryTest extends Base {

	String url;
	EndpointReferenceType registryEpr;

	@FunctionalTest(id="RunServiceDiscoveryTest", description="Tests service discovery using the registry")
	@Test
	public void testServiceDiscovery()throws Exception{
		url=kernel.getContainerProperties().getValue(ContainerProperties.WSRF_BASEURL);
		registryEpr=EndpointReferenceType.Factory.newInstance();
		registryEpr.addNewAddress().setStringValue(url+"/"+Registry.REGISTRY_SERVICE+"?res=default_registry");
		RegistryClient reg=new RegistryClient(registryEpr,kernel.getClientConfiguration());
		//list entries first
		List<EntryType>entries=reg.listEntries();
		assertTrue(entries!=null);
		assertTrue(entries.size()>0);

		//some more detailed checks
		List<EndpointReferenceType> eprs = reg.listServices(TargetSystemFactory.TSF_PORT);
		assertTrue(eprs.size()==1);

		eprs=reg.listServices(TargetSystemFactory.TSF_PORT, 
				new RegistryClient.ServiceListFilter(){
			public boolean accept(EntryType entry){
				return true;
			}
		});
		assertTrue(eprs.size()==1);

		eprs=reg.listServices(TargetSystemFactory.TSF_PORT, 
				new RegistryClient.ServiceListFilter(){
			public boolean accept(EntryType entry){
				return !entry.getContent().isNil();
			}
		});
		assertTrue(eprs.size()==1);


		eprs=reg.listServices(TargetSystemFactory.TSF_PORT);
		assertTrue(eprs.size()==1);

		eprs=reg.listAccessibleServices(TargetSystemFactory.TSF_PORT);
		assertTrue(eprs.size()==1);

		eprs=reg.listServices(TargetSystem.TSS_PORT);
		for(EndpointReferenceType epr: eprs){
			try{
				new BaseUASClient(epr,kernel.getClientConfiguration()).getResourceProperty(TargetSystem.RPName);
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}

		eprs=reg.listServices(StorageManagement.SMS_PORT);
		for(EndpointReferenceType epr: eprs){
			try{
				new BaseUASClient(epr,kernel.getClientConfiguration()).getResourceProperty(StorageManagement.RPProtocol);
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
	}


}
