package de.fzj.unicore.xnjs.io.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;

import org.apache.http.client.HttpClient;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.CreationFlagEnumeration;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.DataStagingType;
import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionDocument;
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
		super.tearDown();
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
			String id=(String)mgr.add(xnjs.makeAction(makeJSDL()),null);
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
			String id=(String)mgr.add(xnjs.makeAction(makeJSDL()),null);
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
			String id=(String)mgr.add(xnjs.makeAction(makeJSDL()),null);
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
	
	private JobDefinitionDocument makeJSDL(){
		JobDefinitionDocument j=JobDefinitionDocument.Factory.newInstance();
		j.addNewJobDefinition().addNewJobDescription().addNewApplication().setApplicationName("Date");
		DataStagingType d=j.getJobDefinition().getJobDescription().addNewDataStaging();
		d.setFileName("infile");
		d.setCreationFlag(CreationFlagEnumeration.OVERWRITE);
		d.addNewSource().setURI(server.getURI());
		DataStagingType d1=j.getJobDefinition().getJobDescription().addNewDataStaging();
		d1.setFileName("infile2");
		d1.setCreationFlag(CreationFlagEnumeration.OVERWRITE);
		d1.addNewSource().setURI(server.getURI());
		return j;
	}
	
}
