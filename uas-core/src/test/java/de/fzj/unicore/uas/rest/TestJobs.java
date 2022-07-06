package de.fzj.unicore.uas.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import de.fzj.unicore.uas.SecuredBase;
import eu.unicore.client.Endpoint;
import eu.unicore.client.core.CoreClient;
import eu.unicore.client.core.JobClient;
import eu.unicore.client.core.SiteClient;
import eu.unicore.services.rest.client.BaseClient;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.services.rest.client.UsernamePassword;

public class TestJobs extends SecuredBase {

	@Test
	public void testJobSubmission() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest/core/jobs";
		System.out.println("Accessing "+url);
		IAuthCallback auth = new UsernamePassword("demouser", "test123");
		BaseClient client = new BaseClient(url, kernel.getClientConfiguration(), auth);
		JSONObject task = new JSONObject();
		task.put("ApplicationName", "Date");
		String jobUrl = client.create(task);
		System.out.println("created: "+jobUrl);
		
		// get job properties
		client.setURL(jobUrl);
		JSONObject jobProps = client.getJSON();
		System.out.println("*** new job: ");
		System.out.println(jobProps.toString(2));
		
		// access parent TSS
		String tssUrl = client.getLink("parentTSS");
		client.setURL(tssUrl);
		JSONObject tssProps = client.getJSON();
		System.out.println("*** parent TSS: ");
		System.out.println(tssProps.toString(2));
		
		String jobsURL=client.getLink("jobs");
		client.setURL(jobsURL);
		// check that the job URL is listed
		System.out.println(client.getJSON().toString(2));
		assertTrue(contains(client.getJSON().getJSONArray("jobs"),jobUrl));
	}
	
	
	@Test
	public void testTaggedJobs() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest/core/jobs";
		System.out.println("Accessing "+url);
		IAuthCallback auth = new UsernamePassword("demouser", "test123");
		BaseClient client = new BaseClient(url,kernel.getClientConfiguration(), auth);
		
		JSONObject task = new JSONObject();
		task.put("ApplicationName", "Date");
		JSONArray tags = new JSONArray();
		tags.put("test");
		tags.put("foo");
		task.put("Tags", tags);
		String jobUrl = client.create(task);
		System.out.println("created: "+jobUrl);

		client.setURL(jobUrl);
		JSONObject jobProps = client.getJSON();
		System.out.println("*** new job: ");
		System.out.println(jobProps.toString(2));
		tags = jobProps.getJSONArray("tags");
		assertTrue(contains(tags, "foo"));
		assertTrue(contains(tags, "test"));

		// check listing with a query works
		client.setURL(url+"?tags=foo");
		JSONArray taggedJobs = client.getJSON().getJSONArray("jobs");
		assertEquals(1, taggedJobs.length());
		client.setURL(url+"?tags=nope");
		taggedJobs = client.getJSON().getJSONArray("jobs");
		assertEquals(0, taggedJobs.length());
	}
	
	@Test
	public void testNotifications() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest/core";
		IAuthCallback auth = new UsernamePassword("demouser", "test123");
		CoreClient core = new CoreClient(new Endpoint(url), kernel.getClientConfiguration(), auth);
		SiteClient site = core.getSiteFactoryClient().getOrCreateSite();
		
		JSONObject task = new JSONObject();
		task.put("ApplicationName", "Date");
		String notificationURL = kernel.getContainerProperties().getContainerURL()+"/rest/foo";
		task.put("Notification", notificationURL);
		JobClient job = site.submitJob(task);
		
		System.out.println("*** new job: "+job.getEndpoint().getUrl());
		while(!job.isFinished()) {
			Thread.sleep(1000);
		}
		System.out.println(job.getProperties().toString(2));
		assertTrue(job.getProperties().toString().contains(notificationURL));
	}
	
	
	private boolean contains(JSONArray array, Object o) throws JSONException {
		for(int i=0; i<array.length();i++){
			if(array.get(i).equals(o))return true;
		}
		return false;
	}
}
