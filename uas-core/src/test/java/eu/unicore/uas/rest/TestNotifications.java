package eu.unicore.uas.rest;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.CoreClient;
import eu.unicore.client.core.JobClient;
import eu.unicore.client.core.SiteClient;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.services.rest.client.UsernamePassword;
import eu.unicore.uas.SecuredBase;
import eu.unicore.uas.notification.Notifications;

public class TestNotifications extends SecuredBase {

	@Test
	public void testNotifications() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest/core";
		IAuthCallback auth = new UsernamePassword("demouser", "test123");
		CoreClient core = new CoreClient(new Endpoint(url), kernel.getClientConfiguration(), auth);
		SiteClient site = core.getSiteFactoryClient().getOrCreateSite();
		
		JSONObject task = new JSONObject();
		task.put("ApplicationName", "Date");
		String notificationURL = kernel.getContainerProperties().getContainerURL()+"/rest/notification";
		task.put("Notification", notificationURL);
		JobClient job = site.submitJob(task);
		
		System.out.println("*** new job: "+job.getEndpoint().getUrl());
		while(!job.isFinished()) {
			Thread.sleep(1000);
		}
		System.out.println(job.getProperties().toString(2));
		assertTrue(job.getProperties().toString().contains(notificationURL));
		
		task = new JSONObject();
		task.put("ApplicationName", "Date");
		JSONObject notifications = new JSONObject();
		notifications.put("URL", notificationURL);
		JSONArray userDefinedTriggers = new JSONArray();
		userDefinedTriggers.put("SUCCESSFUL");
		notifications.put("status", userDefinedTriggers);
		task.put("NotificationSettings", notifications);
		job = site.submitJob(task);
		
		System.out.println("*** new job: "+job.getEndpoint().getUrl());
		while(!job.isFinished()) {
			Thread.sleep(1000);
		}
		System.out.println(job.getProperties().toString(2));
		assertTrue(job.getProperties().toString().contains(notificationURL));
		
		assertTrue(Notifications.notifications.size()>0);
		System.out.println("Notifications:");
		for(JSONObject n: Notifications.notifications) {
			System.out.println(n.toString(2));
		}
	}

}
