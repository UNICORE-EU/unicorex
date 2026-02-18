package eu.unicore.uas.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.CoreClient;
import eu.unicore.client.core.JobClient;
import eu.unicore.client.core.SiteClient;
import eu.unicore.client.core.SiteFactoryClient;
import eu.unicore.client.utils.TaskClient;
import eu.unicore.client.utils.TaskClient.Status;
import eu.unicore.services.Home;
import eu.unicore.services.InitParameters;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.uas.Base;
import eu.unicore.uas.impl.task.TaskImpl;
import eu.unicore.uas.xnjs.XNJSFacade;
import eu.unicore.util.httpclient.ClientProperties;
import eu.unicore.xnjs.ems.BasicManager;
import eu.unicore.xnjs.ems.InternalManager;

public class TestHelpers extends Base {
	String url;

	@Test
	public void testTaskService()throws Exception{
		String URL = kernel.getContainerProperties().getContainerURL()+"/rest/storages/123";
		String uuid = createNewInstance(URL);
		String task = kernel.getContainerProperties().getContainerURL()+"/rest/core/tasks/"+uuid;
		TaskClient c = new TaskClient(new Endpoint(task), kernel.getClientConfiguration(), getAuth());
		c.setUpdateInterval(-1);
		JSONObject properties = c.getProperties();
		System.out.println(properties.toString(2));
		assertEquals(URL, properties.getString("submissionService"));
		assertEquals(Status.RUNNING, c.getStatus());
		c.executeAction("abort", new JSONObject());
		assertEquals(Status.FAILED, c.getStatus());
	}
	
	@Test
	public void createResultTest()throws Exception{
		String URL = kernel.getContainerProperties().getContainerURL()+"/rest/test/123";
		String uuid = createNewInstance(URL);
		String task = kernel.getContainerProperties().getContainerURL()+"/rest/core/tasks/"+uuid;
		TaskClient c = new TaskClient(new Endpoint(task), kernel.getClientConfiguration(), getAuth());
		assertTrue(c.getResult().size()==0);
		createAndStoreResult(uuid);
		c.setUpdateInterval(-1);
		Map<String,String> result = c.getResult();
		String text = result.get("test");
		assertTrue("testresult".equals(text));
		assertEquals(Status.SUCCESSFUL, c.getStatus());
		assertEquals(Integer.valueOf(13), c.getExitCode());
		assertEquals("test123", c.getStatusMessage());	
		System.out.println(c.getProperties().toString(2));
	}

	private void createAndStoreResult(String uuid)throws Exception{
		Map<String,String> result = new HashMap<>();
		result.put("test", "testresult");
		TaskImpl.putResult(kernel, uuid, result, "test123", 13);
	}
	
	private String createNewInstance(String submissionServiceURL) throws Exception{
		Home taskHome = kernel.getHome("Task");
		InitParameters initP = new InitParameters();
		initP.parentServiceName = submissionServiceURL;
		initP.parentUUID = "123";
		initP.ownerDN = "CN=Demo User,O=UNICORE,C=EU";
		return taskHome.createResource(initP);
	}
	
	@Test
	public void testTSSReInitJobList() throws Exception {
		testRecreate();
		testRecreateXNJS();
	}
	
	private void testRecreate()throws Exception{
		ClientProperties security = kernel.getClientConfiguration();
		IAuthCallback auth = getAuth();
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
		IAuthCallback auth = getAuth();
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
