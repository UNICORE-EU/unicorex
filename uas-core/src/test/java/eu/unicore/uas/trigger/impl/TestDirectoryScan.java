package eu.unicore.uas.trigger.impl;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Set;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.FileList;
import eu.unicore.client.core.FileList.FileListEntry;
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
				+"/rest/core/storagefactories/DEFAULT";
		StorageFactoryClient smf = new StorageFactoryClient(new Endpoint(url), 
				kernel.getClientConfiguration(), null);
		StorageClient sms = smf.createStorage();
		// write a rule file
		try(InputStream is=new FileInputStream("src/test/resources/trigger_rules")){
			sms.upload(RuleFactory.RULE_FILE_NAME).write(is);
		}
		// write data files
		sms.upload("/dir/test.txt").write("test1\n".getBytes());
		sms.upload("/dir/test2.txt").write("test2\n".getBytes());
		
		// triggering won't touch files that are not yet old enough
		Thread.sleep(10500);
		
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

		Set<String> ids = getActionIDs(xnjs, actionID);
		for(String id: ids) {
			i=0;
			do{
				Thread.sleep(1000);
				i++;
			}while(!hasRun(xnjs,id)&& i<30);
		}
		// check the expected outfile is there
		assertTrue(sms.stat("/out/test.txt.md5").size>0);
		assertTrue(sms.stat("/out/test2.txt.md5").size>0);
		
		// check notifications were sent
		assertTrue(Notifications.notifications.size()>0);
		System.out.println("Notifications:");
		for(JSONObject n: Notifications.notifications) {
			System.out.println(n.toString(2));
		}
		
		// check logfile(s)
		FileList logfiles = sms.ls(TriggerProcessor.logDirectory);
		List<FileListEntry> fList = logfiles.list();
		assertTrue(fList.size()>0);
		for(FileListEntry e: logfiles.list()) {
			System.out.println(e);
		}
		System.out.println(sms.getProperties().toString(2));
	}
	
	
	@Test
	public void testDirectoryScanSharedMode()throws Exception{
		String url = kernel.getContainerProperties().getContainerURL()
				+"/rest/core/storagefactories/DEFAULT";
		StorageFactoryClient smf = new StorageFactoryClient(new Endpoint(url), 
				kernel.getClientConfiguration(), null);
		StorageClient sms = smf.createStorage();
		// write toplevel rule file
		String tlRule = "{'DirectoryScan':{'IncludeDirs':['scan']}}";
		try(InputStream is = new ByteArrayInputStream(tlRule.getBytes("UTF-8"))){
			sms.upload(RuleFactory.RULE_FILE_NAME).write(is);
		}
		// write per-dir rule file
		try(InputStream is=new FileInputStream("src/test/resources/trigger_rules")){
			sms.upload("/scan/"+RuleFactory.RULE_FILE_NAME).write(is);
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
		
		Set<String> ids = getActionIDs(xnjs, actionID);
		for(String id: ids) {
			i=0;
			do{
				Thread.sleep(1000);
				i++;
			}while(!hasRun(xnjs,id)&& i<30);
		}
		// check the expected outfile is there
		assertTrue(sms.stat("/out/test.txt.md5").size>0);
		assertTrue(sms.stat("/out/test2.txt.md5").size>0);

		// check notifications were sent
		assertTrue(Notifications.notifications.size()>0);
		System.out.println("Notifications:");
		for(JSONObject n: Notifications.notifications) {
			System.out.println(n.toString(2));
		}

		// check logfile(s)
		FileList logfiles = sms.ls(TriggerProcessor.logDirectory);
		for(FileListEntry e: logfiles.list()) {
			System.out.println(e);
			if(e.size>0) {
				ByteArrayOutputStream os = new ByteArrayOutputStream();
				sms.download(e.path).readFully(os);
				System.out.println("Trigger run log\n***************\n\n"+os.toString("UTF-8"));
			}
		}
	}
	
	private boolean hasRun(XNJS xnjs, String actionID)throws Exception{
		Action a=xnjs.get(InternalManager.class).getAction(actionID);
		Long l = a.getProcessingContext().getAs(TriggerProcessor.LAST_RUN_TIME, Long.class);
		return l!=null && l>0 && l<System.currentTimeMillis();
	}
	
	
	@SuppressWarnings("unchecked")
	private Set<String> getActionIDs(XNJS xnjs, String actionID)throws Exception{
		Action a=xnjs.get(InternalManager.class).getAction(actionID);
		return a.getProcessingContext().getAs(TriggerProcessor.ACTION_IDS, Set.class);
	}

}
