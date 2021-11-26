package de.fzj.unicore.xnjs.ems;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.fzj.unicore.xnjs.XNJSTestBase;
import de.fzj.unicore.xnjs.ems.processors.UsageLogger;
import eu.unicore.security.Client;

public class TestProcessors extends XNJSTestBase {

	@Test
	public void testBuildLogEntry()throws Exception{
		UsageLogger p = new UsageLogger(xnjs);
		Action a=makeAction();
		p.setAction(a);
		String log=p.getUsage();
		assertTrue(log.length()>0);
		assertTrue(log.contains("[SUCCESSFUL]"));
	}

	@Test
	public void testDone(){
		UsageLogger p = new UsageLogger(xnjs);
		Action a=makeAction();
		a.setStatus(ActionStatus.DONE);
		p.setAction(a);
		p.done();
		assertEquals(Boolean.TRUE, a.getProcessingContext().get(UsageLogger.USAGE_LOGGED));
	}
	
	private Action makeAction(){
		Action a=new Action();
		a.setType("test");
		a.setUUID("1234");
		Client c=new Client();
		c.setAnonymousClient();
		a.setClient(c);
		ExecutionContext ec = new ExecutionContext();
		ec.setExecutable("test.exe");
		a.setExecutionContext(ec);
		a.setResult(new ActionResult(ActionResult.SUCCESSFUL));
		return a;
	}

}
