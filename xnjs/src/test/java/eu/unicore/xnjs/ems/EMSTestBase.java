package eu.unicore.xnjs.ems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;

import com.google.inject.AbstractModule;

import eu.unicore.security.Client;
import eu.unicore.security.SecurityTokens;
import eu.unicore.security.Xlogin;
import eu.unicore.xnjs.XNJSTestBase;
import eu.unicore.xnjs.persistence.IActionStoreFactory;
import eu.unicore.xnjs.persistence.JDBCActionStoreFactory;

/**
 * setup tests of the core ems processing
 */
public abstract class EMSTestBase extends XNJSTestBase {

	protected Manager mgr;

	protected InternalManager internalMgr;

	@BeforeEach
	public void setUp2() throws Exception {
		internalMgr = xnjs.get(InternalManager.class);
		mgr = xnjs.get(Manager.class);
	}

	@Override
	protected AbstractModule getPersistenceModule(){
		return new AbstractModule() {
			@Override
			protected void configure() {
				bind(IActionStoreFactory.class).to(JDBCActionStoreFactory.class);
			}
		};
	}

	protected JSONObject loadJSONObject(String name) throws Exception {
		return new JSONObject(FileUtils.readFileToString(new File(name), "UTF-8"));
	}

	/**
	 * wait until the specified action becomes "ready" with a 60 seconds timeout
	 * @param actionID
	 * @throws Exception
	 */
	protected void waitUntilReady(String actionID)throws Exception{
		assertNotNull(actionID);
		int count=0;
		int status=0;
		do{
			status=mgr.getStatus(actionID,null).intValue();
			Thread.sleep(1000);
			if(ActionStatus.DONE==status){
				fail("Action already done->must have failed");
			}
			count++;
		}while(count<getTimeOut() && !("READY".equals(ActionStatus.toString(status))));
		if(count>=getTimeOut())throw new Exception("Timeout");
	}

	/**
	 * wait until the specified action becomes "done" with a timeout defined by getTimeOut()
	 * @param actionID
	 * @throws Exception
	 */
	protected void waitUntilDone(String actionID)throws Exception{
		assertNotNull(actionID);
		int status=0;
		int count=0;
		do{
			status=mgr.getStatus(actionID,null).intValue();
			Thread.sleep(1000);
			count++;
		}while(count<getTimeOut() && !("DONE".equals(ActionStatus.toString(status))));
		if(count>=getTimeOut()) {
			mgr.getAction(actionID).printLogTrace();
			throw new Exception("Timeout");
		}
	}
	
	protected void waitUntilRunning(String actionID)throws Exception{
		assertNotNull(actionID);
		int status=0;
		int count=0;
		do{
			status=mgr.getStatus(actionID,null).intValue();
			Thread.sleep(60);
			count++;
		}while(count<getTimeOut() && !("RUNNING".equals(ActionStatus.toString(status))));
		if(count>=getTimeOut())throw new Exception("Timeout");
	}
	
	//timeout in seconds
	protected int getTimeOut(){
		return 60;
	}

	protected void doRun(String actionID)throws Exception{
		doRun(actionID, null);
	}

	protected void doRun(String actionID, Runnable callback)throws Exception{
		assertNotNull(actionID);
		waitUntilReady(actionID);
		mgr.run(actionID,null);
		waitUntilDone(actionID);
		if(callback!=null)callback.run();
	}
	
	protected void assertDone(String actionID)throws Exception{
		int s = mgr.getStatus(actionID,null).intValue();
		assertEquals(ActionStatus.DONE, s);
	}

	protected void assertSuccessful(String actionID)throws Exception{
		Action a=xnjs.get(InternalManager.class).getAction(actionID);
		assertNotNull(a);
		int result = a.getResult().getStatusCode();
		assertEquals(ActionResult.SUCCESSFUL, result);
	}
	
	protected void assertNotSuccessful(String actionID)throws Exception{
		Action a=xnjs.get(InternalManager.class).getAction(actionID);
		assertNotNull(a);
		assertTrue(ActionResult.SUCCESSFUL!=a.getResult().getStatusCode());
	}
	
	protected void printActionLog(String actionID)throws Exception{
		Action a=xnjs.get(InternalManager.class).getAction(actionID);
		assertNotNull(a);
		a.printLogTrace();
	}

	// create a dummy client
	protected Client createClient(){
		Client c = new Client();
		c.setXlogin(new Xlogin(new String[] {"nobody"}));
		SecurityTokens st = new SecurityTokens();
		st.setUserName("CN=test");
		c.setAuthenticatedClient(st);
		return c;
	}
}
