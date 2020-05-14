package de.fzj.unicore.uas.testsuite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.xml.namespace.QName;

import org.ggf.baseprofile.FinalWSResourceInterfaceDocument;
import org.ggf.baseprofile.ResourcePropertyNamesDocument;
import org.junit.Test;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.Base;
import de.fzj.unicore.uas.TargetSystemFactory;
import de.fzj.unicore.uas.client.BaseUASClient;
import de.fzj.unicore.uas.impl.bp.BPSupportImpl;
import de.fzj.unicore.wsrflite.ContainerProperties;
import de.fzj.unicore.wsrflite.xmlbeans.client.RegistryClient;
import de.fzj.unicore.wsrflite.xmlbeans.sg.Registry;
import eu.unicore.bugsreporter.annotation.FunctionalTest;

/**
 * tests of the OGSA Baseprofile support
 */
public class BPSupportTest extends Base{

	String url;
	EndpointReferenceType registryEpr;

	@Test
	@SuppressWarnings("unchecked")
	@FunctionalTest(id="BPupportTest", 
					description="Tests the OGSA Base profile support")
	public void testBPSupport()throws Exception{
		url=kernel.getContainerProperties().getValue(ContainerProperties.WSRF_BASEURL);
		registryEpr=EndpointReferenceType.Factory.newInstance();
		registryEpr.addNewAddress().setStringValue(url+"/"+Registry.REGISTRY_SERVICE+"?res=default_registry");
		RegistryClient reg=new RegistryClient(registryEpr,kernel.getClientConfiguration());
		List<EndpointReferenceType> eprs=reg.listServices(TargetSystemFactory.TSF_PORT);
		assertEquals(1,eprs.size());
		eprs=reg.listServices(TargetSystemFactory.TSF_PORT);
		for(EndpointReferenceType epr: eprs){
			BaseUASClient client=new BaseUASClient(epr,kernel.getClientConfiguration());
			String s=client.getResourceProperty(BPSupportImpl.RPResourcePropertyNames);
			ResourcePropertyNamesDocument nd=ResourcePropertyNamesDocument.Factory.parse(s);
			List<QName>list=nd.getResourcePropertyNames();
			for(QName q: list){
				assertTrue(client.getResourceProperty(q)!=null);
			}
			s=client.getResourceProperty(BPSupportImpl.RPFinalWSResourceInterface);
			FinalWSResourceInterfaceDocument d=FinalWSResourceInterfaceDocument.Factory.parse(s);
			assertTrue(d.getFinalWSResourceInterface().equals(TargetSystemFactory.TSF_PORT));
		}

	}

}
