package de.fzj.unicore.uas.admin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionDocument;
import org.junit.Assert;
import org.junit.Test;

import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.client.JobClient;
import de.fzj.unicore.uas.fts.FileTransferCapability;
import de.fzj.unicore.uas.impl.tss.TargetSystemHomeImpl;
import de.fzj.unicore.uas.testsuite.AbstractJobRun;
import eu.unicore.services.admin.AdminActionResult;
import eu.unicore.services.ws.WSUtilities;


public class TestAdminActions extends AbstractJobRun {

	@Test
	public void testToggleJobSubmission(){
		Map<String,String>params=new HashMap<String,String>();
		params.put("message", "test123");
		TargetSystemHomeImpl th=(TargetSystemHomeImpl)uas.getKernel().getHome(UAS.TSS);
		Assert.assertTrue(th.isJobSubmissionEnabled());
		new ToggleJobSubmission().invoke(params, uas.getKernel());
		Assert.assertFalse(th.isJobSubmissionEnabled());
		Assert.assertEquals("test123", th.getHighMessage());
		params.put("message", "OK");
		new ToggleJobSubmission().invoke(params,uas.getKernel());
		Assert.assertTrue(th.isJobSubmissionEnabled());
		Assert.assertEquals("OK", th.getHighMessage());
	}
	
	@Test
	public void testShowServerStatusOverview()throws Exception{
		initClients();
		runJob(tss);
		Map<String,String>params=new HashMap<String,String>();
		AdminActionResult res=new ShowServerUsageOverview().invoke(params,uas.getKernel());
		Assert.assertTrue(res.successful());
		System.out.println(res.getMessage());
		System.out.println(res.getResults());
	}
	
	@Test
	public void testShowJobDetails()throws Exception{
		initClients();
		JobClient job = runJob(tss);
		Map<String,String>params=new HashMap<String,String>();
		params.put("jobID", WSUtilities.extractResourceID(job.getEPR()));
		AdminActionResult res=new ShowJobDetails().invoke(params,uas.getKernel());
		Assert.assertTrue(res.successful());
		System.out.println(res.getMessage());
		System.out.println(res.getResults());
		
		//check unknown job
		params=new HashMap<String,String>();
		params.put("jobID", "no_such_job_should_exist");
		res=new ShowJobDetails().invoke(params,uas.getKernel());
		Assert.assertTrue(res.successful());
		System.out.println(res.getMessage());
		System.out.println(res.getResults());
		Assert.assertTrue(res.getResults().get("Info").contains("No such job"));
	}
	
	@Test
	public void testFindCapabilities() throws Exception {
		List<String>disabled = new ArrayList<>();
		disabled.add("SBYTEIO");
		Collection<FileTransferCapability> ft = kernel.getCapabilities(FileTransferCapability.class).stream().
				filter(c->c.isAvailable() && !disabled.contains(c.getProtocol())).
				collect(Collectors.toList());
		for(FileTransferCapability c: ft){
			System.out.println(c.getName()+" : "+c.getProtocol());
		}
		Assert.assertTrue(ft.size()>0);
		Assert.assertTrue(ft.stream().filter(c->c.getName().contains("SBYTEIO")).count()==0);
	}
	

	@Test
	@SuppressWarnings("unchecked")
	public void testMerge(){
		List<String>serviceNames=Arrays.asList(new String[]{"foo","bar"});
		Map<String, AtomicInteger>fooInst=new HashMap<String, AtomicInteger>();
		fooInst.put("Alice", new AtomicInteger(3));
		fooInst.put("Bob", new AtomicInteger(10));
		Map<String, AtomicInteger>barInst=new HashMap<String, AtomicInteger>();
		barInst.put("Alice", new AtomicInteger(1));
		barInst.put("Bob", new AtomicInteger(0));
		Map<String,Map<String,Integer>>merged=new ShowServerUsageOverview().merge(null, serviceNames, fooInst, barInst);
		Assert.assertEquals(2, merged.size());
		Assert.assertNotNull(merged.get("Alice"));
		Assert.assertEquals(2,merged.get("Alice").size());
		Assert.assertNotNull(merged.get("Bob"));
		Assert.assertEquals(2,merged.get("Bob").size());
		System.out.println(merged);
		Assert.assertEquals((Integer)10,merged.get("Bob").get("foo"));
		Assert.assertEquals((Integer)0,merged.get("Bob").get("bar"));
	}

	@Override
	protected JobDefinitionDocument getJob() {
		JobDefinitionDocument jdd=JobDefinitionDocument.Factory.newInstance();
		jdd.addNewJobDefinition().addNewJobDescription().addNewApplication().setApplicationName("Date");
		return jdd;
	}
}
