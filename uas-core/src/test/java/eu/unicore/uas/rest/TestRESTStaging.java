package eu.unicore.uas.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.CoreClient;
import eu.unicore.client.core.JobClient;
import eu.unicore.client.core.SiteClient;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.services.restclient.UsernamePassword;
import eu.unicore.uas.SecuredBase;
import eu.unicore.uas.UASProperties;

public class TestRESTStaging extends SecuredBase {
	
	@BeforeAll
	public static void createTestFile() throws Exception{
		try{
			FileUtils.write(new File("target/unicorex-test/test1.txt"), "test data", "UTF-8");
			FileUtils.write(new File("target/unicorex-test/test2.dat"), "more test data", "UTF-8");
			FileUtils.write(new File("target/unicorex-test/test3.dat"), "even more test data", "UTF-8");
		}catch(IOException e){throw new RuntimeException(e);}
	}

	@Test
	public void testStageIn() throws Exception {
		UASProperties cfg = kernel.getAttribute(UASProperties.class);
		for(String noOpt: new String[]{"true", "false"}){
			System.out.println("Forcing remote transfer: "+noOpt);
			cfg.setProperty(UASProperties.SMS_TRANSFER_FORCEREMOTE, noOpt);
			doStageIn();
		}
	}
	
	private void doStageIn()throws Exception {	
		String url = kernel.getContainerProperties().getContainerURL()+"/rest/core";
		IAuthCallback auth = new UsernamePassword("demouser", "test123");
		CoreClient core = new CoreClient(new Endpoint(url), kernel.getClientConfiguration(), auth);
		SiteClient site = core.getSiteFactoryClient().getOrCreateSite();
		JSONArray imports = new JSONArray();
		imports.put(new JSONObject("{"
				+ "From: 'https://localhost:65321/rest/core/storages/WORK/files/test1.txt',"
				+ "To: test1.txt"
				+ "}"));
		imports.put(new JSONObject("{"
				+ "From: 'https://localhost:65321/rest/core/storages/WORK/files/*.dat',"
				+ "To: '.'"
				+ "}"));
		JSONObject task = new JSONObject();
		task.put("Executable", "ls");
		task.put("Imports", imports);
		JobClient job = site.submitJob(task);
		
		System.out.println("*** new job: "+job.getEndpoint().getUrl());
		System.out.println(job.getProperties().toString(2));
		while(!job.isFinished()) {
			Thread.sleep(1000);
		}
		System.out.println(job.getProperties().toString(2));
		String wd = job.getWorkingDirectory().getProperties().getString("mountPoint");
		
		String[] files = { "test1.txt", "test2.dat", "test3.dat" };
		for(String f: files) {
			File i1 = new File(wd+"/"+f);
			assertTrue(i1.exists());
			assertEquals(FileUtils.checksumCRC32(new File("target/unicorex-test/"+f)),
					FileUtils.checksumCRC32(i1));
		}
	}

	@Test
	public void testStageOut() throws Exception {
		UASProperties cfg = kernel.getAttribute(UASProperties.class);
		for(String noOpt: new String[]{"true", "false"}){
			System.out.println("Forcing remote transfer: "+noOpt);
			cfg.setProperty(UASProperties.SMS_TRANSFER_FORCEREMOTE, noOpt);
			doStageOut();
		}
	}

	private void doStageOut() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest/core";
		IAuthCallback auth = new UsernamePassword("demouser", "test123");
		CoreClient core = new CoreClient(new Endpoint(url), kernel.getClientConfiguration(), auth);
		SiteClient site = core.getSiteFactoryClient().getOrCreateSite();
		JSONArray xports = new JSONArray();
		xports.put(new JSONObject("{"
				+ "To: 'https://localhost:65321/rest/core/storages/WORK/files/out/stdout',"
				+ "From: stdout"
				+ "}"));
		JSONObject task = new JSONObject();
		task.put("Executable", "echo");
		JSONArray args = new JSONArray("['test data']");
		task.put("Arguments", args);
		task.put("Exports", xports);
		JobClient job = site.submitJob(task);
		
		System.out.println("*** new job: "+job.getEndpoint().getUrl());
		System.out.println(job.getProperties().toString(2));
		while(!job.isFinished()) {
			Thread.sleep(1000);
		}
		System.out.println(job.getProperties().toString(2));
		String wd = job.getWorkingDirectory().getProperties().getString("mountPoint");
		
		String[] files = { "stdout" };
		for(String f: files) {
			File i1 = new File(wd+"/"+f);
			assertTrue(i1.exists());
			assertEquals(FileUtils.checksumCRC32(new File("target/unicorex-test/out/"+f)),
					FileUtils.checksumCRC32(i1));
		}
	}
	
}
