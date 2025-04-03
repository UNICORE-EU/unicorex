package eu.unicore.xnjs.tsi.remote;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.channels.SocketChannel;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import eu.unicore.util.ChannelUtils;

/**
 *  this tests forwarding for a non-SSL TSI connection
 *  @see TestTSISSL for the SSL version
 */
public class TestPortForwarding extends RemoteTSITestCase {

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

	@Test
	public void testForwarding() throws Exception {
		RemoteTSI tsi=(RemoteTSI)xnjs.getTargetSystemInterface(null, null);
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