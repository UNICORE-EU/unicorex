package de.fzj.unicore.uas.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.client.TaskClient;
import de.fzj.unicore.wsrflite.ContainerProperties;
import de.fzj.unicore.wsrflite.Home;
import de.fzj.unicore.wsrflite.Kernel;
import eu.unicore.bugsreporter.annotation.FunctionalTest;
import eu.unicore.services.ws.utils.WSServerUtilities;


public class TestAddressUpdate {
	private UAS uas;
	
	@FunctionalTest(id="testAddressUpdate",
		description="Tests that published EPRs change when the server URL is re-configured")
	@Test
	public void testAddressUpdate()throws Exception{
		uas=new UAS("src/test/resources/minimal/container.properties");
		uas.startSynchronous();
		Kernel kernel=uas.getKernel();
		
		Home taskHome=kernel.getHome(UAS.TASK);
		assertNotNull(taskHome);
		
		BaseInitParameters initParams = new BaseInitParameters();
		initParams.parentServiceName = "foo";
		initParams.parentUUID = "bar";
		String taskID=taskHome.createResource(initParams);
		EndpointReferenceType taskEPR=WSServerUtilities.makeEPR(UAS.TASK, taskID, kernel);
		System.out.println(taskEPR);
		TaskClient client=new TaskClient(taskEPR,kernel.getClientConfiguration());
		client.setUpdateInterval(-1);
		EndpointReferenceType submissionRef=client.getResourcePropertiesDocument().getTaskProperties().getSubmissionServiceReference();
		assertNotNull(submissionRef);
		String url=submissionRef.getAddress().getStringValue();
		assertTrue(url.toString().contains("localhost"));
		
		//OK, now change the base URL... and check it is actually changed
		kernel.getContainerProperties().setProperty(ContainerProperties.WSRF_BASEURL, "http://127.0.0.1:65321/services");
		submissionRef=client.getResourcePropertiesDocument().getTaskProperties().getSubmissionServiceReference();
		assertNotNull(submissionRef);
		url=submissionRef.getAddress().getStringValue();
		assertTrue(url, url.toString().contains("127.0.0.1"));
		
	}
	
	@After
	public void tearDown() {
		uas.getKernel().shutdown();
	}
}
