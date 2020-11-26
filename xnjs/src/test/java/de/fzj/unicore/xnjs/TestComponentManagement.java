package de.fzj.unicore.xnjs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;

import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.ActionStatus;
import de.fzj.unicore.xnjs.ems.ProcessingContext;
import de.fzj.unicore.xnjs.ems.processors.DummyProcessor;
import de.fzj.unicore.xnjs.tsi.TSI;

public class TestComponentManagement extends XNJSTestBase {

	@Test
	public void testXNJSSetupAndStart(){
		assertNotNull(xnjs);
		TSI tsi1 = xnjs.getTargetSystemInterface(null);
		assertNotNull(tsi1);
		TSI tsi2 = xnjs.getTargetSystemInterface(null);
		assertNotSame(tsi1,tsi2);
	}
	
	@Test
	public void testProcessorChainInit(){
		try{
			xnjs.getProcessorChain("foo");
		}catch(Exception e){
			fail();
		}
	}
	
	@Test
	public void testAddProcessing(){
		String[]proc=new String[]{DummyProcessor.class.getName()};
		try{
			xnjs.setProcessingChain("foo", "urn:test", proc);
			assertTrue(xnjs.haveProcessingFor("foo"));
			assertTrue(xnjs.getProcessorChain("foo").get(0).equals(DummyProcessor.class.getName()));
		}catch(Exception e){
			fail();
		}
	}
	
	@Test
	public void testPrintInfo(){
		Action a=new Action();
		a.addLogTrace("test123");
		a.addLogTrace("Another test");
		StringWriter sw=new StringWriter();
		PrintWriter pw=new PrintWriter(sw);
		a.printLogTrace(pw);
		assertTrue(sw.toString().contains("test123"));
		a.fail("some error message");
		String s = a.toString();
		assertTrue(s.contains("some error message"));
		System.out.println(s);
	}
	
	@Test
	public void testActionInit(){
		Action a=new Action();
		assertNotNull(a.getProcessingContext());
		assertNotNull(a.getLog());
		assertEquals(a.getStatus(), ActionStatus.CREATED);
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void testProcessingContext(){
		ProcessingContext pc=new ProcessingContext();
		pc.put(Boolean.class,Boolean.TRUE);
		assertNotNull(pc.get(Boolean.class));
		assertTrue(pc.get(Boolean.class));
		ArrayList<String>l=new ArrayList<String>();
		pc.put(List.class,l);
		pc.put("foo",l);
		List<String>l2=pc.get(List.class);
		assertNotNull(l2);
		List<String>l3=pc.getAs("foo",List.class);
		assertNotNull(l3);
	}
	
	@Test
	public void testMetricsRegistry() throws Exception {
		MetricRegistry m = xnjs.getMetricRegistry();
		ConsoleReporter r = ConsoleReporter.forRegistry(m).build();
		r.report();
	}
	
}
