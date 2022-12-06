package de.fzj.unicore.xnjs.tsi.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPortForwarding extends RemoteTSITestCase {

	private static EchoServer echo = null;
	
	@BeforeClass
	public static void startUFTPD() throws Exception {
		echo = new EchoServer();
		echo.start();
	}

	@AfterClass
	public static void stopUFTPD() {
		if(echo!=null)try{
			echo.shutdown();
		}catch(Exception e) {}
	}

	@Test
	public void testForwarding() throws Exception {
		RemoteTSI tsi=(RemoteTSI)xnjs.getTargetSystemInterface(null);
		assertNotNull(tsi);
		Socket s = tsi.openConnection("127.0.0.1", echo.getServerPort());
		PrintWriter out = new PrintWriter(s.getOutputStream());
		BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
		System.out.println("Forwarding established");
		String line = "this is a test";
		System.out.println("--> "+line);
		out.write(line+"\n");
		out.flush();
		String reply = in.readLine();
		System.out.println("<-- "+reply);
		assertEquals(line, reply);
	}
}