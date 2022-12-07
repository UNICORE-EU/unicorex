package de.fzj.unicore.xnjs.tsi.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.InetAddress;
import java.nio.channels.SocketChannel;
import java.util.Random;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import eu.unicore.util.ChannelUtils;

public class TestTSISSL extends RemoteTSISSLTestCase{

	Random r=new Random();

	@Test
	public void testBasicSetup()throws Exception{
		DefaultTSIConnectionFactory f = (DefaultTSIConnectionFactory)xnjs.get(TSIConnectionFactory.class);
		assertNotNull(f);
		try(TSIConnection c=f.getTSIConnection("nobody", null, null, -1)){
			System.out.println("TSI "+c.getTSIVersion()+" isAlive="+c.isAlive());
			System.out.println(c);
			assertTrue(c.compareVersion(TSIConnection.RECOMMENDED_TSI_VERSION));
		}

		try(TSIConnection c=f.getTSIConnection("nobody", null,"127.0.0.1",-1)){
			InetAddress localhost=InetAddress.getByName("127.0.0.1");
			assertEquals(localhost,c.getTSIAddress());
		}
		int n = f.getNumberOfPooledConnections();
		try(TSIConnection c=f.getTSIConnection("nobody", null,"127.0.0.1",-1)){}
		assertEquals(n,f.getNumberOfPooledConnections());

		try{
			f.getTSIConnection("nobody", null, "no-such-host", -1);
			fail("expected exception here");
		}catch(IllegalArgumentException e){
			assertTrue(e.getMessage().contains("No TSI is configured at 'no-such-host'"));
		}

		RemoteTSI tsi=makeTSI();
		assertNotNull(tsi);
		assertEquals("UNICORE TSI at 127.0.0.1:65431",tsi.getFileSystemIdentifier());
	}

	@Test
	public void testForwardingSSL() throws Exception {
		RemoteTSI tsi=(RemoteTSI)xnjs.getTargetSystemInterface(null);
		assertNotNull(tsi);
		SocketChannel s = tsi.openConnection("127.0.0.1", echo.getServerPort());
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
	
	private static EchoServer echo = null;
	
	@BeforeClass
	public static void startBackend() throws Exception {
		echo = new EchoServer();
		echo.start();
	}

	@AfterClass
	public static void stopBackend() {
		if(echo!=null)try{
			echo.shutdown();
		}catch(Exception e) {}
	}

}
