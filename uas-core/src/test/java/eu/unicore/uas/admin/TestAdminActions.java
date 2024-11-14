package eu.unicore.uas.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.CoreClient;
import eu.unicore.client.core.JobClient;
import eu.unicore.services.admin.AdminActionResult;
import eu.unicore.services.restclient.UsernamePassword;
import eu.unicore.uas.Base;
import eu.unicore.uas.UAS;
import eu.unicore.uas.fts.FileTransferCapability;
import eu.unicore.uas.impl.tss.TargetSystemHomeImpl;


public class TestAdminActions extends Base {

	@Test
	public void testToggleJobSubmission(){
		Map<String,String>params = new HashMap<>();
		params.put("message", "test123");
		TargetSystemHomeImpl th=(TargetSystemHomeImpl)uas.getKernel().getHome(UAS.TSS);
		assertTrue(th.isJobSubmissionEnabled());
		new ToggleJobSubmission().invoke(params, uas.getKernel());
		assertFalse(th.isJobSubmissionEnabled());
		assertEquals("test123", th.getHighMessage());
		params.put("message", "OK");
		new ToggleJobSubmission().invoke(params,uas.getKernel());
		assertTrue(th.isJobSubmissionEnabled());
		assertEquals("OK", th.getHighMessage());
	}
	
	@Test
	public void testShowServerStatusOverview()throws Exception{
		Map<String,String>params = new HashMap<>();
		AdminActionResult res=new ShowServerUsageOverview().invoke(params,uas.getKernel());
		assertTrue(res.successful());
		System.out.println(res.getMessage());
		System.out.println(res.getResults());
	}
	
	@Test
	public void testShowJobDetails()throws Exception{
		JobClient job = runJob();
		Map<String,String>params = new HashMap<>();
		String jId = job.getEndpoint().getUrl();
		jId = jId.substring(jId.lastIndexOf("/")+1);
		params.put("jobID", jId);
		AdminActionResult res=new ShowJobDetails().invoke(params,uas.getKernel());
		assertTrue(res.successful());
		System.out.println(res.getMessage());
		System.out.println(res.getResults());
		
		//check unknown job
		params=new HashMap<String,String>();
		params.put("jobID", "no_such_job_should_exist");
		res=new ShowJobDetails().invoke(params,uas.getKernel());
		assertTrue(res.successful());
		System.out.println(res.getMessage());
		System.out.println(res.getResults());
		assertTrue(res.getResults().get("Info").contains("No such job"));
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
		assertTrue(ft.size()>0);
		assertTrue(ft.stream().filter(c->c.getName().contains("SBYTEIO")).count()==0);
	}
	

	@Test
	@SuppressWarnings("unchecked")
	public void testMerge(){
		List<String>serviceNames=Arrays.asList(new String[]{"foo","bar"});
		Map<String, AtomicInteger>fooInst = new HashMap<>();
		fooInst.put("Alice", new AtomicInteger(3));
		fooInst.put("Bob", new AtomicInteger(10));
		Map<String, AtomicInteger>barInst = new HashMap<>();
		barInst.put("Alice", new AtomicInteger(1));
		barInst.put("Bob", new AtomicInteger(0));
		Map<String,Map<String,Integer>>merged=new ShowServerUsageOverview().merge(null, serviceNames, fooInst, barInst);
		assertEquals(2, merged.size());
		assertNotNull(merged.get("Alice"));
		assertEquals(2,merged.get("Alice").size());
		assertNotNull(merged.get("Bob"));
		assertEquals(2,merged.get("Bob").size());
		System.out.println(merged);
		assertEquals((Integer)10,merged.get("Bob").get("foo"));
		assertEquals((Integer)0,merged.get("Bob").get("bar"));
	}

	@Test
	public void testListPartitions(){
		Map<String,String>params = new HashMap<>();
		AdminActionResult res = new ListPartitions().invoke(params, uas.getKernel());
		assertTrue(res.successful());
		System.out.println(res.getMessage());
		System.out.println(res.getResults());
	}

	private JobClient runJob() throws Exception {
		CoreClient c = new CoreClient(
				new Endpoint(kernel.getContainerProperties().getContainerURL()+"/rest/core"),
				kernel.getClientConfiguration(),
				new UsernamePassword("demouser", "test123"));
		JSONObject job = new JSONObject();
		job.put("ApplicationName", "Date");
		return c.getSiteClient().submitJob(job);
	}
	
}
