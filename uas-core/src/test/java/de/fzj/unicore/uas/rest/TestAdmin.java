package de.fzj.unicore.uas.rest;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import javax.ws.rs.core.MediaType;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.junit.Test;

import de.fzj.unicore.uas.Base;
import de.fzj.unicore.wsrflite.server.JettyServer;
import eu.unicore.services.rest.client.BaseClient;
import eu.unicore.util.httpclient.HttpUtils;

public class TestAdmin extends Base {

	@Test
	public void testGetJSON() throws Exception {
		JettyServer server=kernel.getServer();
		String url = server.getUrls()[0].toExternalForm()+"/rest";
		HttpClient client=HttpUtils.createClient(url, kernel.getClientConfiguration());
		String resource  = url+"/admin";
		System.out.println("Accessing "+resource);
		HttpGet get=new HttpGet(resource);
		get.addHeader("Accept", MediaType.APPLICATION_JSON);
		HttpResponse response=client.execute(get);
		int status=response.getStatusLine().getStatusCode();
		assertEquals("Got: "+response.getStatusLine(),200, status);
		String reply=IOUtils.toString(response.getEntity().getContent(), "UTF-8");
		System.out.println("Service reply: "+reply);
		assertTrue(reply.contains("connectionStatus"));
	}
	
	@Test
	public void testGetHTML() throws Exception {
		JettyServer server=kernel.getServer();
		String url = server.getUrls()[0].toExternalForm()+"/rest";
		HttpClient client=HttpUtils.createClient(url, kernel.getClientConfiguration());
		String resource  = url+"/admin";
		System.out.println("Accessing "+resource);
		HttpGet get=new HttpGet(resource);
		get.addHeader("Accept", MediaType.TEXT_HTML);
		HttpResponse response=client.execute(get);
		int status=response.getStatusLine().getStatusCode();
		assertEquals("Got: "+response.getStatusLine(),200, status);
		String reply=IOUtils.toString(response.getEntity().getContent(), "UTF-8");
		System.out.println("Service reply: "+reply);
		assertTrue(reply.contains("connectionStatus"));
	}
	
	@Test
	public void testBaseGetJSON() throws Exception {
		JettyServer server=kernel.getServer();
		String resource = server.getUrls()[0].toExternalForm()+"/rest/core";
		
		BaseClient bcl = new BaseClient(resource, kernel.getClientConfiguration());
		HttpResponse response=bcl.get(ContentType.APPLICATION_JSON);
		int status=response.getStatusLine().getStatusCode();
		assertEquals("Got: "+response.getStatusLine(),200, status);
		String reply=IOUtils.toString(response.getEntity().getContent(), "UTF-8");
		System.out.println("Service reply: "+reply);
		
		// check that the links work
		resource = bcl.getLink("factories");
		System.out.println("Factories: "+bcl.getJSON());
		
	}

}
