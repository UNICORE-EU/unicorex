package eu.unicore.client;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import de.fzj.unicore.uas.Base;
import eu.unicore.client.core.CoreClient;
import eu.unicore.client.core.EnumerationClient;
import eu.unicore.client.core.JobClient;
import eu.unicore.client.core.SiteClient;
import eu.unicore.client.core.SiteFactoryClient;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.services.rest.client.UsernamePassword;

public class TestJobManagement extends Base {

	@Test
	public void testSubmitJob() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest";
		String resource = url+"/core";
		System.out.println("Accessing "+resource);
		Endpoint ep = new Endpoint(resource);
		IAuthCallback auth = new UsernamePassword("demouser", "test123");
		CoreClient client = new CoreClient(ep, kernel.getClientConfiguration(), auth);
		
		SiteFactoryClient sfc = client.getSiteFactoryClient();
		SiteClient sc = sfc.getOrCreateSite();
		JSONObject job = new JSONObject();
		job.put("ApplicationName", "Date");
		sc.submitJob(job);
		job.put("Tags", new JSONArray("[\"date\"]"));
		JobClient jc = sc.submitJob(job);
		jc.setUpdateInterval(-1);
		waitForFinish(jc);
		
		EnumerationClient jobList = sc.getJobsList();
		System.out.println(jobList.getProperties().toString(2));
		
		List<String> jL = jobList.getUrls(0, 100, (String[])null);
		Assert.assertEquals(2, jL.size());
		List<String> jL2 = jobList.getUrls(0, 100, "date");
		Assert.assertEquals(1, jL2.size());

	}
	
	protected void waitForFinish(JobClient jc) throws Exception {
		int c=0;
		while(c<20 && !jc.isFinished()){
			Thread.sleep(1000);
			c++;
		}
	}
}
