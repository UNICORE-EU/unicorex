package eu.unicore.uas.util;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class TestAnnotationStateMachine {

	@Test
	public void testNoError()throws Exception {
		Target t = new Target();
		t.error=false;
		State<Target> result = new StateMover<>(t).call();
		assertNull(result);
		assertTrue(t.invoked1);
		assertTrue(t.invoked2);
		assertFalse(t.invoked2Error);
	}
	
	@Test
	public void testAsync()throws Exception {
		Target t = new Target();
		t.error=false;
		State<Target> result = new StateMover<>(t, true).call();
		assertNotNull(result);
		assertEquals("state2",result.getName());
		assertTrue(t.invoked1);
		assertFalse(t.invoked2);
		assertFalse(t.invoked2Error);
	}
	
	@Test
	public void testError()throws Exception {
		Target t = new Target();
		t.error=true;
		State<Target> result = new StateMover<>(t).call();
		assertNull(result);
		assertTrue(t.invoked1);
		assertTrue(t.invoked2);
		assertTrue(t.invoked2Error);
	}
	
	@Test
	public void testRetry()throws Exception {
		Target2 t = new Target2();
		t.error=true;
		State<Target2> result = new StateMover<>(t).call();
		assertNull(result);
		assertTrue(t.invoked1);
		assertTrue(t.invoked2);
		assertEquals(1,3, t.invoked2_count);
		assertTrue(t.invoked2Error);
	}
	
	public static class Target {
		public boolean error = false;
		public boolean invoked1 = false;
		public boolean invoked2 = false;
		public boolean invoked2Error = false;
		
		@StateInfo(name="state1", isInitial=true, onSuccess="state2", onError="", isPausable=true)
		public void m1(){
			invoked1=true;
		}
		
		@StateInfo(name="state2", isTerminal=true, onSuccess="", onError="")
		public void m2()throws Exception{
			invoked2=true;
			if(error){
				invoked2Error=true;
				throw new Exception();
			}
		}
		
	}
	
	public static class Target2 {
		public boolean error = false;
		public boolean invoked1 = false;
		public boolean invoked2 = false;
		public int invoked2_count = 0;
		public boolean invoked2Error = false;
		
		@StateInfo(name="state1", isInitial=true, onSuccess="state2", onError="", isPausable=true)
		public void m1(){
			invoked1=true;
		}
		
		@StateInfo(name="state2", isTerminal=true, onSuccess="", onError="", retryCount=3, retryDelay=100)
		public void m2()throws Exception{
			invoked2=true;
			invoked2_count++;
			if(error){
				invoked2Error=true;
				throw new Exception();
			}
		}
		
	}
	
}
