package de.fzj.unicore.uas.trigger.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import de.fzj.unicore.uas.Base;
import de.fzj.unicore.uas.trigger.xnjs.TriggerProcessor;
import de.fzj.unicore.uas.xnjs.XNJSFacade;
import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.InternalManager;
import eu.unicore.client.Endpoint;
import eu.unicore.client.core.StorageClient;
import eu.unicore.client.core.StorageFactoryClient;
import eu.unicore.security.Client;

public class TestDirectoryScan extends Base {
	
	@Test
	public void testDirectoryScan()throws Exception{
		String url = kernel.getContainerProperties().getContainerURL()
				+"/rest/core/storagefactories/default_storage_factory";
		StorageFactoryClient smf = new StorageFactoryClient(new Endpoint(url), 
				kernel.getClientConfiguration(), null);
		Map<String,String>settings = new HashMap<>();
		settings.put("trigger.disable","true");
		StorageClient sms = smf.createStorage(null, null, settings, null);
		// write a rule file
		try(InputStream is=new FileInputStream("src/test/resources/trigger_rules")){
			sms.upload(RuleFactory.RULE_FILE_NAME).writeAllData(is);
		}
		// write data files
		sms.upload("/dir/test.txt").write("this is a test\n".getBytes());
		sms.upload("/dir/test2.txt").write("this is a test in a subdirectory\n".getBytes());
		
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
		Thread.sleep(5000);
		// check the expected outfile is there
		Assert.assertTrue(sms.stat("/out/test.txt.md5").size>0);
		Assert.assertTrue(sms.stat("/out/test2.txt.md5").size>0);
	}
	
	private boolean hasRun(XNJS xnjs, String actionID)throws Exception{
		Action a=xnjs.get(InternalManager.class).getAction(actionID);
		Long l=(Long)a.getProcessingContext().get(TriggerProcessor.LAST_RUN_TIME);
		return l!=null && l>0 && l<System.currentTimeMillis();
	}

}
