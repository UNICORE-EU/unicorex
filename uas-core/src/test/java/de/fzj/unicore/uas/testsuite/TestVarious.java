package de.fzj.unicore.uas.testsuite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.xmlbeans.XmlObject;
import org.json.JSONObject;
import org.junit.Test;
import org.unigrids.services.atomic.types.StatusType;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.Base;
import de.fzj.unicore.uas.client.TaskClient;
import de.fzj.unicore.uas.impl.task.TaskImpl;
import de.fzj.unicore.uas.xnjs.XNJSFacade;
import de.fzj.unicore.xnjs.ems.BasicManager;
import de.fzj.unicore.xnjs.ems.InternalManager;
import eu.unicore.client.Endpoint;
import eu.unicore.client.core.CoreClient;
import eu.unicore.client.core.JobClient;
import eu.unicore.client.core.SiteClient;
import eu.unicore.client.core.SiteFactoryClient;
import eu.unicore.services.ContainerProperties;
import eu.unicore.services.Home;
import eu.unicore.services.InitParameters;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.services.rest.client.UsernamePassword;
import eu.unicore.services.utils.Utilities;
import eu.unicore.services.ws.utils.WSServerUtilities;
import eu.unicore.util.httpclient.ClientProperties;

public class TestVarious extends Base {
	String url;
	EndpointReferenceType epr;

	@Test
	public void testTaskService()throws Exception{
		String URL=kernel.getContainerProperties().getValue(ContainerProperties.EXTERNAL_URL)+"/services/Test?res=123";
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
		String URL=kernel.getContainerProperties().getValue(ContainerProperties.EXTERNAL_URL)+"services/Test?res=123";
		Home taskHome=kernel.getHome("Task");
		if(taskHome==null)throw new Exception("Task service is not deployed");
		EndpointReferenceType epr=EndpointReferenceType.Factory.newInstance();
		epr.addNewAddress().setStringValue(URL);
		InitParameters initP = new InitParameters();
		initP.parentServiceName = "Test";
		initP.parentUUID = "123";
		return taskHome.createResource(initP);
	}
	
	@Test
	public void testTSSReInitJobList() throws Exception {
		testRecreate();
		testRecreateXNJS();
	}
	
	private void testRecreate()throws Exception{
		ClientProperties security = kernel.getClientConfiguration();
		IAuthCallback auth = new UsernamePassword("demouser", "test123");
		CoreClient c = new CoreClient(
				new Endpoint(kernel.getContainerProperties().getContainerURL()+"/rest/core"),
				security, auth);
		SiteFactoryClient tsf = c.getSiteFactoryClient();
		for(String url : tsf.getSiteList()) {
			new SiteClient(new Endpoint(url), security, auth).delete();
		}
		// create two TSS - we only want to re-create jobs that 
		// are not listed by another TSS
		SiteClient tss = tsf.createSite();
		waitUntilReady(tss);
		// make sure no jobs exist
		for(String url: tss.getJobsList()){
			new JobClient(new Endpoint(url), security, auth).delete();
		}
		runJob(tss);
		
		// these jobs we want to re-create
		tss = tsf.createSite();
		assertTrue(tsf.getSiteList().getUrls(0, 100).size()==2);

		int existingJobs = tss.getJobsList().getUrls(0, 100).size();
		int numJobs=3;

		for(int i=0;i<numJobs;i++){
			runJob(tss);
		}
		Thread.sleep(5000);
		tss.setUpdateInterval(-1);
		long nj = tss.getJobsList().getUrls(0, 100).size();
		assertEquals(existingJobs+numJobs,nj);
		tss.delete();

		tss = tsf.createSite();
		waitUntilReady(tss);

		assertEquals(existingJobs+numJobs, tss.getJobsList().getUrls(0, 100).size());
	}

	private void testRecreateXNJS()throws Exception{
		ClientProperties security = kernel.getClientConfiguration();
		IAuthCallback auth = new UsernamePassword("demouser", "test123");
		CoreClient c = new CoreClient(
				new Endpoint(kernel.getContainerProperties().getContainerURL()+"/rest/core"),
				security, auth);
		SiteFactoryClient tsf = c.getSiteFactoryClient();
		for(String url : tsf.getSiteList()) {
			new SiteClient(new Endpoint(url), security, auth).delete();
		}
		SiteClient tss = tsf.createSite();
		waitUntilReady(tss);

		int existingJobs = tss.getJobsList().getUrls(0, 100).size();
		int numJobs=3;
		
		for(int i=0;i<numJobs;i++){
			runJob(tss);
		}
		Thread.sleep(5000);
		tss.setUpdateInterval(-1);
		long nj = tss.getJobsList().getUrls(0, 100).size();
		assertEquals(existingJobs+numJobs,nj);
		tss.delete();

		// kill the xnjs jobs
		((BasicManager)XNJSFacade.get(null, kernel).getXNJS().get(InternalManager.class)).
		getActionStore().removeAll();

		tss = tsf.createSite();
	
		waitUntilReady(tss);

		assertEquals(existingJobs+numJobs, tss.getJobsList().getUrls(0, 100).size());

		// check some properties of the re-generated jobs
		JobClient j = new JobClient(new Endpoint(tss.getJobsList().getUrls(0, 100).get(0)),
				security, auth);
		System.out.println(j.getProperties().toString(2));
	}


	private void runJob(SiteClient tss)throws Exception{
		JSONObject job = new JSONObject();
		job.put("ApplicationName", "Date");
		tss.submitJob(job);
	}

	private void waitUntilReady(SiteClient tss)throws Exception{
		int c=0;
		String lastStatus=null;
		while(c<10){
			String s=tss.getProperties().getString("resourceStatus");
			if(!s.equals(lastStatus)){
				lastStatus=s;
				System.out.println("TSS Status is: "+s);	
			}
			Thread.sleep(1000);
			c++;
			if("READY".equals(s))break;
		}
	}

}
