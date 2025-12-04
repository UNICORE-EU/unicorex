package eu.unicore.xnjs.tsi.remote;

import java.net.InetAddress;

import org.junit.jupiter.api.Disabled;

import eu.unicore.uftp.server.UFTPServer;

@Disabled
public class UFTPDServerRunner {

	public int jobPort = 62434;

	public int srvPort = 62435;

	private InetAddress host;

	private UFTPServer server;

	public UFTPDServerRunner(int serverPort, int jobPort) {
		this.srvPort = serverPort;
		this.jobPort = jobPort;
		try {
			 host = InetAddress.getByName("0.0.0.0");
		}catch(Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public UFTPDServerRunner() {
		this(62435, 62434);
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
