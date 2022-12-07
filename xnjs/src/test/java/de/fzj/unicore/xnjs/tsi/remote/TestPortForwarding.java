package de.fzj.unicore.xnjs.tsi.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.channels.SocketChannel;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import eu.unicore.util.ChannelUtils;

/**
 *  this tests forwarding for a non-SSL TSI connection
 *  @see TestTSISSL for the SSL version
 */
public class TestPortForwarding extends RemoteTSITestCase {

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

	@Test
	public void testForwarding() throws Exception {
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
}