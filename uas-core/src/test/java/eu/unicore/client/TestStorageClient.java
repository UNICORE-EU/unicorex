package eu.unicore.client;

import java.util.List;

import org.json.JSONObject;
import org.junit.Test;

import de.fzj.unicore.uas.Base;
import eu.unicore.client.core.CoreClient;
import eu.unicore.client.core.FileList;
import eu.unicore.client.core.JobClient;
import eu.unicore.client.core.SiteClient;
import eu.unicore.client.core.SiteFactoryClient;
import eu.unicore.client.core.StorageClient;
import eu.unicore.client.core.FileList.FileListEntry;
import eu.unicore.services.rest.client.IAuthCallback;
import eu.unicore.services.rest.client.UsernamePassword;
import org.junit.Assert;

public class TestStorageClient extends Base {

	@Test
	public void testStorageClient() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest/core/storages/WORK";
		StorageClient storage = new StorageClient(new Endpoint(url), kernel.getClientConfiguration(), null);
		System.out.println(storage.getProperties().toString(2));
		for(FileListEntry e: storage.getFiles(".").list(0, 1000)) {
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
		
		FileList fl = usp.getFiles("/");
		List<FileListEntry> files = fl.list(0,50);
		for(FileListEntry fle: files)System.out.println(fle);
		Assert.assertEquals(4, files.size());
		files = fl.list(1,50);
		Assert.assertEquals(3, files.size());
		files = fl.list(0,2);
		Assert.assertEquals(2, files.size());
	}
	
	protected void waitForFinish(JobClient jc) throws Exception {
		int c=0;
		while(c<20 && !jc.isFinished()){
			Thread.sleep(1000);
			c++;
		}
	}
}
