package eu.unicore.xnjs.tsi.remote;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.InetAddress;
import java.nio.channels.SocketChannel;
import java.util.Random;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import eu.unicore.security.Client;
import eu.unicore.util.ChannelUtils;
import eu.unicore.xnjs.tsi.remote.server.DefaultTSIConnectionFactory;
import eu.unicore.xnjs.tsi.remote.server.ServerTSIConnection;

public class TestTSISSL extends RemoteTSISSLTestCase{

	Random r=new Random();

	@Test
	public void testBasicSetup()throws Exception{
		DefaultTSIConnectionFactory f = (DefaultTSIConnectionFactory)xnjs.get(TSIConnectionFactory.class);
		assertNotNull(f);
		Client cl = TSIMessages.createMinimalClient("nobody");
		try(ServerTSIConnection c = f.getTSIConnection(cl, null, -1)){
			System.out.println("TSI "+c.getTSIVersion()+" isAlive="+c.isAlive());
			System.out.println(c);
			assertTrue(c.compareVersion(ServerTSIConnection.RECOMMENDED_TSI_VERSION));
		}
		try(ServerTSIConnection c = f.getTSIConnection(cl,"127.0.0.1",-1)){
			InetAddress localhost = InetAddress.getByName("127.0.0.1");
			assertEquals(localhost,c.getTSIAddress());
		}
		int n = f.getNumberOfPooledConnections();
		try(ServerTSIConnection c = f.getTSIConnection(cl,"127.0.0.1",-1)){}
		assertEquals(n,f.getNumberOfPooledConnections());
		try{
			f.getTSIConnection(cl, "no-such-host", -1);
			fail("expected exception here");
		}catch(IllegalArgumentException e){
			assertTrue(e.getMessage().contains("No TSI is configured at 'no-such-host'"));
		}
		RemoteTSI tsi = makeTSI();
		assertNotNull(tsi);
		assertEquals("UNICORE TSI at 127.0.0.1",tsi.getFileSystemIdentifier());
	}

	@Test
	public void testForwardingSSL() throws Exception {
		RemoteTSI tsi = makeTSI();
		SocketChannel s = tsi.openConnection("127.0.0.1:"+echo.getServerPort());
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

	@BeforeAll
	public static void startBackend() throws Exception {
		echo = new EchoServer();
		echo.start();
	}

	@AfterAll
	public static void stopBackend() {
		if(echo!=null)try{
			echo.shutdown();
		}catch(Exception e) {}
	}

}