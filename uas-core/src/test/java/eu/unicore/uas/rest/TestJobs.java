package eu.unicore.uas.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import eu.unicore.client.Endpoint;
import eu.unicore.client.core.CoreClient;
import eu.unicore.client.core.JobClient;
import eu.unicore.client.core.StorageClient;
import eu.unicore.services.restclient.BaseClient;
import eu.unicore.services.restclient.ForwardingHelper;
import eu.unicore.services.restclient.IAuthCallback;
import eu.unicore.services.restclient.RESTException;
import eu.unicore.services.restclient.UsernamePassword;
import eu.unicore.uas.SecuredBase;
import eu.unicore.uas.util.EchoServer;
import eu.unicore.util.ChannelUtils;

public class TestJobs extends SecuredBase {

	@Test
	public void testJobSubmission() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest/core/jobs";
		System.out.println("Accessing "+url);
		IAuthCallback auth = new UsernamePassword("demouser", "test123");
		BaseClient client = new BaseClient(url, kernel.getClientConfiguration(), auth);
		JSONObject task = new JSONObject();
		task.put("ApplicationName", "Date");
		String jobUrl = client.create(task);
		System.out.println("created: "+jobUrl);
		JobClient job = new JobClient(new Endpoint(jobUrl), kernel.getClientConfiguration(), auth);
		// get job properties
		JSONObject jobProps = job.getProperties();
		System.out.println("*** new job: ");
		System.out.println(jobProps.toString(2));

		// access parent TSS
		String tssUrl = job.getLinkUrl("parentTSS");
		client.setURL(tssUrl);
		JSONObject tssProps = client.getJSON();
		System.out.println("*** parent TSS: ");
		System.out.println(tssProps.toString(2));

		String jobsURL=client.getLink("jobs");
		client.setURL(jobsURL);
		// check that the job URL is listed
		System.out.println(client.getJSON().toString(2));
		assertTrue(contains(client.getJSON().getJSONArray("jobs"),jobUrl));

		// job desc
		JSONObject submitted = job.getSubmittedJobDescription();
		System.out.println("*** retrieving submitted job: ");
		System.out.println(submitted.toString(2));
		assertEquals("Date",submitted.getString("ApplicationName"));

		// details
		JSONObject details = job.getBSSDetails();
		System.out.println("*** bss details: ");
		System.out.println(details.toString(2));
		// with query params
		details = job.getBSSDetails("rawDetailsData");
		assertEquals(1, details.length());

		// working directory
		JSONObject wdProps = job.getWorkingDirectory().getProperties();
		System.out.println("*** working directory: ");
		System.out.println(wdProps.toString(2));
	}

	@Test
	public void testTaggedJobs() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest/core/jobs";
		System.out.println("Accessing "+url);
		IAuthCallback auth = new UsernamePassword("demouser", "test123");
		BaseClient client = new BaseClient(url,kernel.getClientConfiguration(), auth);

		JSONObject task = new JSONObject();
		task.put("ApplicationName", "Date");
		JSONArray tags = new JSONArray();
		tags.put("test");
		tags.put("foo");
		task.put("Tags", tags);
		String jobUrl = client.create(task);
		System.out.println("created: "+jobUrl);

		client.setURL(jobUrl);
		JSONObject jobProps = client.getJSON();
		System.out.println("*** new job: ");
		System.out.println(jobProps.toString(2));
		tags = jobProps.getJSONArray("tags");
		assertTrue(contains(tags, "foo"));
		assertTrue(contains(tags, "test"));

		// check listing with a query works
		client.setURL(url+"?tags=foo");
		JSONArray taggedJobs = client.getJSON().getJSONArray("jobs");
		assertEquals(1, taggedJobs.length());
		client.setURL(url+"?tags=nope");
		taggedJobs = client.getJSON().getJSONArray("jobs");
		assertEquals(0, taggedJobs.length());
	}

	@Test
	public void testJobDirectoryHandling() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest/core";
		System.out.println("Accessing "+url);
		IAuthCallback auth = new UsernamePassword("demouser", "test123");
		CoreClient cc = new CoreClient(new Endpoint(url),kernel.getClientConfiguration(), auth);
		JSONObject task = new JSONObject();
		task.put("ApplicationName", "Date");
		JobClient jc = cc.getSiteClient().submitJob(task);
		StorageClient sc = jc.getWorkingDirectory();
		System.out.println(sc.getProperties().toString(2));
		while(!jc.isFinished())Thread.sleep(1000);
		System.out.println(sc.getProperties().toString(2));
		Thread.sleep(500);
	}
	
	@Test
	public void testJobControl() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest/core/jobs";
		System.out.println("Accessing "+url);
		final IAuthCallback auth = new UsernamePassword("demouser", "test123");
		final BaseClient client = new BaseClient(url, kernel.getClientConfiguration(), auth);
		JSONObject task = new JSONObject();
		task.put("ApplicationName", "Date");
		String jobUrl = client.create(task);
		System.out.println("created: "+jobUrl);
		JobClient job = new JobClient(new Endpoint(jobUrl), kernel.getClientConfiguration(), auth);
		job.poll(null);
		job.restart();
		Thread.sleep(1000);
		job.abort();
		Thread.sleep(1000);
		job.delete();
		RESTException e = assertThrows(RESTException.class, ()->job.restart());
		assertEquals(404, e.getStatus());
	}

	@Test
	public void testPortForwarding() throws Exception {
		String url = kernel.getContainerProperties().getContainerURL()+"/rest/core/jobs";
		System.out.println("Accessing "+url);
		final IAuthCallback auth = new UsernamePassword("demouser", "test123");
		final BaseClient client = new BaseClient(url, kernel.getClientConfiguration(), auth);
		JSONObject task = new JSONObject();
		task.put("ApplicationName", "Date");
		String jobUrl = client.create(task);
		System.out.println("created: "+jobUrl);
		JobClient job = new JobClient(new Endpoint(jobUrl), kernel.getClientConfiguration(), auth);
		job.poll(null);
		System.out.println("Job is: "+job.getStatus()+", starting port forwarding.");
		String forwardURL = job.getLinkUrl("forwarding")+"?port="+echo.getServerPort();
		client.setURL(forwardURL);
		ForwardingHelper fh = new ForwardingHelper(client);
		final AtomicInteger localPort = new AtomicInteger();
		Thread t = new Thread( ()->{
			try {
				try(ServerSocketChannel sc = ServerSocketChannel.open()){
					sc.configureBlocking(false);
					sc.bind(new InetSocketAddress(InetAddress.getLocalHost(), 0), 1);
					fh.accept(sc, (c)->{
						try {
							SocketChannel serviceProxy = fh.connect(forwardURL);
							fh.startForwarding(c, serviceProxy);
						}catch(Exception ex) {
							throw new RuntimeException(ex);
						}
					});
					localPort.set(((InetSocketAddress)(sc.getLocalAddress())).getPort());
					fh.run();
				}
			}
			catch(Exception e) {}
		});
		t.start();
		while(localPort.get()==0)Thread.sleep(200);
		try(SocketChannel s = SocketChannel.open(new InetSocketAddress(InetAddress.getLocalHost(), localPort.get()))){
			PrintWriter w = new PrintWriter(new OutputStreamWriter(ChannelUtils.newOutputStream(s, 65536)), true);
			Reader r = new InputStreamReader(ChannelUtils.newInputStream(s, 65536));
			BufferedReader br = new BufferedReader(r);
			System.out.println("Forwarding established");
			String line = "this is a test";
			System.out.println("--> "+line);
			w.println(line);
			String reply = br.readLine();
			System.out.println("<-- "+reply);
			assertEquals(line, reply);
		}
	}

	private boolean contains(JSONArray array, Object o) throws JSONException {
		for(int i=0; i<array.length();i++){
			if(array.get(i).equals(o))return true;
		}
		return false;
	}


	private static EchoServer echo = null;

	@BeforeAll
	public static void startEchoServer() throws Exception {
		echo = new EchoServer();
		echo.start();
	}

	@AfterAll
	public static void stopEchoServer() {
		if(echo!=null)try{
			echo.shutdown();
		}catch(Exception e) {}
	}

}
