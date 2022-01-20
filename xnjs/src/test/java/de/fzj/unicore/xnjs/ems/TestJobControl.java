package de.fzj.unicore.xnjs.ems;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import de.fzj.unicore.xnjs.ConfigurationSource;
import de.fzj.unicore.xnjs.ems.processors.JobProcessor;
import de.fzj.unicore.xnjs.util.LogUtil;
import eu.unicore.security.Client;

public class TestJobControl extends EMSTestBase {

	//jsdl document paths
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
	}

	@Test
	public void testDestroy() throws Exception {
		Action action=xnjs.makeAction(loadJSONObject(d1));
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

	@Test
	public void testRestart() throws Exception {
		Action action=xnjs.makeAction(loadJSONObject(d1));
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
		String timeProfile = JobProcessor.getTimeProfile(a.getProcessingContext());
		System.out.println(timeProfile);
	}

}
