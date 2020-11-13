package de.fzj.unicore.uas.admin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionDocument;
import org.junit.Assert;
import org.junit.Test;

import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.client.JobClient;
import de.fzj.unicore.uas.fts.FileTransferCapability;
import de.fzj.unicore.uas.impl.tss.TargetSystemHomeImpl;
import de.fzj.unicore.uas.testsuite.AbstractJobRun;
import de.fzj.unicore.wsrflite.admin.AdminActionResult;
import de.fzj.unicore.wsrflite.xmlbeans.WSUtilities;
import eu.unicore.bugsreporter.annotation.FunctionalTest;


public class TestAdminActions extends AbstractJobRun {

	@Test
	@FunctionalTest(id="testToggleJobSubmission", description="Tests the ToggleJobSubmission admin action")
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
	@FunctionalTest(id="testShowServerStatusOverview", description="Tests the ShowServerStatusOverview admin action")
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
	@FunctionalTest(id="testShowJobDetails", description="Tests the ShowJobDetails admin action")
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
	
	@Override
	protected JobDefinitionDocument getJob() {
		JobDefinitionDocument jdd=JobDefinitionDocument.Factory.newInstance();
		jdd.addNewJobDefinition().addNewJobDescription().addNewApplication().setApplicationName("Date");
		return jdd;
	}
}
