package de.fzj.unicore.uas.util;

import org.junit.Assert;

import org.junit.Test;

public class TestAnnotationStateMachine {

	@Test
	public void testNoError()throws Exception {
		Target t = new Target();
		t.error=false;
		State<Target> result = new StateMover<>(t).call();
		Assert.assertNull(result);
		Assert.assertTrue(t.invoked1);
		Assert.assertTrue(t.invoked2);
		Assert.assertFalse(t.invoked2Error);
	}
	
	@Test
	public void testAsync()throws Exception {
		Target t = new Target();
		t.error=false;
		State<Target> result = new StateMover<>(t, true).call();
		Assert.assertNotNull(result);
		Assert.assertEquals("state2",result.getName());
		Assert.assertTrue(t.invoked1);
		Assert.assertFalse(t.invoked2);
		Assert.assertFalse(t.invoked2Error);
	}
	
	@Test
	public void testError()throws Exception {
		Target t = new Target();
		t.error=true;
		State<Target> result = new StateMover<>(t).call();
		Assert.assertNull(result);
		Assert.assertTrue(t.invoked1);
		Assert.assertTrue(t.invoked2);
		Assert.assertTrue(t.invoked2Error);
	}
	
	@Test
	public void testRetry()throws Exception {
		Target2 t = new Target2();
		t.error=true;
		State<Target2> result = new StateMover<>(t).call();
		Assert.assertNull(result);
		Assert.assertTrue(t.invoked1);
		Assert.assertTrue(t.invoked2);
		Assert.assertEquals(1,3, t.invoked2_count);
		Assert.assertTrue(t.invoked2Error);
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
