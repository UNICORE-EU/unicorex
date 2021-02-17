package de.fzj.unicore.uas.testsuite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.apache.xmlbeans.XmlObject;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionType;
import org.junit.Test;
import org.unigrids.services.atomic.types.StatusType;
import org.unigrids.x2006.x04.services.tss.SubmitDocument;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.Base;
import de.fzj.unicore.uas.TargetSystemFactory;
import de.fzj.unicore.uas.client.JobClient;
import de.fzj.unicore.uas.client.TSFClient;
import de.fzj.unicore.uas.client.TSSClient;
import de.fzj.unicore.uas.client.TaskClient;
import de.fzj.unicore.uas.impl.task.TaskImpl;
import de.fzj.unicore.uas.xnjs.XNJSFacade;
import de.fzj.unicore.wsrflite.ContainerProperties;
import de.fzj.unicore.wsrflite.Home;
import de.fzj.unicore.wsrflite.InitParameters;
import de.fzj.unicore.wsrflite.utils.Utilities;
import de.fzj.unicore.wsrflite.xmlbeans.client.RegistryClient;
import de.fzj.unicore.wsrflite.xmlbeans.sg.Registry;
import de.fzj.unicore.xnjs.ems.InternalManager;
import de.fzj.unicore.xnjs.ems.BasicManager;
import eu.unicore.bugsreporter.annotation.FunctionalTest;
import eu.unicore.services.ws.utils.WSServerUtilities;
import eu.unicore.util.httpclient.ClientProperties;

public class TestVarious extends Base {
	String url;
	EndpointReferenceType epr;

	@Test
	public void testTaskService()throws Exception{
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
	
	@FunctionalTest(id="TSSReinit", 
			description="Tests TargetSystemService behaviour when re-created by a user")
	@Test
	public void testTSSReInitJobList() throws Exception {
		testRecreate();
		testRecreateXNJS();
	}
	
	private void testRecreate()throws Exception{
		url=kernel.getContainerProperties().getValue(ContainerProperties.WSRF_BASEURL);
		epr=EndpointReferenceType.Factory.newInstance();
		epr.addNewAddress().setStringValue(url+"/"+Registry.REGISTRY_SERVICE+"?res=default_registry");
		ClientProperties securityProps = kernel.getClientConfiguration();

		RegistryClient reg=new RegistryClient(epr, securityProps);
		//find a TargetSystemFactory
		List<EndpointReferenceType> tsfs=reg.listServices(TargetSystemFactory.TSF_PORT);
		EndpointReferenceType tsfepr=findFirstAccessibleService(tsfs);
		TSFClient tsf=new TSFClient(tsfepr,securityProps);
		tsf.setUpdateInterval(-1);
		//clean up all TSSs to avoid dependencies on other tests...
		for(EndpointReferenceType tEpr: tsf.getAccessibleTargetSystems()){
			new TSSClient(tEpr,securityProps).destroy();
		}
		assertTrue(tsf.getAccessibleTargetSystems().size()==0);
		
		// create two TSS - we only want to re-create jobs that 
		// are not listed by another TSS
		TSSClient tss = tsf.createTSS();
		waitUntilReady(tss);
		// make sure no jobs exist
		for(EndpointReferenceType tEpr: tss.getJobs()){
			new JobClient(tEpr,securityProps).destroy();
		}
		runJob(tss);
		
		// these jobs we want to re-create
		tss = tsf.createTSS();
		assertTrue(tsf.getAccessibleTargetSystems().size()==2);

		int existingJobs=tss.getJobs().size();
		int numJobs=3;

		for(int i=0;i<numJobs;i++){
			runJob(tss);
		}
		Thread.sleep(5000);
		tss.setUpdateInterval(-1);
		long nj=tss.getJobs().size();
		assertEquals(existingJobs+numJobs,nj);
		tss.destroy();

		tss=tsf.createTSS();
		waitUntilReady(tss);

		assertEquals(existingJobs+numJobs, tss.getJobs().size());
	}

	private void testRecreateXNJS()throws Exception{
		url=kernel.getContainerProperties().getValue(ContainerProperties.WSRF_BASEURL);
		epr=EndpointReferenceType.Factory.newInstance();
		epr.addNewAddress().setStringValue(url+"/"+Registry.REGISTRY_SERVICE+"?res=default_registry");
		ClientProperties securityProps = kernel.getClientConfiguration();

		RegistryClient reg=new RegistryClient(epr, securityProps);
		//find a TargetSystemFactory
		List<EndpointReferenceType> tsfs=reg.listServices(TargetSystemFactory.TSF_PORT);
		EndpointReferenceType tsfepr=findFirstAccessibleService(tsfs);
		TSFClient tsf=new TSFClient(tsfepr,securityProps);
		tsf.setUpdateInterval(-1);
		//clean up all TSSs to avoid dependencies on other tests...
		for(EndpointReferenceType tEpr: tsf.getAccessibleTargetSystems()){
			new TSSClient(tEpr,securityProps).destroy();
		}
		assertTrue(tsf.getAccessibleTargetSystems().size()==0);
		TSSClient tss = tsf.createTSS();
		waitUntilReady(tss);
		assertTrue(tsf.getAccessibleTargetSystems().size()==1);

		int existingJobs=tss.getJobs().size();
		int numJobs=3;

		for(int i=0;i<numJobs;i++){
			runJob(tss);
		}
		Thread.sleep(5000);
		tss.setUpdateInterval(-1);
		long nj=tss.getJobs().size();
		assertEquals(existingJobs+numJobs,nj);
		tss.destroy();

		// kill the xnjs jobs
		((BasicManager)XNJSFacade.get(null, kernel).getXNJS().get(InternalManager.class)).
		getActionStore().removeAll();

		tss=tsf.createTSS();
	
		waitUntilReady(tss);

		assertEquals(existingJobs+numJobs, tss.getJobs().size());

		// check some properties of the re-generated jobs
		JobClient j=new JobClient(tss.getJobs().get(0),securityProps);
		System.out.println(j.getResourcePropertiesDocument());
	}


	private void runJob(TSSClient tss)throws Exception{
		SubmitDocument sd=SubmitDocument.Factory.newInstance();
		sd.addNewSubmit().setAutoStartWhenReady(true);
		JobDefinitionType jobDefinition=JobDefinitionType.Factory.newInstance();
		jobDefinition.addNewJobDescription().addNewApplication().setApplicationName("Date");
		sd.getSubmit().setJobDefinition(jobDefinition);
		tss.submit(sd);
	}

	private void waitUntilReady(TSSClient tss)throws Exception{
		int c=0;
		String lastStatus=null;
		while(c<10){
			String s=tss.getServiceStatus();
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
