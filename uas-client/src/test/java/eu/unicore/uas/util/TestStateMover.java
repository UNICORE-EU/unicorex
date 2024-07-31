package eu.unicore.uas.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class TestStateMover {

	@Test
	public void testNoError()throws Exception {
		Target t = new Target();
		State<Target> result = new StateMover<Target>(new State1(), t).call();
		assertNull(result);
		assertTrue(t.invoked1);
		assertTrue(t.invoked2);
		assertFalse(t.invoked2Error);
	}
	
	@Test
	public void testError()throws Exception {
		Target t = new Target();
		t.error=true;
		State<Target> result = new StateMover<Target>(new State1(), t).call();
		assertNull(result);
		assertTrue(t.invoked1);
		assertTrue(t.invoked2);
		assertTrue(t.invoked2Error);
	}
	
	@Test
	public void testAsync()throws Exception {
		Target t = new Target();
		t.error=false;
		State<Target> result = new StateMover<Target>(new AsyncState1(), t, true).call();
		assertNotNull(result);
		assertTrue(result instanceof State2);
		assertTrue(t.invoked1);
		assertFalse(t.invoked2);
		assertFalse(t.invoked2Error);
	}
	
	
	
	public static class Target {
		public boolean error = false;
		public boolean invoked1 = false;
		public boolean invoked2 = false;
		public boolean invoked2Error = false;
	}
	
	public static class State1 implements State<Target>{
		public String getName(){
			return "initial";
		}

		@Override
		public State<Target> next(Target target) throws Exception {
			target.invoked1=true;
			return new State2();
		}

		@Override
		public State<Target> onError(Target target, Exception ex) throws Exception {
			return null;
		}
		
		public boolean isPausable(){
			return false;
		}
		
		public int getNumberOfRetries(){
			return 0;
		}
		
		public int getRetryDelay(){
			return 0;
		}
	}

	public static class State2 implements State<Target>{
		public String getName(){
			return "final";
		}

		@Override
		public State<Target> next(Target target) throws Exception {
			target.invoked2=true;
			if(target.error){
				throw new Exception();
			}
			else{
				return null;
			}
		}

		@Override
		public State<Target> onError(Target target, Exception ex) throws Exception {
			target.invoked2Error=true;
			return null;
		}
		
		public boolean isPausable(){
			return false;
		}
		
		public int getNumberOfRetries(){
			return 0;
		}
		
		public int getRetryDelay(){
			return 0;
		}
	}
	
	public static class AsyncState1 extends State1{
		
		public boolean isPausable(){
			return true;
		}
		
	}

}
