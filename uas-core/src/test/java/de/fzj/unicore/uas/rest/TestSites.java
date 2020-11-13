package de.fzj.unicore.uas.rest;

import static org.junit.Assert.assertEquals;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.junit.Test;

import de.fzj.unicore.uas.Base;
import eu.unicore.client.Endpoint;
import eu.unicore.client.core.BaseServiceClient;
import eu.unicore.client.core.CoreClient;
import eu.unicore.client.core.SiteClient;
import eu.unicore.client.core.SiteFactoryClient;
import eu.unicore.services.rest.client.BaseClient;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.services.rest.client.UsernamePassword;

public class TestSites extends Base {


	@Test
	public void testFactoryCreateTSS() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest";
		String resource = url+"/core/factories/default_target_system_factory";
		System.out.println("Accessing "+resource);
		IAuthCallback auth = new UsernamePassword("demouser", "test123");
		SiteFactoryClient client = new SiteFactoryClient(new Endpoint(resource), kernel.getClientConfiguration(), auth);	

		// get its properties
		JSONObject tsfProps = client.getProperties();
		System.out.println(tsfProps.toString(2));
		
		// get apps list
		String appsUrl = resource+"/applications";
		BaseServiceClient bc = new BaseServiceClient(new Endpoint(appsUrl), kernel.getClientConfiguration(), auth);
		System.out.println(bc.getProperties().toString(2));
	
		// create a new TSS
		SiteClient sc = client.getOrCreateSite();
		System.out.println("created: "+sc.getEndpoint().getUrl());
		sc.delete();
	}

	@Test
	public void testCreateTSSAndSubmitJob() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest";
		String resource  = url+"/core/sites";
		System.out.println("Accessing "+resource);
		BaseClient client = new BaseClient(resource, kernel.getClientConfiguration());
		
		// create a new TSS
		JSONObject tssDesc = new JSONObject();
		
		HttpResponse response = client.post(tssDesc);
		int status = client.getLastHttpStatus();
		assertEquals("Got: "+client.getLastStatus(),201, status);
		String tssUrl = response.getFirstHeader("Location").getValue();
		System.out.println("created: "+tssUrl);
		EntityUtils.consumeQuietly(response.getEntity());

		// get its properties
		client.setURL(tssUrl);
		JSONObject tssProps = client.getJSON();
		System.out.println(tssProps.toString(2));
		
		// submit a job to it
		String jobUrl = submitJob(client, tssUrl);
		// get job properties
		client.setURL(jobUrl);
		JSONObject jobProps = client.getJSON();
		status = client.getLastHttpStatus();
		assertEquals("Got: "+client.getLastStatus(),200, status);
		System.out.println("*** new job: ");
		System.out.println(jobProps.toString(2));
		
		// submit a few more
		for(int i = 0; i<4; i++){
			submitJob(client, tssUrl);
		}
		
		// check job enumeration
		client.setURL(tssUrl+"/jobs?offset=0&num=5");
		JSONObject jobList = client.getJSON();
		System.out.println(jobList.toString(2));
		
		// get apps list
		String appsUrl = tssUrl+"/applications";
		client.setURL(appsUrl);
		String apps = client.getJSON().toString(2);
		System.out.println(apps);
	}

	@Test
	public void testGetOrCreateTSS() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest/core";
		IAuthCallback auth = new UsernamePassword("demouser", "test123");
		CoreClient core = new CoreClient(new Endpoint(url), kernel.getClientConfiguration(), auth);
		SiteClient sc = core.getSiteFactoryClient().getOrCreateSite();
		String u1 = sc.getEndpoint().getUrl();
		System.out.println("Created: "+u1);
		sc = core.getSiteFactoryClient().getOrCreateSite();
		assertEquals(u1, sc.getEndpoint().getUrl());
	}
	
	private String submitJob(BaseClient client, String target) throws Exception{
		JSONObject task = new JSONObject();
		task.put("ApplicationName", "Date");
		client.setURL(target);
		HttpResponse response = client.post(task);
		int status = client.getLastHttpStatus();
		assertEquals("Got: "+client.getLastStatus(),201, status);
		String jobUrl = response.getFirstHeader("Location").getValue();
		System.out.println("created: "+jobUrl);
		EntityUtils.consumeQuietly(response.getEntity());
		return jobUrl;
	}
}
