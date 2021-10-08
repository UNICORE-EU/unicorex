/*********************************************************************************
 * Copyright (c) 2006 Forschungszentrum Juelich GmbH 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * (1) Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the disclaimer at the end. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * 
 * (2) Neither the name of Forschungszentrum Juelich GmbH nor the names of its 
 * contributors may be used to endorse or promote products derived from this 
 * software without specific prior written permission.
 * 
 * DISCLAIMER
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************/

package de.fzj.unicore.xnjs.tsi.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.json.JSONObject;
import org.junit.Test;

import de.fzj.unicore.xnjs.ConfigurationSource;
import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.XNJSProperties;
import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.ems.IExecutionContextManager;
import de.fzj.unicore.xnjs.ems.event.EventHandler;
import de.fzj.unicore.xnjs.ems.event.XnjsEvent;
import de.fzj.unicore.xnjs.incarnation.ITweaker;
import de.fzj.unicore.xnjs.tsi.IExecution;
import de.fzj.unicore.xnjs.tsi.IExecutionSystemInformation;
import de.fzj.unicore.xnjs.tsi.remote.Execution.BSSInfo;
import de.fzj.unicore.xnjs.tsi.remote.Execution.BSSSummary;
import de.fzj.unicore.xnjs.tsi.remote.Execution.BSS_STATE;
import de.fzj.unicore.xnjs.util.IOUtils;
import eu.unicore.security.Client;
import eu.unicore.security.Xlogin;


public class TestJobProcessingLegacyTSI extends LegacyTSITestCase implements EventHandler {

	private static String 
	date = "src/test/resources/json/date.json",
	date_with_redirect="src/test/resources/json/date_with_extras.json",
	sleep = "src/test/resources/json/sleep.json";

	@Override
	protected RemoteTSIModule getTSIModule(ConfigurationSource cs){
		return new MyTSIModule(cs.getProperties());
	}

	public static class MyTSIModule extends RemoteTSIModule{
		public MyTSIModule(Properties p){
			super(p);
		}

		@Override
		protected void bindExecution(){
			bind(IExecution.class).to(MyExec.class);
			bind(IExecutionSystemInformation.class).to(MyExec.class);
		}

	}

	@Override
	protected void addProperties(ConfigurationSource cs){
		super.addProperties(cs);
		cs.getProperties().put("XNJS."+XNJSProperties.RESUBMIT_DELAY, "1");
	}

	@Test
	public void testRunMulti() throws Exception {
		ThreadPoolExecutor es = new ThreadPoolExecutor(4, 4, 
				60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

		Callable<String> t1 = new Callable<String>() {
			public String call() {
				try {
					resourcesAndRedirect();
				}catch(Exception e) {}
				return "";
			}
		};

		Callable<String> t2 = new Callable<String>() {
			public String call() {
				try {
					submitInteractive();
				}catch(Exception e) {}
				return "";
			}
		};

		Callable<String> t3 = new Callable<String>() {
			public String call() {
				try {
					queueUpdate();
				}catch(Exception e) {}
				return "";
			}
		};

		CompletionService<String> cs = new ExecutorCompletionService<>(es);
		cs.submit(t1);
		cs.submit(t2);
		cs.submit(t3);
		
		for(int i=0; i<3; i++) {
			cs.poll(120, TimeUnit.SECONDS).get();
		}

		es.shutdownNow();
	}


	private void resourcesAndRedirect() throws Exception {
		System.out.println("*** resourcesAndRedirect");
		JSONObject job = loadJSONObject(date_with_redirect);
		String id="";
		Action a=xnjs.makeAction(job);
		Client c=new Client();
		a.setClient(c);
		c.setXlogin(new Xlogin(new String[] {"nobody"}));
		id=a.getUUID();
		mgr.add(a,c);
		doRun(id);
		a = internalMgr.getAction(id);
		a.printLogTrace();
		assertEquals("myout",a.getExecutionContext().getStdout());
		assertEquals("myerr",a.getExecutionContext().getStderr());
		File f=new File(a.getExecutionContext().getWorkingDirectory(),a.getExecutionContext().getStdout());
		assertTrue(f.exists());
		f=new File(a.getExecutionContext().getWorkingDirectory(),a.getExecutionContext().getStderr());
		assertTrue(f.exists());

		assertTrue(a.getLog().toString().contains("#TSI_TIME 30"));
		assertTrue(a.getLog().toString().contains("#TSI_PROJECT qcd"));
	}

	protected int getTimeOut(){
		return 6000;
	}

	static String pid=null;

	private void submitInteractive() throws Exception {
		System.out.println("*** submitInteractive");
		JSONObject job = loadJSONObject(date);
		String id="";
		final Action a=xnjs.makeAction(job);
		Client c=new Client();
		a.setClient(c);
		c.setXlogin(new Xlogin(new String[] {"nobody"}));
		id=a.getUUID();
		xnjs.get(IExecutionContextManager.class).getContext(a);
		a.getExecutionContext().setRunOnLoginNode(true);
		final String wd=a.getExecutionContext().getWorkingDirectory();
		System.out.println("Starting async script in "+wd);
		mgr.add(a,c);

		Runnable check=new Runnable(){
			public void run(){
				File pidFile=new File(wd, a.getExecutionContext().getPIDFileName());
				assertTrue(pidFile.exists());
				try{
					pid=IOUtils.readFile(pidFile);
					System.out.println("Have PID : "+pid);
				}catch(Exception e){}
			}
		};
		doRun(id, check);

		Action a1 = internalMgr.getAction(id);

		a1.printLogTrace();
		String bsid=a1.getBSID();
		System.out.println("BSID: "+bsid);
	}

	private void queueUpdate() throws Exception {
		System.out.println("*** queueUpdate");
		JSONObject job = loadJSONObject(sleep);
		String id="";
		Action a=xnjs.makeAction(job);
		Client c=new Client();
		a.setClient(c);
		c.setXlogin(new Xlogin(new String[] {"nobody"}));
		id=a.getUUID();
		mgr.add(a,c);
		doRun(id);
		a = internalMgr.getAction(id);
		assertEquals("NOBATCH", a.getExecutionContext().getBatchQueue());
	}

	@Test
	public void testSubmitRetry() throws Exception {
		MyExec.failSubmits=true;
		JSONObject job = loadJSONObject(date);
		String id="";
		Action a=xnjs.makeAction(job);
		Client c=new Client();
		a.setClient(c);
		c.setXlogin(new Xlogin(new String[] {"nobody"}));
		id=a.getUUID();
		mgr.add(a,c);
		doRun(id);
		assertSuccessful(id);
		Action a1 = internalMgr.getAction(id);
		assertTrue(a1.getLog().toString().contains("Fake submit failure"));
		MyExec.failSubmits = false;
	}


	@Test
	public void testParseStatusListing() throws Exception {
		String s1="QSTAT \n 2795100 RUNNING\n 2795100 RUNNING \n";
		Map<String,BSSInfo>st=new HashMap<String, BSSInfo>();
		st.put("2795100", new BSSInfo("2795100", "j1", BSS_STATE.UNKNOWN));
		BSSState.updateBatchJobStates(st,s1,null);
		assertEquals(BSS_STATE.RUNNING,st.get("2795100").bssState);
	}

	@Test
	public void testParseLongStatusListing() throws Exception {
		eventsReceived=0;
		long entries=100000;
		StringBuilder s1=new StringBuilder("QSTAT \n");
		Map<String,BSSInfo>st=new HashMap<String, BSSInfo>();

		for(int i=1;i<=entries;i++){
			s1.append(i+" RUNNING DEFAULT_QUEUE\n");
			st.put(String.valueOf(i), new BSSInfo(String.valueOf(i),"j"+i, BSS_STATE.UNKNOWN));
		}

		long start=System.currentTimeMillis();
		BSSState.updateBatchJobStates(st,s1.toString(),this);
		long end=System.currentTimeMillis();
		System.out.println("Parsing qstat "+entries+" entries took "+(end-start)+ " msec.");
		assertEquals(entries,eventsReceived);
	}

	@Test
	public void testParseQueueInfo() throws Exception {
		String s1="QSTAT \n 2795100 RUNNING FAST\n 2795101 RUNNING NORMAL\n 2795102 RUNNING NORMAL\n";

		Map<String,BSSInfo>st=new HashMap<String, BSSInfo>();
		st.put("2795100", new BSSInfo("2795100","j1",BSS_STATE.UNKNOWN));
		st.put("2795101", new BSSInfo("2795101","j1",BSS_STATE.UNKNOWN));
		st.put("2795102", new BSSInfo("2795102","j1",BSS_STATE.UNKNOWN));
		BSSSummary summary = BSSState.updateBatchJobStates(st,s1,null);
		assertEquals(BSS_STATE.RUNNING,st.get("2795100").bssState);
		System.out.println(summary.toString());
		assertEquals(2, summary.queueFilling.get("NORMAL").intValue());
	}

	@Test
	public void testVersionCompare(){
		assertTrue(TSIUtils.compareVersion("1.2.3", "1.2.3"));
		assertTrue(TSIUtils.compareVersion("1.2.3.4", "1.2.3"));
		assertTrue(TSIUtils.compareVersion("1.2.4", "1.2.3"));
		assertTrue(TSIUtils.compareVersion("2.2.3", "1.2.3"));
		assertTrue(TSIUtils.compareVersion("2.0", "1.2.3"));


		assertFalse(TSIUtils.compareVersion("1.2.2", "1.2.3"));
		assertFalse(TSIUtils.compareVersion("1.2", "1.2.3"));
		assertFalse(TSIUtils.compareVersion("1.2.2.4", "1.2.3"));

		//test number format problems
		assertFalse(TSIUtils.compareVersion("TESTING", "1.2.3"));

	}

	private int eventsReceived=0;

	public void handleEvent(XnjsEvent xe){
		eventsReceived++;
	}


	@Singleton
	public static class MyExec extends Execution {

		@Inject
		public MyExec(XNJS xnjs, ITweaker tw, TSIConnectionFactory factory, IBSSState bss){
			super(xnjs,tw,factory, bss);
		}

		public static boolean failSubmits=false;

		boolean ok = false;

		@Override
		public int submit(Action job)throws ExecutionException{
			if(failSubmits){
				ok=!ok;
				if(ok){
					System.out.println("Failing submit...");
					throw new ExecutionException("Fake submit failure");
				}
			}
			return super.submit(job);
		}
	}
}
