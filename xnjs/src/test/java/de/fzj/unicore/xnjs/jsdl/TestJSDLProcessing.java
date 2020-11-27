package de.fzj.unicore.xnjs.jsdl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;

import org.ggf.schemas.jsdl.x2005.x11.jsdl.JobDefinitionDocument;
import org.junit.Test;

import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.ActionResult;
import de.fzj.unicore.xnjs.ems.ActionStateChangeListener;
import de.fzj.unicore.xnjs.ems.BasicManager;
import de.fzj.unicore.xnjs.ems.EMSTestBase;
import de.fzj.unicore.xnjs.ems.MockChangeListener;
import eu.unicore.bugsreporter.annotation.FunctionalTest;

/**
 * setup and run tests of the core ems processing
 */
public class TestJSDLProcessing extends EMSTestBase {

	//jsdl document paths
	private static String d1="src/test/resources/jsdl/date_with_inlinedata.jsdl";

	@Test
	@FunctionalTest(id="testJSDLProcessing", 
	description="Tests processing of JSDL jobs by the XNJS")
	public void testJSDLAction() throws Exception {
		Action action=xnjs.makeAction(getJSDLDoc(d1));
		String id=action.getUUID();
		mgr.add(action,null);
		doRun(id);
		action=((BasicManager)mgr).getAction(id);
		assertEquals("my test job",action.getJobName());

		List<String>log=action.getLog();
		System.out.println(log.get(log.size()-1));

		assertSuccessful(id);

		//print stats
		String timeProfile=JSDLProcessor.getTimeProfile(action.getProcessingContext());
		assertTrue(timeProfile.contains("Total:"));
		System.out.println(timeProfile);

		// notifications
		MockChangeListener m = (MockChangeListener)xnjs.get(ActionStateChangeListener.class);
		assertTrue(m.getOrCreate(id).get()>0);
		System.out.println("State change notifications: "+m.getOrCreate(id).get());
		
		//test destroy()
		mgr.destroy(id, null);
		Thread.sleep(1000);
		Action a=xnjs.getActionStore("JOBS").get(id);
		assertNull(a);
	}

	@Test
	public void testScheduledProcessing() throws Exception {
		Action a = xnjs.makeAction(getJSDLDoc(d1));
		int delay=10000;
		long start=System.currentTimeMillis();
		a.setNotBefore(start+delay);
		String id=(String)mgr.add(a,null);
		doRun(id);
		a=internalMgr.getAction(id);
		ActionResult res=a.getResult();
		assertTrue(res.isSuccessful());
		long end=System.currentTimeMillis();
		assertTrue(end-start>delay);
		List<String>log=a.getLog();
		for(String s: log){
			if(s.contains("Further processing scheduled"))System.out.println(s);
		}
	}

	@Test
	public void testScheduledProcessingFromJSDL() throws Exception {
		JobDefinitionDocument jdd=getJSDLDoc(d1);
		int delay=10000;
		long start=System.currentTimeMillis();
		String sD="notBefore:"+JSDLUtils.getDateFormat().format(new Date(start+delay));
		jdd.getJobDefinition().getJobDescription().getJobIdentification().addJobAnnotation(sD);
		System.out.println(jdd);
		String id=(String)mgr.add(xnjs.makeAction(jdd),null);
		doRun(id);
		Action a=internalMgr.getAction(id);
		ActionResult res=a.getResult();
		assertTrue(res.isSuccessful());
		long end=System.currentTimeMillis();
		assertTrue(end-start>delay/2);
		List<String>log=a.getLog();
		for(String s: log){
			if(s.contains("Further processing scheduled"))System.out.println(s);
		}
	}
}
