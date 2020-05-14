package de.fzj.unicore.xnjs.persistence;

import static org.junit.Assert.*;

import org.apache.xmlbeans.XmlObject;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionDocument;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.fzj.unicore.xnjs.ems.Action;
import eu.unicore.security.Client;
import eu.unicore.security.Queue;
import eu.unicore.security.SubjectAttributesHolder;
import eu.unicore.security.Xlogin;

public class TestPersistence {

	@Test
	public void testGSONBuilder(){
		GsonBuilder builder = new GsonBuilder();
		builder.registerTypeHierarchyAdapter(XmlObject.class, new GSONUtils.XmlBeansAdapter());
		Gson gson = builder.create();
		JobDefinitionDocument src = JobDefinitionDocument.Factory.newInstance();
		src.addNewJobDefinition().addNewJobDescription().addNewApplication().setApplicationName("foo");
		String json=gson.toJson(src);
		JobDefinitionDocument jdd=gson.fromJson(json, JobDefinitionDocument.class);
		assertNotNull(jdd);
		assertEquals("foo",jdd.getJobDefinition().getJobDescription().getApplication().getApplicationName());
	
		builder = new GsonBuilder();
		builder.registerTypeHierarchyAdapter(XmlObject.class, new GSONUtils.XmlBeansAdapter());
		builder.setPrettyPrinting();
		gson = builder.create();
		
		Client client = createClient();
		json=gson.toJson(client);
		System.out.println(json);
		
		Client client2 = gson.fromJson(json, Client.class);
		Xlogin x2=client2.getXlogin();
		Xlogin x1=client.getXlogin();
		assertTrue(x2.getEncoded().equals(x1.getEncoded()));
		
		builder = new GsonBuilder();
		builder.registerTypeHierarchyAdapter(XmlObject.class, new GSONUtils.XmlBeansAdapter());
		builder.setPrettyPrinting();
		gson = builder.create();
		
		Action a = new Action("1234");
		a.setClient(createClient());
		
		json=gson.toJson(a);

		Action a2 = gson.fromJson(json, Action.class);
		assertEquals(a.getUUID(), a2.getUUID());
	}
	
	private Client createClient(){
		Client client = new Client();
		Xlogin x1=new Xlogin(new String[]{"foo","bar"},new String[]{"g-foo","g-bar"});
		client.setXlogin(x1);
		client.setVos(new String[]{"vo1","vo2"});
		client.setQueue(new Queue(new String[]{"q1","q2"}));
		SubjectAttributesHolder sah = new SubjectAttributesHolder();
		client.setSubjectAttributes(sah);
		return client;
	}
}
