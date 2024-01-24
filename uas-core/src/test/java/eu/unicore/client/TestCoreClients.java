package eu.unicore.client;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import eu.unicore.client.core.CoreClient;
import eu.unicore.client.core.EnumerationClient;
import eu.unicore.client.core.FileList;
import eu.unicore.client.core.JobClient;
import eu.unicore.client.core.SiteClient;
import eu.unicore.client.core.SiteFactoryClient;
import eu.unicore.client.core.StorageClient;
import eu.unicore.client.core.FileList.FileListEntry;
import eu.unicore.client.registry.RegistryClient;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.services.rest.client.UsernamePassword;
import eu.unicore.uas.Base;

public class TestCoreClients extends Base {

	@Test
	public void testClientProperties() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest";
		String resource = url+"/core";
		System.out.println("Accessing "+resource);
		Endpoint ep = new Endpoint(resource);
		IAuthCallback auth = new UsernamePassword("demouser", "test123");
		CoreClient client = new CoreClient(ep, kernel.getClientConfiguration(), auth);
		System.out.println("Client info: " +client.getClientInfo().toString(2));
	}


	@Test
	public void testRegistryProperties() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest";
		String resource = url+"/registries/default_registry";
		System.out.println("Accessing "+resource);
		
		RegistryClient client = new RegistryClient(resource, kernel.getClientConfiguration(), null);
		
		List<Endpoint> entries = client.listEntries();
		
		boolean found = false;
		for (int i=0; i<entries.size(); i++){
			Endpoint ep = entries.get(i);
			System.out.println(ep.getUrl()+" : "+ep.getInterfaceName());
			if(ep.getInterfaceName().equals("CoreServices")){
				found = true;
				break;
			}
		}
		assertTrue("CoreServices not in registry", found);
		
		Endpoint core = client.listEntries(
				new RegistryClient.ServiceTypeFilter("CoreServices")).get(0);
		
		System.out.println("Found: " +core.getUrl());
	}

	@Test
	public void testStorageClient() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest/core/storages/WORK";
		StorageClient storage = new StorageClient(new Endpoint(url), kernel.getClientConfiguration(), null);
		System.out.println(storage.getProperties().toString(2));
		for(FileListEntry e: storage.ls(".").list(0, 1000)) {
			System.out.println(e);
		}
		System.out.println(storage.stat("/"));
		System.out.println("MP: "+storage.getMountPoint());
		System.out.println("FS: "+storage.getFileSystemDescription());
	}
	
	@Test
	public void testWorkingDir() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest/core";
		System.out.println("Accessing "+url);
		Endpoint ep = new Endpoint(url);
		IAuthCallback auth = new UsernamePassword("demouser", "test123");
		CoreClient client = new CoreClient(ep, kernel.getClientConfiguration(), auth);
		
		
		SiteFactoryClient sfc = client.getSiteFactoryClient();
		SiteClient sc = sfc.getOrCreateSite();
		JSONObject job = new JSONObject();
		job.put("ApplicationName", "Date");
		JobClient jc = sc.submitJob(job);
		jc.setUpdateInterval(-1);
		waitForFinish(jc);
		
		StorageClient usp = jc.getWorkingDirectory();
		System.out.println(usp.getProperties().toString(2));
		
		FileList fl = usp.ls("/");
		List<FileListEntry> files = fl.list(0,50);
		for(FileListEntry fle: files)System.out.println(fle);
		Assert.assertEquals(4, files.size());
		files = fl.list(1,50);
		Assert.assertEquals(3, files.size());
		files = fl.list(0,2);
		Assert.assertEquals(2, files.size());
	}

	@Test
	public void testSubmitJob() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest";
		String resource = url+"/core";
		System.out.println("Accessing "+resource);
		Endpoint ep = new Endpoint(resource);
		IAuthCallback auth = new UsernamePassword("demouser", "test123");
		CoreClient client = new CoreClient(ep, kernel.getClientConfiguration(), auth);
		
		SiteFactoryClient sfc = client.getSiteFactoryClient();
		SiteClient sc = sfc.getOrCreateSite();
		JSONObject job = new JSONObject();
		job.put("ApplicationName", "Date");
		sc.submitJob(job);
		job.put("Tags", new JSONArray("[\"date\"]"));
		JobClient jc = sc.submitJob(job);
		jc.setUpdateInterval(-1);
		waitForFinish(jc);
		System.out.println("JOB LOG: "+jc.getLog());
		EnumerationClient jobList = sc.getJobsList();
		System.out.println(jobList.getProperties().toString(2));
		
		List<String> jL = jobList.getUrls(0, 100, (String[])null);
		Assert.assertTrue(jL.size()>0);
		List<String> jL2 = jobList.getUrls(0, 100, "date");
		Assert.assertEquals(1, jL2.size());

	}
	
	protected void waitForFinish(JobClient jc) throws Exception {
		int c=0;
		while(c<20 && !jc.isFinished()){
			Thread.sleep(1000);
			c++;
		}
	}
}
