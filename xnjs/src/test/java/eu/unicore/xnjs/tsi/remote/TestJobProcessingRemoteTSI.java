package eu.unicore.xnjs.tsi.remote;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import eu.unicore.security.Client;
import eu.unicore.security.Xlogin;
import eu.unicore.util.Log;
import eu.unicore.xnjs.ConfigurationSource;
import eu.unicore.xnjs.XNJSProperties;
import eu.unicore.xnjs.ems.Action;
import eu.unicore.xnjs.ems.ActionStateChangeListener;
import eu.unicore.xnjs.ems.ActionStatus;
import eu.unicore.xnjs.ems.ExecutionException;
import eu.unicore.xnjs.ems.IExecutionContextManager;
import eu.unicore.xnjs.ems.event.EventHandler;
import eu.unicore.xnjs.ems.event.INotificationSender;
import eu.unicore.xnjs.ems.event.XnjsEvent;
import eu.unicore.xnjs.idb.ApplicationInfo.JobType;
import eu.unicore.xnjs.tsi.IExecution;
import eu.unicore.xnjs.tsi.IExecutionSystemInformation;
import eu.unicore.xnjs.tsi.TSIBusyException;
import eu.unicore.xnjs.tsi.remote.Execution.BSSInfo;
import eu.unicore.xnjs.tsi.remote.Execution.BSS_STATE;
import eu.unicore.xnjs.util.ErrorCode;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;


public class TestJobProcessingRemoteTSI extends RemoteTSITestCase implements EventHandler {

	private static String 
	date = "src/test/resources/json/date.json",
	date_with_redirect = "src/test/resources/json/date_with_extras.json",
	sleep = "src/test/resources/json/sleep.json",
	pre_post = "src/test/resources/json/prepost.json";

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
			bind(INotificationSender.class).to(MockNotificationSender.class);
			bind(ActionStateChangeListener.class).to(MockNotificationSender.class);
		}

	}

	@Override
	protected void addProperties(ConfigurationSource cs){
		super.addProperties(cs);
		cs.getProperties().put("XNJS."+XNJSProperties.RESUBMIT_DELAY, "1");
	}

	@Test
	public void testRunMulti() throws Exception {
		var es = new ThreadPoolExecutor(4, 4, 
				60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

		var t1 = new Callable<String>() {
			public String call() {
				try {
					resourcesAndRedirect();
				}catch(Exception e) {}
				return "";
			}
		};

		var t2 = new Callable<String>() {
			public String call() {
				try {
					submitInteractive();
				}catch(Exception e) {}
				return "";
			}
		};

		var t3 = new Callable<String>() {
			public String call() {
				try {
					queueUpdate();
				}catch(Exception e) {}
				return "";
			}
		};

		var cs = new ExecutorCompletionService<String>(es);
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

		assertTrue(a.getLog().toString().contains("#TSI_TIME 1800"));
		assertTrue(a.getLog().toString().contains("#TSI_PROJECT qcd"));
	}

	protected int getTimeOut(){
		return 6000;
	}

	static String pid=null;

	private void submitInteractive() throws Exception {
		System.out.println("*** submitInteractive");
		JSONObject job = loadJSONObject(date);
		Action a=xnjs.makeAction(job);
		Client c=new Client();
		a.setClient(c);
		c.setXlogin(new Xlogin(new String[] {"nobody"}));
		final String id=a.getUUID();
		xnjs.get(IExecutionContextManager.class).initialiseContext(a);
		a.getExecutionContext().setRunOnLoginNode(true);
		mgr.add(a,c);
		waitUntilReady(id);
		Runnable check=new Runnable(){
			public void run(){
				try{
					Action a1 = mgr.getAction(id);
					final String wd = a1.getExecutionContext().getWorkingDirectory();
					System.out.println("Script running in "+wd);
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
	public void testJobControl() throws Exception {
		JSONObject job = loadJSONObject(sleep);
		job.put("Job type", JobType.ON_LOGIN_NODE.toString());
		Action a=xnjs.makeAction(job);
		Client c=new Client();
		a.setClient(c);
		c.setXlogin(new Xlogin(new String[] {"nobody"}));
		String id=a.getUUID();
		mgr.add(a,c);
		mgr.run(id,	c);
		Thread.sleep(1000);		
		mgr.pause(id, c);
		Thread.sleep(500);
		mgr.resume(id, c);
		Thread.sleep(500);
		mgr.abort(id, c);
		Thread.sleep(3000);
		a = mgr.getAction(id);
		assertDone(id);
		assertNotSuccessful(id);
		assertFalse(xnjs.get(IExecution.class).isBeingTracked(a));
		assertTrue(a.getLog().toString().contains("Paused"));
		assertTrue(a.getLog().toString().contains("Resumed"));
	}

	@Test
	public void testParseStatusListing() throws Exception {
		var s1 = "QSTAT \n 2795100 RUNNING\n 2795100 RUNNING \n";
		var st = new HashMap<String, BSSInfo>();
		st.put("2795100", new BSSInfo("2795100", "j1", BSS_STATE.UNKNOWN));
		BSSState.updateBatchJobStates(st,s1,null);
		assertEquals(BSS_STATE.RUNNING,st.get("2795100").bssState);
	}

	@Test
	public void testParseLongStatusListing() throws Exception {
		eventsReceived=0;
		long entries=100000;
		var s1 = new StringBuilder("QSTAT \n");
		var st = new HashMap<String, BSSInfo>();

		for(int i=1;i<=entries;i++){
			s1.append(i+" RUNNING DEFAULT_QUEUE\n");
			st.put(String.valueOf(i), new BSSInfo(String.valueOf(i),"j"+i, BSS_STATE.UNKNOWN));
		}

		var start = System.currentTimeMillis();
		BSSState.updateBatchJobStates(st,s1.toString(),this);
		var end = System.currentTimeMillis();
		System.out.println("Parsing qstat "+entries+" entries took "+(end-start)+ " msec.");
		assertEquals(entries,eventsReceived);
	}

	@Test
	public void testParseQueueInfo() throws Exception {
		var s1="QSTAT \n 2795100 RUNNING FAST\n 2795101 RUNNING NORMAL\n 2795102 RUNNING NORMAL\n";
		var st = new HashMap<String, BSSInfo>();
		st.put("2795100", new BSSInfo("2795100","j1",BSS_STATE.UNKNOWN));
		st.put("2795101", new BSSInfo("2795101","j1",BSS_STATE.UNKNOWN));
		st.put("2795102", new BSSInfo("2795102","j1",BSS_STATE.UNKNOWN));
		var summary = BSSState.updateBatchJobStates(st,s1,null);
		assertEquals(BSS_STATE.RUNNING,st.get("2795100").bssState);
		System.out.println(summary.toString());
		assertEquals(2, summary.queueFilling.get("NORMAL").intValue());
	}

	@Test
	public void testGetProcessListing() throws Exception {
		BSSState bss = xnjs.get(BSSState.class);
		var ps = bss.getProcessList("127.0.0.1");
		System.out.println(ps);
	}

	@Test
	public void testAllocateJob() throws Exception {
		MyExec.failSubmits=false;
		String id="";
		Action a = null;
		JSONObject job = new JSONObject();
		job.put("Job type", "allocate");
		JSONObject resources = new JSONObject();
		resources.put("Runtime", "60");
		resources.put("Nodes", "1");	
		job.put("Resources", resources);
		a = xnjs.makeAction(job);
		Client c = new Client();
		a.setClient(c);
		c.setXlogin(new Xlogin(new String[] {"nobody"}));
		id = a.getUUID();
		mgr.add(a,c);
		doRun(id);
		assertSuccessful(id);
		a = mgr.getAction(id);
		assertEquals("123456", a.getBSID());
		a.printLogTrace();
	}

	@Test
	public void testPrePostJob() throws Exception {
		System.out.println("*** prePostJob");
		JSONObject job = loadJSONObject(pre_post);
		Action a = xnjs.makeAction(job);
		Client c = new Client();
		a.setClient(c);
		c.setXlogin(new Xlogin(new String[] {"nobody"}));
		String id = a.getUUID();
		mgr.add(a,c);
		doRun(id);
		assertSuccessful(id);
	}

	@Test
	public void testBasicNotify() throws Exception {
		MyExec.failSubmits=false;
		JSONObject job = new JSONObject();
		job.put("Executable", "sleep 10");
		job.put("Job type", "ON_LOGIN_NODE");
		job.put("Notification", "dummy://notification-target");
		Action a = xnjs.makeAction(job);
		Client c = new Client();
		a.setClient(c);
		c.setXlogin(new Xlogin(new String[] {"nobody"}));
		String id = a.getUUID();
		mgr.add(a,c);
		doRun(id);
		assertSuccessful(id);
		a = mgr.getAction(id);
		a.printLogTrace();
	}

	@Test
	public void testDetailedNotify() throws Exception {
		MyExec.failSubmits=false;
		String id="";
		Action a = null;
		JSONObject job = new JSONObject();
		job.put("Executable", "sleep 10");
		JSONObject notification = new JSONObject();
		notification.put("URL", "dummy://notification-target");
		JSONArray arr = new JSONArray();
		arr.put("S");
		notification.put("bssStatus", arr);
		job.put("NotificationSettings", notification);
		a = xnjs.makeAction(job);
		Client c = new Client();
		a.setClient(c);
		c.setXlogin(new Xlogin(new String[] {"nobody"}));
		id = a.getUUID();
		mgr.add(a,c);
		doRun(id);
		assertSuccessful(id);
		a = mgr.getAction(id);
		a.printLogTrace();
	}

	private int eventsReceived=0;

	public void handleEvent(XnjsEvent xe){
		eventsReceived++;
	}


	@Singleton
	public static class MyExec extends Execution {

		@Inject
		public MyExec(TSIConnectionFactory factory, IBSSState bss, TSIMessages tsiMessages, TSIProperties props){
			super(factory, bss, tsiMessages, props);
		}

		public static boolean failSubmits=false;

		boolean ok = false;

		@Override
		public int submit(Action job)throws ExecutionException, TSIBusyException {
			if(failSubmits){
				ok=!ok;
				if(ok){
					System.out.println("Failing submit...");
					throw new ExecutionException(ErrorCode.ERR_TSI_COMMUNICATION,
							"Fake submit failure");
				}
			}
			return super.submit(job);
		}
	}
	
	@Singleton
	public static class MockNotificationSender implements INotificationSender, ActionStateChangeListener {

		public MockNotificationSender() { }

		@Override
		public void send(JSONObject msg, Action job) throws Exception{
			System.out.println("Notification: "+msg.toString(2));
			job.addLogTrace("Sent to: "+job.getNotificationURLs().get(0));
		}

		@Override
		public void stateChanged(Action action, int newState) {
			if(action==null ||
					action.getNotificationURLs()==null ||
					action.getNotificationURLs().isEmpty())return;
			if (newState==ActionStatus.RUNNING || newState==ActionStatus.DONE) {
				JSONObject message = new JSONObject();
				message.put("status", ActionStatus.toString(newState));
				try {
					send(message, action);
				}catch(Exception ex) {
					action.addLogTrace(Log.createFaultMessage("Could not send notification.", ex));
				}
			}
		}
	}
}
