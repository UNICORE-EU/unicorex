package eu.unicore.uas.rest;

import org.junit.Assert;
import org.junit.Test;

import eu.unicore.client.Endpoint;
import eu.unicore.client.Job;
import eu.unicore.client.Job.Resources;
import eu.unicore.client.Job.Type;
import eu.unicore.client.core.AllocationClient;
import eu.unicore.client.core.CoreClient;
import eu.unicore.client.core.JobClient;
import eu.unicore.client.core.SiteClient;
import eu.unicore.services.rest.client.UsernamePassword;
import eu.unicore.uas.Base;

public class TestAllocations extends Base {

	@Test
	public void test1() throws Exception{
		CoreClient c = new CoreClient(
				new Endpoint(kernel.getContainerProperties().getContainerURL()+"/rest/core"),
				kernel.getClientConfiguration(),
				new UsernamePassword("demouser", "test123"));
		SiteClient site = c.getSiteClient();
		Job j = new Job();
		Resources r = j.resources();
		r.runtime("60m");
		r.nodes(1);
		j.type(Type.ALLOCATE);
		j.executable("sleep 60"); // since we don't have a real BSS here
		AllocationClient alloc = site.createAllocation(j.getJSON());
		System.out.println(alloc.getProperties().toString(2));
		int i = 0;
		while(i<20) {
			JobClient.Status s = alloc.getStatus();
			if(JobClient.Status.RUNNING.equals(s))break;
			i++;
			Thread.sleep(1000);
		}
		Assert.assertEquals("ALLOCATE", alloc.getProperties().get("jobType"));
		Job j2 = new Job();
		j2.executable("date");
		JobClient jobClient = alloc.submitJob(j2.getJSON());
		i = 0;
		while(!jobClient.isFinished()&& i<3600) {
			i++;
			Thread.sleep(1000);
		}
		Assert.assertEquals("ON_LOGIN_NODE", jobClient.getProperties().get("jobType"));
		System.out.println(jobClient.getProperties().toString(2));
		
	}

}
