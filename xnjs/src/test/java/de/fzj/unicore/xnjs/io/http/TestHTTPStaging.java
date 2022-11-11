package de.fzj.unicore.xnjs.io.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;

import org.apache.hc.client5.http.classic.HttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.fzj.unicore.xnjs.ems.ActionResult;
import de.fzj.unicore.xnjs.ems.BasicManager;
import de.fzj.unicore.xnjs.ems.EMSTestBase;
import de.fzj.unicore.xnjs.io.impl.HTTPFileDownload;
import de.fzj.unicore.xnjs.io.impl.UsernamePassword;

public class TestHTTPStaging extends EMSTestBase {

	boolean gotCall=false;
	String answer="hello world!";
	FakeServer server;
	
	@Before
	public void startFakeHttpServer()throws Exception{
		server=new FakeServer();
		server.setAnswer(answer);
		server.start();
		Thread.sleep(1000);
	}

	@After
	public void stopServer()throws Exception{
		try{
			server.stop();
			Thread.sleep(1000);
		}catch(Exception ex){}
	}
	
	@Test
	public void testHTTPDownload()throws Exception{
		String source=server.getURI();
		File tFile=new File("target","xnjs_test"+System.currentTimeMillis());
		String target=tFile.getName();
		server.setRequireAuth(true);
		UsernamePassword up = new UsernamePassword("user", "test123");
		HTTPFileDownload hfd=new HTTPFileDownload(null,"target",source,target,xnjs,up);
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
			String id=(String)mgr.add(xnjs.makeAction(makeJob()),null);
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
			String id=(String)mgr.add(xnjs.makeAction(makeJob()),null);
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
			server.setVerySlowMode(true);
			String id=(String)mgr.add(xnjs.makeAction(makeJob()),null);
			assertNotNull(id);
			Thread.sleep(4000);
			System.out.println(mgr.abort(id, null));
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
	
}
