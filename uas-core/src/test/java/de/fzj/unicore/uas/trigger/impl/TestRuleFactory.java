package de.fzj.unicore.uas.trigger.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.junit.Test;

import de.fzj.unicore.uas.trigger.Rule;
import de.fzj.unicore.uas.trigger.RuleSet;
import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.io.IStorageAdapter;
import eu.unicore.security.Client;

public class TestRuleFactory {

	@Test
	public void test1()throws Exception{
		InputStream is=new FileInputStream("src/test/resources/testing_rules");
		RuleFactory rf=new RuleFactory(null, "test123");
		RuleSet rules=null;
		try{
			rules=rf.readRules(is);
		}finally{
			is.close();
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
		Map<String,String>ctx = new HashMap<String, String>();
		ctx.put("UC_FILE_NAME","replaced.txt");
		json = new BaseAction() {
			@Override
			public void fire(IStorageAdapter storage, String filePath, Client client,
					XNJS xnjs) throws Exception {
			}
		}.expandVariables(json, ctx);
		System.out.println(json);
		j = new JSONObject(json);
		System.out.println(j.toString(2));
		assertTrue(json.contains("replaced.txt"));
	}
	
	
	@Test
	public void testReplaceMany() throws Exception {
		String json = FileUtils.readFileToString(new File("src/test/resources/testing_rules"), "UTF-8");
		JSONObject j = new JSONObject(json);
		assertNotNull(j);
		Map<String,String>ctx = new HashMap<String, String>();
		ctx.put("UC_FILE_NAME","replaced.txt");
		BaseAction a = new BaseAction() {
			@Override
			public void fire(IStorageAdapter storage, String filePath, Client client,
					XNJS xnjs) throws Exception {
			}
		};
		String n="";
		long l=0;
		long start = System.currentTimeMillis();
		for(int x = 0; x<1000;x++){
			n = a.expandVariables(json, ctx);
			l+=n.hashCode();
		}
		long duration = System.currentTimeMillis() - start;
		System.out.println("Took "+duration+" ms.");
		System.out.println(n+l);
		j = new JSONObject(json);
	}
	
}
