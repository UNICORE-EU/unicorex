package de.fzj.unicore.xnjs.cluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.junit.Ignore;
import org.junit.Test;

import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.ActionStatus;
import de.fzj.unicore.xnjs.ems.Manager;
import de.fzj.unicore.xnjs.persistence.IActionStore;
import de.fzj.unicore.xnjs.persistence.JDBCActionStore;

@Ignore
public class TestOperationUsingSharedDatabase extends ClusterTestBase {

	private static String job1="src/test/resources/json/date_with_inline_data.json";
	
	@Test
	public void testDistributedLocks()throws Exception{
		IActionStore s1=xnjs1.getActionStore("JOBS");
		s1.removeAll();
		String key="1";
		Action a=makeAction();
		a.setUUID(key);
		a.setWaiting(true);
		xnjs1.get(Manager.class).add(a,null);
		Collection<String>ids1=s1.getUniqueIDs();
		assertEquals(1, ids1.size());
		
		IActionStore s2=xnjs2.getActionStore("JOBS");
		Collection<String>ids2=s2.getUniqueIDs();
		assertEquals(1, ids2.size());
		
		((JDBCActionStore)s2).setTimeoutPeriod(2);
		((JDBCActionStore)s1).setTimeoutPeriod(2);
		
		//check locking
		Action a1=s1.getForUpdate(key);
		a1.setWaiting(true);
		try{
			s2.getForUpdate(key);
			fail("Should get a timeout exception");
		}catch(TimeoutException te){
			/* ok */
		}
		
		s1.put("1", a1);
		try{
			s2.getForUpdate(key);
			s2.put("1", a1);	
		}catch(TimeoutException te){
			fail();
		}
	}
	
	@Test
	public void testProcessing()throws Exception{
		Thread.sleep(500);
		Manager mgr1=xnjs1.get(Manager.class);
		Manager mgr2=xnjs2.get(Manager.class);
		Action a=makeAction();
		String id=a.getUUID();
		mgr1.add(a,null);
		assertNotNull(id);
		runAction(id,mgr2);
		runAction(id,mgr1);
	}
	
	
	@Test
	public void testMultiProcessing()throws Exception{
		List<String>ids = new ArrayList<>();
		Manager mgr1=xnjs1.get(Manager.class);
		Manager mgr2=xnjs2.get(Manager.class);
		for(int i = 0; i<30 ; i++){
			Action a=makeAction();
			String id=a.getUUID();
			mgr1.add(a,null);
			ids.add(id);
		}
		for(String id: ids){
			System.out.println(id);
			assertNotNull(id);
			runAction(id,mgr2);
		}
	}
	
	private void runAction(String id, Manager mgr)throws Exception{
		int status=0;
		int count = 0;
		do{
			int newstatus=mgr.getStatus(id,null).intValue();
			count++;
			Thread.sleep(1000);
			if(newstatus!=status){
				status = newstatus;
				System.out.println(ActionStatus.toString(status));
			}
		}while(!("DONE".equals(ActionStatus.toString(status))) || count<60);
		
		mgr.destroy(id, null);
	}
	
	Action makeAction() throws Exception {
		Action action=xnjs1.makeAction(loadJSON(job1));
		action.getProcessingContext().put(Action.AUTO_SUBMIT,Boolean.TRUE);
		assertNotNull(action);
		return action;
	}
	
}
