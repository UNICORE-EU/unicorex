package eu.unicore.uas.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import eu.unicore.services.restclient.BaseClient;
import eu.unicore.services.restclient.RESTException;
import eu.unicore.uas.Base;

public class TestApplications extends Base {

	@Test
	public void testCreateTSSAndListApps() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest/core/sites";
		try(BaseClient client = new BaseClient(url, kernel.getClientConfiguration(), getAuth()))
		{
			System.out.println("Accessing "+url);
			String tssUrl = client.create(new JSONObject());
			System.out.println("created: "+tssUrl);
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
			assertEquals(2, date.getJSONObject("Options").keySet().size());
			// test non existing one
			client.setURL(appsUrl+"/no_such_app---v1.0");
			RESTException re = assertThrows(RESTException.class, ()->{	client.getJSON();});
			assertEquals(404, re.getStatus());
		}
	}
}
