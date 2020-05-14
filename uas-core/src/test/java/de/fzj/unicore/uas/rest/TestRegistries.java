package de.fzj.unicore.uas.rest;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

import de.fzj.unicore.uas.Base;
import eu.unicore.services.rest.client.BaseClient;

public class TestRegistries extends Base {


	@Test
	public void testRegistryProperties() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest";
		String resource = url+"/registries/default_registry";
		System.out.println("Accessing "+resource);
		BaseClient client = new BaseClient(resource, kernel.getClientConfiguration());
		
		// get its properties
		JSONObject props = client.getJSON();
		System.out.println(props.toString(2));
		
		// check that the core service entry is there
		JSONArray entries = props.getJSONArray("entries");
		boolean found = false;
		for (int i=0; i<entries.length();i++){
			JSONObject o = entries.getJSONObject(i);
			if(o.getString("type").equals("CoreServices")){
				found = true;
				break;
			}
		}
		assertTrue("CoreServices not in registry", found);
	}

}
