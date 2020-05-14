package de.fzj.unicore.xnjs.ems;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.TimeoutException;

import org.junit.Test;

import de.fzj.unicore.xnjs.ConfigurationSource;
import de.fzj.unicore.xnjs.jsdl.JSDLProcessor;
import de.fzj.unicore.xnjs.util.LogUtil;
import eu.unicore.bugsreporter.annotation.FunctionalTest;
import eu.unicore.security.Client;

public class TestJobControl extends EMSTestBase {

	//jsdl document paths
	private static String 
	d1="src/test/resources/ems/date.jsdl";
	private Client client=new Client();

	@Override
	protected void addProperties(ConfigurationSource cs){
		super.addProperties(cs);
		cs.getProperties().put("XNJS.numberofworkers", "1");
	}

	@Test
	public void testRun() throws Exception {
		Action action=xnjs.makeAction(getJSDLDoc(d1));
		String id=action.getUUID();
		mgr.add(action,client);
		doRun(id);
		action=((BasicManager)mgr).getAction(id);
		assertTrue(action.getResult().isSuccessful());
	}

	@Test
	public void testRun2() throws Exception {
		Action action=xnjs.makeAction(getJSDLDoc(d1));
		String id=action.getUUID();
		mgr.add(action,client);
		doRun(id);
		action=((BasicManager)mgr).getAction(id);
		assertTrue(action.getResult().isSuccessful());
	}

	@Test
	public void testAbort() throws Exception {
		Action action=xnjs.makeAction(getJSDLDoc(d1));
		String id=action.getUUID();
		mgr.add(action,client);
		waitUntilReady(id);
		mgr.abort(id,null);
		waitUntilDone(id);
		Thread.sleep(1000);

		Action a1=((BasicManager)mgr).getActionForUpdate(id);
		assertNotNull(a1);
		System.out.println(a1.getUUID()+" is "+a1.getStatusAsString());
		System.out.println(a1.getResult().getErrorMessage());
		assertEquals("USER_ABORTED",a1.getResult().getStatusString());
		((BasicManager)mgr).doneProcessing(a1);
	}

	@Test
	public void testTimeoutException() throws Exception{
		Action action=xnjs.makeAction(getJSDLDoc(d1));
		String id=action.getUUID();
		MyThread t = new MyThread(id);
		try{
			mgr.add(action,client);
			waitUntilReady(id);
			t.start();
			do{
				Thread.sleep(1000);
			}while(t.a==null);
			try{
				System.out.println("test abort of "+id+" from "+Thread.currentThread().getName());
				mgr.abort(id,client);
				fail("Expected timeout");
			}catch(ExecutionException te){
				boolean OK=te.getCause() instanceof TimeoutException;
				assertTrue(OK);
			}
		}finally{
			t.release();
		}
	}

	public class MyThread extends Thread{
		volatile Action a;
		private String id;
		private volatile boolean stopped = false;

		public MyThread(String id){
			this.id=id;
		}

		public void run(){
			//force a timeout exception
			try{
				lockAction(id);
			}catch(Exception e){
				throw new RuntimeException(e);
			}
			while(!stopped){
				try{
					Thread.sleep(500);
				}catch(InterruptedException ie){}
			}
			System.out.println("Releasing : "+id+" from "+getName());
			// now we can release it
			if(a!=null)((BasicManager)mgr).doneProcessing(a);
		}

		public void lockAction(String id)throws Exception{
			a=((BasicManager)mgr).getActionForUpdate(id);
			if(a!=null){
				System.out.println("Locked : "+id+" from "+getName());
			}
			else{
				throw new IllegalStateException();
			}
		}

		public void release()throws Exception{
			stopped=true;
		}

	}

	@Test
	public void testDestroy() throws Exception {
		Action action=xnjs.makeAction(getJSDLDoc(d1));
		String id=action.getUUID();
		mgr.add(action,client);
		waitUntilReady(id);
		mgr.destroy(id,client);
		//wait a bit and check it is gone
		Thread.sleep(1000);
		try{
			mgr.getStatus(id, client);
			fail();
		}catch(ExecutionException te){
			System.out.println("OK: exception "+LogUtil.createFaultMessage("", te));
		}
	}

	@FunctionalTest(id="testJSDLRestart", 
			description="Tests restarting of jobs")
	@Test
	public void testRestart() throws Exception {
		Action action=xnjs.makeAction(getJSDLDoc(d1));
		String id=action.getUUID();
		mgr.add(action,null);
		doRun(id);
		// restart it
		mgr.restart(id, null);
		waitUntilDone(id);
		Action a=((BasicManager)mgr).getAction(id);
		a.printLogTrace();
		assertTrue(a.getLog().toString().contains("RESTARTING"));
		//print stats
		String timeProfile=JSDLProcessor.getTimeProfile(a.getProcessingContext());
		System.out.println(timeProfile);
	}

}
