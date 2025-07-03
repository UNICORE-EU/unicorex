package eu.unicore.xnjs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;

import eu.unicore.xnjs.ConfigurationSource.ProcessorChain;
import eu.unicore.xnjs.ems.Action;
import eu.unicore.xnjs.ems.ActionStatus;
import eu.unicore.xnjs.ems.DummyProcessor;
import eu.unicore.xnjs.ems.ProcessingContext;
import eu.unicore.xnjs.tsi.TSI;

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
	public void testAddProcessing() throws Exception {
		xnjs.setProcessingChain("foo", "urn:test", DummyProcessor.class.getName());
		assertTrue(xnjs.haveProcessingFor("foo"));
		assertTrue(xnjs.getProcessorChain("foo").getProcessorClasses().
				get(0).equals(DummyProcessor.class.getName()));
		ProcessorChain pc = xnjs.getProcessorChain("foo");
		pc.insertProcessor(DummyProcessor2.class.getName(), 0);
		assertEquals(2, pc.getProcessorClasses().size());
		pc.insertProcessor(DummyProcessor3.class.getName(), DummyProcessor.class.getName(), DummyProcessor2.class.getName());
		assertEquals(3, pc.getProcessorClasses().size());
		assertEquals(DummyProcessor3.class.getName(), pc.getProcessorClasses().get(1));
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
		MetricRegistry m = new MetricRegistry();
		xnjs.getMetrics().entrySet().forEach(entry->
			m.register(entry.getKey(), entry.getValue()));
		ConsoleReporter r = ConsoleReporter.forRegistry(m).build();
		r.report();
	}

	public static class DummyProcessor2 extends DummyProcessor {
		public DummyProcessor2(XNJS xnjs){
			super(xnjs);
		}
	}

	public static class DummyProcessor3 extends DummyProcessor {
		public DummyProcessor3(XNJS xnjs){
			super(xnjs);
		}
	}

}
