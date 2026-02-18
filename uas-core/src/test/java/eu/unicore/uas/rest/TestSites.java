package eu.unicore.uas.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.BaseServiceClient;
import eu.unicore.client.core.CoreClient;
import eu.unicore.client.core.JobClient;
import eu.unicore.client.core.SiteClient;
import eu.unicore.client.core.SiteFactoryClient;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.services.restclient.RESTException;
import eu.unicore.uas.Base;

public class TestSites extends Base {


	@Test
	public void testFactoryCreateTSS() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest";
		String resource = url+"/core/factories/default_target_system_factory";
		System.out.println("Accessing "+resource);
		IAuthCallback auth = getAuth();
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

		// check TSS properties
		System.out.println(sc.getProperties().toString(2));

		// check site-specific storages list
		assertEquals(1, sc.getSiteSpecificStorages().size());

		sc.delete();
	}

	@Test
	public void testCreateTSSAndSubmitJob() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest/core";
		CoreClient client = new CoreClient(new Endpoint(url), kernel.getClientConfiguration(), getAuth());
		// create a new TSS
		SiteClient tss = client.getSiteFactoryClient().createSite();
		System.out.println(tss.getProperties().toString(2));
		// submit a job to it
		JobClient job = submitJob(tss);
		System.out.println("*** new job: ");
		System.out.println(job.getProperties().toString(2));
		
		// submit a few more
		for(int i = 0; i<4; i++){
			submitJob(tss);
		}
		List<String> jobList = tss.getJobsList().getUrls(0, 5);
		System.out.println(jobList);
	}

	@Test
	public void testGetOrCreateTSS() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest/core";
		IAuthCallback auth = getAuth();
		CoreClient core = new CoreClient(new Endpoint(url), kernel.getClientConfiguration(), auth);
		SiteClient sc = core.getSiteFactoryClient().getOrCreateSite();
		String u1 = sc.getEndpoint().getUrl();
		System.out.println("Created: "+u1);
		sc = core.getSiteFactoryClient().getOrCreateSite();
		assertEquals(u1, sc.getEndpoint().getUrl());
	}

	@Test
	public void testAccesControl()throws Exception {
		String ep = kernel.getContainerProperties().getContainerURL()+
				"/rest/core/factories/default_target_system_factory";
		SiteFactoryClient sms = new SiteFactoryClient(new Endpoint(ep), kernel.getClientConfiguration(), getAuth());
		RESTException re = assertThrows(RESTException.class, ()->sms.delete());
		assertEquals(403, re.getStatus());
		RESTException re1 = assertThrows(RESTException.class, ()->sms.setProperties(new JSONObject()));
		assertEquals(403, re1.getStatus());
	}

	private JobClient submitJob(SiteClient client) throws Exception{
		JSONObject task = new JSONObject();
		task.put("ApplicationName", "Date");
		return client.submitJob(task);
	}
}
