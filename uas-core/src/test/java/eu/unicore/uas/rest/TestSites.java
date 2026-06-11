package eu.unicore.uas.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

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
		SiteFactoryClient client = new SiteFactoryClient(resource, kernel.getClientConfiguration(), auth);	

		// get its properties
		JSONObject tsfProps = client.getProperties();
		System.out.println(tsfProps.toString(2));
		
		// get apps list
		String appsUrl = resource+"/applications";
		BaseServiceClient bc = new BaseServiceClient(appsUrl, kernel.getClientConfiguration(), auth);
		System.out.println(bc.getProperties().toString(2));
	
		// create a new TSS
		SiteClient sc = client.getOrCreateSite();
		System.out.println("created: "+sc.getEndpoint());

		// check TSS properties
		System.out.println(sc.getProperties().toString(2));

		// check site-specific storages list
		assertEquals(1, sc.getSiteSpecificStorages().size());

		sc.delete();
		IOUtils.closeQuietly(sc, client, bc);
	}

	@Test
	public void testCreateTSSAndSubmitJob() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest/core";
		CoreClient client = new CoreClient(url, kernel.getClientConfiguration(), getAuth());
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
		IOUtils.closeQuietly(tss, client);
	}

	@Test
	public void testGetOrCreateTSS() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest/core";
		IAuthCallback auth = getAuth();
		CoreClient core = new CoreClient(url, kernel.getClientConfiguration(), auth);
		SiteClient sc = core.getSiteFactoryClient().getOrCreateSite();
		String u1 = sc.getEndpoint();
		System.out.println("Created: "+u1);
		sc = core.getSiteFactoryClient().getOrCreateSite();
		assertEquals(u1, sc.getEndpoint());
		IOUtils.closeQuietly(core, sc);

	}

	@Test
	public void testAccesControl()throws Exception {
		String ep = kernel.getContainerProperties().getContainerURL()+
				"/rest/core/factories/default_target_system_factory";
		SiteFactoryClient sms = new SiteFactoryClient(ep, kernel.getClientConfiguration(), getAuth());
		RESTException re = assertThrows(RESTException.class, ()->sms.delete());
		assertEquals(403, re.getStatus());
		RESTException re1 = assertThrows(RESTException.class, ()->sms.setProperties(new JSONObject()));
		assertEquals(403, re1.getStatus());
		sms.close();
	}

	private JobClient submitJob(SiteClient client) throws Exception{
		JSONObject task = new JSONObject();
		task.put("ApplicationName", "Date");
		return client.submitJob(task);
	}
}
