package de.fzj.unicore.uas.util;

import junit.framework.Assert;

import org.junit.Test;

public class TestStateMover {

	@Test
	public void testNoError()throws Exception {
		Target t = new Target();
		State<Target> result = new StateMover<Target>(new State1(), t).call();
		Assert.assertNull(result);
		Assert.assertTrue(t.invoked1);
		Assert.assertTrue(t.invoked2);
		Assert.assertFalse(t.invoked2Error);
	}
	
	@Test
	public void testError()throws Exception {
		Target t = new Target();
		t.error=true;
		State<Target> result = new StateMover<Target>(new State1(), t).call();
		Assert.assertNull(result);
		Assert.assertTrue(t.invoked1);
		Assert.assertTrue(t.invoked2);
		Assert.assertTrue(t.invoked2Error);
	}
	
	@Test
	public void testAsync()throws Exception {
		Target t = new Target();
		t.error=false;
		State<Target> result = new StateMover<Target>(new AsyncState1(), t, true).call();
		Assert.assertNotNull(result);
		Assert.assertTrue(result instanceof State2);
		Assert.assertTrue(t.invoked1);
		Assert.assertFalse(t.invoked2);
		Assert.assertFalse(t.invoked2Error);
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
