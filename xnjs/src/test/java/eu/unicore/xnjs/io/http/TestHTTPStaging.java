package eu.unicore.xnjs.io.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.hc.client5.http.classic.HttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import eu.unicore.persist.util.UUID;
import eu.unicore.security.Client;
import eu.unicore.xnjs.ems.ActionResult;
import eu.unicore.xnjs.ems.BasicManager;
import eu.unicore.xnjs.ems.EMSTestBase;
import eu.unicore.xnjs.io.IFileTransferEngine;
import eu.unicore.xnjs.io.impl.HTTPFileDownload;
import eu.unicore.xnjs.io.impl.UsernamePassword;

public class TestHTTPStaging extends EMSTestBase {

	boolean gotCall=false;
	String answer="hello world!";
	FakeServer server;
	
	@BeforeEach
	public void startFakeHttpServer()throws Exception{
		server=new FakeServer();
		server.setAnswer(answer);
		server.start();
		Thread.sleep(1000);
	}

	@AfterEach
	public void stopServer()throws Exception{
		try{
			server.stop();
			Thread.sleep(1000);
		}catch(Exception ex){}
	}
	
	@Test
	public void testHTTPDownload()throws Exception{
		String source=server.getURI();
		String target="xnjs_test"+System.currentTimeMillis()+"/test-http";
		File tFile=new File("target", target);
		server.setRequireAuth(true);
		UsernamePassword up = new UsernamePassword("user", "test123");
		HTTPFileDownload hfd=new HTTPFileDownload(createClient(),"target",source,target,xnjs,up);
		hfd.run();
		long bytes=hfd.getInfo().getTransferredBytes();
		assertEquals(answer.length(),bytes);
		assertEquals(bytes,tFile.length());
		assertNotNull(server.getLastAuthNHeader());
		server.setRequireAuth(false);
	}
	
	@Test
	public void testRunJobWithStagein()throws Exception{
		BasicManager mgr=(BasicManager)internalMgr;
		try{
			String id=(String)mgr.add(xnjs.makeAction(makeJob()), createClient());
			assertNotNull(id);
			doRun(id);
			assertSuccessful(id);
		}catch(Exception e){
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testRunJobWithSlowStagein()throws Exception{
		BasicManager mgr=(BasicManager)internalMgr;
		try{
			server.setVerySlowMode(true);
			String id=(String)mgr.add(xnjs.makeAction(makeJob()), createClient());
			assertNotNull(id);
			doRun(id);
			assertSuccessful(id);
		}catch(Exception e){
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testAbortJobWithStagein()throws Exception{
		BasicManager mgr=(BasicManager)internalMgr;
		try{
			Client cl = createClient(); 
			server.setVerySlowMode(true);
			String id=(String)mgr.add(xnjs.makeAction(makeJob()), cl);
			assertNotNull(id);
			Thread.sleep(4000);
			System.out.println(mgr.abort(id, cl));
			waitUntilDone(id);
			assertEquals(ActionResult.USER_ABORTED,mgr.getAction(id).getResult().getStatusCode());
			Thread.sleep(2000);
		}catch(Exception e){
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testSimpleConnectionFactory(){
		HttpClient c = new SimpleConnectionFactory().getConnection("http://www.fz-juelich.de", null);
		assertNotNull(c);
		HttpClient c1 = new SimpleConnectionFactory().getConnection("http://www.fz-juelich.de", null);
		assertNotNull(c1);
		c = new SimpleConnectionFactory().getConnection("http://www.fz-juelich.de", null);
		assertNotNull(c);
	}
	
	
	@Test
	public void testHttpFTSExport() throws Exception {
		server.waitForContent = true;
		File tFile=new File("target","xnjs_test"+System.currentTimeMillis());
		String content = "this is a test";
		FileUtils.write(tFile, content, "UTF-8");
		JSONObject j = new JSONObject();
		j.put("file", tFile.getName());
		j.put("target", server.getURI());
		j.put("workdir", new File("target").getAbsolutePath());
		JSONObject creds = new JSONObject();
		creds.put("Username", "demouser");
		creds.put("Password", "test123");
		j.put("credentials", creds);
		IFileTransferEngine e = xnjs.get(IFileTransferEngine.class);
		assertNotNull(e);
		String id=(String)mgr.add(xnjs.makeAction(j, "FTS", UUID.newUniqueID()), createClient());
		waitUntilDone(id);
		mgr.getAction(id).printLogTrace();
		System.out.println(mgr.getAction(id).getResult().toString());
		assertNotNull(server.getLastAuthNHeader());
		System.out.println(server.getLastAuthNHeader());
		System.out.println(server.getLastRequest());
		assertTrue(server.getLastRequest().contains(content));
	}
	
	@Test
	public void testHttpFTSImport() throws Exception {
		File tFile=new File("target","xnjs_test"+System.currentTimeMillis());
		JSONObject j = new JSONObject();
		j.put("file", tFile.getName());
		j.put("source", server.getURI());
		j.put("workdir", new File("target").getAbsolutePath());
		IFileTransferEngine e = xnjs.get(IFileTransferEngine.class);
		assertNotNull(e);
		String id = (String)mgr.add(xnjs.makeAction(j, "FTS",
				UUID.newUniqueID()), createClient());
		waitUntilDone(id);
		mgr.getAction(id).printLogTrace();
		System.out.println(mgr.getAction(id).getResult().toString());
		System.out.println(server.getLastRequest());
		assertEquals(FileUtils.readFileToString(tFile, "UTF-8"), answer);
	}
	
	private JSONObject makeJob() throws JSONException {
		JSONObject j = new JSONObject();
		j.put("ApplicationName", "Date");
		JSONArray in = new JSONArray();
		JSONObject d = new JSONObject();
		d.put("To", "infile");
		d.put("From", server.getURI());
		JSONObject d1 = new JSONObject();
		d1.put("To", "infile2");
		d1.put("From", server.getURI());
		in.put(d);
		in.put(d1);
		j.put("Imports", in);
		return j;
	}
	
	//timeout in seconds
	protected int getTimeOut(){
		return 6000;
	}
}
