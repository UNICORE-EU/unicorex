package de.fzj.unicore.uas.testsuite;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.xmlbeans.XmlObject;
import org.junit.Test;
import org.unigrids.services.atomic.types.StatusType;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.Base;
import de.fzj.unicore.uas.client.TaskClient;
import de.fzj.unicore.uas.impl.task.TaskImpl;
import de.fzj.unicore.wsrflite.ContainerProperties;
import de.fzj.unicore.wsrflite.Home;
import de.fzj.unicore.wsrflite.InitParameters;
import de.fzj.unicore.wsrflite.utils.Utilities;
import eu.unicore.bugsreporter.annotation.FunctionalTest;
import eu.unicore.services.ws.utils.WSServerUtilities;

public class TestTask extends Base{

	@FunctionalTest(id="TaskTest", description="Tests the Task service")
	@Test
	public void basicTest()throws Exception{
		String URL=kernel.getContainerProperties().getValue(ContainerProperties.WSRF_BASEURL)+"/Test?res=123";
		String uuid=createNewInstance();
		EndpointReferenceType taskEPR=WSServerUtilities.makeEPR("Task", uuid, kernel);
		TaskClient c=new TaskClient(taskEPR,kernel.getClientConfiguration());
		String parentUrl=c.getResourcePropertiesDocument().getTaskProperties().getSubmissionServiceReference().getAddress().getStringValue();
		assertTrue(parentUrl.equals(URL));
		assertTrue(c.getSubmissionTime()!=null);
		assertTrue(c.getStatus().equals(StatusType.RUNNING));
		try{
			c.cancel();
		}
		catch(Exception ex){
			ex.printStackTrace();
			fail(ex.toString());
		}
	}
	
	@Test
	public void createResultTest()throws Exception{
		String uuid=createNewInstance();
		EndpointReferenceType taskEPR=WSServerUtilities.makeEPR("Task", uuid, kernel);
		TaskClient c=new TaskClient(taskEPR,kernel.getClientConfiguration());
		assertTrue(c.getResult()==null);
		createAndStoreResult(uuid);
		c.setUpdateInterval(-1);
		XmlObject result=c.getResult();
		String text=Utilities.extractElementTextAsString(result);
		assertTrue("testresult".equals(text));
		assertTrue(c.getStatus().equals(StatusType.SUCCESSFUL));
		assertTrue(c.getExitCode().equals(Integer.valueOf(13)));
		assertTrue(c.getStatusMessage().equals("test123"));	
	}
	
	private void createAndStoreResult(String uuid)throws Exception{
		XmlObject result=XmlObject.Factory.parse("<t:test xmlns:t=\"http://test\">testresult</t:test>");
		TaskImpl.putResult(kernel, uuid, result, "test123", 13);
	}
	
	private String createNewInstance() throws Exception{
		String URL=kernel.getContainerProperties().getValue(ContainerProperties.WSRF_BASEURL)+"/Test?res=123";
		Home taskHome=kernel.getHome("Task");
		if(taskHome==null)throw new Exception("Task service is not deployed");
		EndpointReferenceType epr=EndpointReferenceType.Factory.newInstance();
		epr.addNewAddress().setStringValue(URL);
		InitParameters initP = new InitParameters();
		initP.parentServiceName = "Test";
		initP.parentUUID = "123";
		return taskHome.createResource(initP);
	}

}
