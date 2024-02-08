package eu.unicore.uas.trigger.impl;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.StorageClient;
import eu.unicore.client.core.StorageFactoryClient;
import eu.unicore.security.Client;
import eu.unicore.uas.Base;
import eu.unicore.uas.notification.Notifications;
import eu.unicore.uas.trigger.xnjs.TriggerProcessor;
import eu.unicore.uas.xnjs.XNJSFacade;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.ems.Action;
import eu.unicore.xnjs.ems.InternalManager;

public class TestDirectoryScan extends Base {
	
	@Test
	public void testDirectoryScan()throws Exception{
		String url = kernel.getContainerProperties().getContainerURL()
				+"/rest/core/storagefactories/default_storage_factory";
		StorageFactoryClient smf = new StorageFactoryClient(new Endpoint(url), 
				kernel.getClientConfiguration(), null);
		StorageClient sms = smf.createStorage();
		// write a rule file
		try(InputStream is=new FileInputStream("src/test/resources/trigger_rules")){
			sms.upload(RuleFactory.RULE_FILE_NAME).writeAllData(is);
		}
		// write data files
		sms.upload("/dir/test.txt").write("test1\n".getBytes());
		sms.upload("/dir/test2.txt").write("test2\n".getBytes());
		
		// triggering won't touch files that are not yet old enough
		Thread.sleep(8000);
		
		// setup the scan
		String sID =  new File(new URI(sms.getEndpoint().getUrl()).getPath()).getName();
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
		}while(!hasRun(xnjs,actionID)&& i<30);
		
		// allow some grace time - processing is async
		Thread.sleep(10000);
		// check the expected outfile is there
		Assert.assertTrue(sms.stat("/out/test.txt.md5").size>0);
		Assert.assertTrue(sms.stat("/out/test2.txt.md5").size>0);
		
		
		assertTrue(Notifications.notifications.size()>0);
		System.out.println("Notifications:");
		for(JSONObject n: Notifications.notifications) {
			System.out.println(n.toString(2));
		}
	}
	
	
	@Test
	public void testDirectoryScanSharedMode()throws Exception{
		String url = kernel.getContainerProperties().getContainerURL()
				+"/rest/core/storagefactories/default_storage_factory";
		StorageFactoryClient smf = new StorageFactoryClient(new Endpoint(url), 
				kernel.getClientConfiguration(), null);
		StorageClient sms = smf.createStorage();
		// write toplevel rule file
		String tlRule = "{'DirectoryScan':{'IncludeDirs':['scan'],'Interval':10}}";
		try(InputStream is = new ByteArrayInputStream(tlRule.getBytes("UTF-8"))){
			sms.upload(RuleFactory.RULE_FILE_NAME).writeAllData(is);
		}
		// write per-dir rule file
		try(InputStream is=new FileInputStream("src/test/resources/trigger_rules")){
			sms.upload("/scan/"+RuleFactory.RULE_FILE_NAME).writeAllData(is);
		}
		// write data files
		sms.upload("/scan/dir/test.txt").write("test1\n".getBytes());
		sms.upload("/scan/dir/test2.txt").write("test2\n".getBytes());
		
		// triggering won't touch files that are not yet old enough
		Thread.sleep(10000);
		
		// setup the scan
		String sID =  new File(new URI(sms.getEndpoint().getUrl()).getPath()).getName();
		Client client=new Client();
		client.setAnonymousClient();
		XNJS xnjs=XNJSFacade.get(null, kernel).getXNJS();
		SetupDirectoryScan sds=new SetupDirectoryScan(sID, "/", client, xnjs, -1, 
				new String[]{"/scan/"}, null, 10, true);
		String actionID=sds.call();
		
		int i=0;
		do{
			Thread.sleep(1000);
			i++;
		}while(!hasRun(xnjs,actionID)&& i<30);
		
		// allow some grace time - processing is async
		Thread.sleep(10000);
		// check the expected outfile is there
		Assert.assertTrue(sms.stat("/out/test.txt.md5").size>0);
		Assert.assertTrue(sms.stat("/out/test2.txt.md5").size>0);
	}
	
	private boolean hasRun(XNJS xnjs, String actionID)throws Exception{
		Action a=xnjs.get(InternalManager.class).getAction(actionID);
		Long l = a.getProcessingContext().getAs(TriggerProcessor.LAST_RUN_TIME, Long.class);
		return l!=null && l>0 && l<System.currentTimeMillis();
	}

}
