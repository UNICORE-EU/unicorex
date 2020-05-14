package de.fzj.unicore.xnjs.ems.processors;

import static org.junit.Assert.*;

import java.util.UUID;

import org.junit.Test;

import de.fzj.unicore.xnjs.XNJSTestBase;
import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.ActionResult;
import de.fzj.unicore.xnjs.ems.ActionStatus;
import de.fzj.unicore.xnjs.ems.ExecutionContext;
import de.fzj.unicore.xnjs.ems.processors.LogProcessingTimeProcessor;
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
		ExecutionContext ec=new ExecutionContext(UUID.randomUUID().toString());
		ec.setExecutable("test.exe");
		a.setExecutionContext(ec);
		a.setResult(new ActionResult(ActionResult.SUCCESSFUL));
		return a;
	}

	@Test
	public void testBuildTimeLogEntry()throws Exception{
		LogProcessingTimeProcessor p = new LogProcessingTimeProcessor(xnjs);
		Action a=new Action();
		Long startTime=Long.valueOf(System.currentTimeMillis()-2000);
		a.getProcessingContext().put(LogProcessingTimeProcessor.startTimeKey, startTime);
		a.setType("test");
		a.setUUID("1234");
		p.setAction(a);
		String log=p.buildLogEntry();
		assertTrue(log.length()>0);
		assertTrue(log.contains("TIMER"));
	}

	@Test
	public void testLogTimeBegin()throws Exception{
		LogProcessingTimeProcessor p = new LogProcessingTimeProcessor(xnjs);
		Action a=new Action();
		p.setAction(a);
		p.begin();
		assertNotNull(a.getProcessingContext().get(LogProcessingTimeProcessor.startTimeKey));
	}

	@Test
	public void testLogTimeDone()throws Exception{
		LogProcessingTimeProcessor p = new LogProcessingTimeProcessor(xnjs);
		Action a=new Action();
		Long startTime=Long.valueOf(System.currentTimeMillis()-2000);
		a.getProcessingContext().put(LogProcessingTimeProcessor.startTimeKey, startTime);
		a.setType("test");
		a.setUUID("1234");
		a.setStatus(ActionStatus.DONE);
		p.setAction(a);
		p.done();
		String log=a.getLog().get(a.getLog().size()-1);
		assertTrue(log.contains("TIMER"));
	}
}
