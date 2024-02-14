package eu.unicore.uas.trigger.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.junit.Test;

import eu.unicore.security.Client;
import eu.unicore.uas.trigger.Rule;
import eu.unicore.uas.trigger.RuleSet;
import eu.unicore.uas.trigger.xnjs.ScanSettings;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.io.IStorageAdapter;

public class TestTriggers {

	@Test
	public void testRuleFileCorrectness() throws Exception {
		try(InputStream is=new FileInputStream("src/test/resources/testing_rules"))
		{	
			new RuleFactory(null, "test123").readRules(is);
		}
		try(InputStream is=new FileInputStream("src/test/resources/trigger_rules"))
		{
			new RuleFactory(null, "test123").readRules(is);
		}
	}

	@Test
	public void testRuleFactory()throws Exception{
		RuleFactory rf=new RuleFactory(null, "test123");
		RuleSet rules=null;
		try(InputStream is=new FileInputStream("src/test/resources/testing_rules"))
		{
			rules=rf.readRules(is);
		}
		System.out.println(rules);
		Rule noop1=getRule("noop1", rules);
		assertNotNull(noop1);
		assertTrue(noop1.matches("/documents/foo1.pdf", null));
		assertFalse(noop1.matches("/documents/bar.txt", null));
		
		Rule local1=getRule("local1", rules);
		assertNotNull(local1);
		assertFalse(local1.matches("/documents/foo1.pdf", null));
		assertTrue(local1.matches("/documents/bar.jpg", null));
		ScanSettings  sc = new ScanSettings();
		try(InputStream is=new FileInputStream("src/test/resources/testing_rules"))
		{
			rf.updateSettings(sc, is);
		}
		assertEquals(5, sc.maxDepth);
		assertEquals(10, sc.gracePeriod);
		assertEquals(3600, sc.updateInterval);
		assertEquals(2, sc.includes.length);
		assertEquals(1, sc.excludes.length);
		
	}
	
	
	private Rule getRule(String name, Set<Rule>rules){
		for(Rule r: rules){
			if(name.equals(((SimpleRule)r).getName()))return r;
		}
		return null;
	}
	
	@Test
	public void testReplace() throws Exception {
		String json = FileUtils.readFileToString(new File("src/test/resources/testing_rules"), "UTF-8");
		JSONObject j = new JSONObject(json);
		Map<String,String>ctx = new HashMap<>();
		ctx.put("UC_FILE_NAME","replaced.txt");
		json = new BaseAction() {
			@Override
			public String run(IStorageAdapter storage, String filePath, Client client,
					XNJS xnjs) throws Exception {
				return null;
			}
		}.expandVariables(json, ctx);
		System.out.println(json);
		j = new JSONObject(json);
		System.out.println(j.toString(2));
		assertTrue(json.contains("replaced.txt"));
	}

	@Test
	public void testLog() throws Exception {
		System.out.println(new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(new Date()));
	}
}
