package de.fzj.unicore.xnjs.tsi.remote;

import java.net.InetAddress;

import org.junit.Ignore;

import eu.unicore.uftp.server.UFTPServer;

@Ignore
public class UFTPDServerRunner {

	public int jobPort = 62434;

	public int srvPort = 62435;

	public InetAddress host;

	private UFTPServer server;
	
	public UFTPDServerRunner(int serverPort, int jobPort) {
		this.srvPort = serverPort;
		this.jobPort = jobPort;
		try {
			 host = InetAddress.getByName("localhost");
		}catch(Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	public UFTPDServerRunner() {
		try {
			 host = InetAddress.getByName("localhost");
		}catch(Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	public void start() throws Exception {
		System.out.println("Starting UFTPD server.");
		server = new UFTPServer(host, jobPort, host, srvPort);
		Thread serverThread = new Thread(server);
		serverThread.setName("UFTPD-Server-Thread");
		serverThread.start();
		System.out.println("Started UFTPD server.");
	}

	public void stop() throws Exception {
		server.stop();
		System.out.println("Stopped UFTPD server.");
	}

}
