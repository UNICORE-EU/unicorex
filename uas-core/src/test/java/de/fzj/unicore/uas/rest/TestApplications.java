package de.fzj.unicore.uas.rest;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.junit.Test;

import de.fzj.unicore.uas.Base;
import eu.unicore.services.rest.client.BaseClient;

public class TestApplications extends Base {

	@Test
	public void testCreateTSSAndListApps() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest";
		String resource  = url+"/core/sites";
		BaseClient client = new BaseClient(resource,kernel.getClientConfiguration());
		System.out.println("Accessing "+resource);
		
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
		
		// get apps representation(s)
		String appsUrl = client.getLink("applications");
		client.setURL(appsUrl);
		String apps = client.getJSON().toString(2);
		
		assertTrue(apps.contains("Date---v1.0"));
		
		// get single app
		client.setURL(appsUrl+"/Date---v1.0");
		JSONObject date = client.getJSON();
		System.out.println(date.toString(2));
		assertNotNull(date.getString("ApplicationName"));
		
		// test non existing one
		try{
			client.setURL(appsUrl+"/no_such_app---v1.0");
			client.getJSON();
			fail("Expect a 404");
		}catch(Exception ex){
			assertEquals("Expect a 404",404,client.getLastHttpStatus());
		}
		
	}
	
}
