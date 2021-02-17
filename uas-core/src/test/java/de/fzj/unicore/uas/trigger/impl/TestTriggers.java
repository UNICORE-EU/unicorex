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
import org.junit.Assert;
import org.junit.Test;
import org.w3.x2005.x08.addressing.EndpointReferenceType;

import de.fzj.unicore.uas.Base;
import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.client.StorageClient;
import de.fzj.unicore.uas.client.StorageFactoryClient;
import de.fzj.unicore.uas.trigger.Rule;
import de.fzj.unicore.uas.trigger.RuleSet;
import de.fzj.unicore.uas.trigger.xnjs.TriggerProcessor;
import de.fzj.unicore.uas.xnjs.XNJSFacade;
import de.fzj.unicore.wsrflite.xmlbeans.WSUtilities;
import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.InternalManager;
import de.fzj.unicore.xnjs.io.IStorageAdapter;
import eu.unicore.security.Client;
import eu.unicore.services.ws.utils.WSServerUtilities;

public class TestTriggers extends Base {

	@Test
	public void testRuleFactory()throws Exception{
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
	
	@Test
	public void testDirectoryScan()throws Exception{
		// create a storage first
		EndpointReferenceType epr=WSServerUtilities.makeEPR(UAS.SMF, "default_storage_factory", kernel);
		StorageFactoryClient smf=new StorageFactoryClient(epr, kernel.getClientConfiguration());
		Map<String,String>settings = new HashMap<String, String>();
		settings.put("trigger.disable","true");
		StorageClient sms=smf.createSMS(null,null,settings,null);
		// write a rule file
		InputStream is=new FileInputStream("src/test/resources/trigger_rules");
		sms.upload(RuleFactory.RULE_FILE_NAME).writeAllData(is);
		
		// write data files
		sms.upload("/dir/test.txt").write("this is a test\n".getBytes());
		sms.upload("/dir/test2.txt").write("this is a test in a subdirectory\n".getBytes());
		
		// triggering won't touch files that are not yet old enough
		Thread.sleep(8000);
		
		
		// setup the scan
		String sID=WSUtilities.extractResourceID(sms.getEPR());
		Client client=new Client();
		client.setAnonymousClient();
		XNJS xnjs=XNJSFacade.get(null, kernel).getXNJS();
		SetupDirectoryScan sds=new SetupDirectoryScan(sID, "/", client, xnjs, -1, 
				new String[]{"/dir/.*txt"}, null, 10, false);
		String actionID=sds.call();
		
		int i=0;
		do{
			Thread.sleep(1000);
			i++;
		}while(!hasRun(xnjs,actionID)&& i<300);
		
		// allow some grace time - processing is async
		Thread.sleep(5000);
		// check the expected outfile is there
		Assert.assertTrue(sms.listProperties("/out/test.txt.md5").getSize()>0);
		Assert.assertTrue(sms.listProperties("/out/test2.txt.md5").getSize()>0);
	}
	
	private boolean hasRun(XNJS xnjs, String actionID)throws Exception{
		Action a=xnjs.get(InternalManager.class).getAction(actionID);
		Long l=(Long)a.getProcessingContext().get(TriggerProcessor.LAST_RUN_TIME);
		return l!=null && l>0 && l<System.currentTimeMillis();
	}

}
