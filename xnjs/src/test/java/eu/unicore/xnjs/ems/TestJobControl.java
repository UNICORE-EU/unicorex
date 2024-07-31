package eu.unicore.xnjs.ems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collection;

import org.junit.jupiter.api.Test;

import eu.unicore.security.Client;
import eu.unicore.xnjs.ConfigurationSource;
import eu.unicore.xnjs.ems.processors.JobProcessor;
import eu.unicore.xnjs.tsi.IExecution;
import eu.unicore.xnjs.util.LogUtil;

public class TestJobControl extends EMSTestBase {

	private static String 
	d1="src/test/resources/json/date.json";
	private Client client=new Client();

	@Override
	protected void addProperties(ConfigurationSource cs){
		super.addProperties(cs);
		cs.getProperties().put("XNJS.numberofworkers", "1");
	}

	@Test
	public void testRun() throws Exception {
		Action action=xnjs.makeAction(loadJSONObject(d1));
		String id=action.getUUID();
		mgr.add(action,client);
		doRun(id);
		action=((BasicManager)mgr).getAction(id);
		assertTrue(action.getResult().isSuccessful());
		
		// notifications
		MockChangeListener m = (MockChangeListener)xnjs.get(ActionStateChangeListener.class);
		assertTrue(m.getOrCreate(id).get()>0);
		Collection<String> ids = mgr.list(client);
		assertTrue(ids.size()>0);
	}

	@Test
	public void testAbort() throws Exception {
		Action action=xnjs.makeAction(loadJSONObject(d1));
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
		assertFalse(xnjs.get(IExecution.class).isBeingTracked(action));
	}

	@Test
	public void testDestroy() throws Exception {
		Action action=xnjs.makeAction(loadJSONObject(d1));
		String id=action.getUUID();
		mgr.add(action,client);
		waitUntilReady(id);
		mgr.destroy(id,client);
		Thread.sleep(1000);
		try{
			mgr.getStatus(id, client);
			fail();
		}catch(ExecutionException te){
			assertTrue(te.getMessage().contains("No such action"));
			System.out.println("OK: exception "+LogUtil.createFaultMessage("", te));
		}
	}

	@Test
	public void testDestroyDone() throws Exception {
		Action action=xnjs.makeAction(loadJSONObject(d1));
		String id=action.getUUID();
		mgr.add(action,client);
		doRun(id);
		waitUntilDone(id);
		mgr.destroy(id,client);
		Thread.sleep(1000);
		try{
			mgr.getStatus(id, client);
			fail();
		}catch(ExecutionException te){
			assertTrue(te.getMessage().contains("No such action"));
			System.out.println("OK: exception "+LogUtil.createFaultMessage("", te));
		}
	}

	@Test
	public void testRestart() throws Exception {
		Action action=xnjs.makeAction(loadJSONObject(d1));
		String id=action.getUUID();
		mgr.add(action,null);
		doRun(id);
		mgr.restart(id, null);
		waitUntilDone(id);
		Action a=((BasicManager)mgr).getAction(id);
		a.printLogTrace();
		assertTrue(a.getLog().toString().contains("RESTARTING"));
		String timeProfile = JobProcessor.getTimeProfile(a.getProcessingContext());
		System.out.println(timeProfile);
	}

	@Test
	public void testPause() throws Exception {
		Action action=xnjs.makeAction(loadJSONObject(d1));
		String id=action.getUUID();
		mgr.add(action,null);
		mgr.pause(id, client);
		Thread.sleep(200);
		mgr.getStatus(id, client);
		mgr.resume(id, client);
		doRun(id);
		waitUntilDone(id);
	}

}
