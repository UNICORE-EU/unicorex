package eu.unicore.xnjs.tsi.remote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author bjoernh
 */
public class EchoServer extends Thread {
	
	private boolean active = true;
	private final ServerSocket socket;
	
	public EchoServer() throws IOException {
		 socket = new ServerSocket(0, 0, InetAddress.getByName("127.0.0.1"));
	}
	
	public int getServerPort() {
		return socket.getLocalPort();
	}
	
	@Override
	public void run() {
		while(active) {
			try {
				Socket conn = socket.accept();
				System.out.println("ECHO: New connection from " + conn.getRemoteSocketAddress());
				new EchoConnection(conn).start();
			} catch (IOException e) {
				e.printStackTrace();
				// keep going
			}
			
		}
	}
	
	private class EchoConnection extends Thread {
		private final Socket conn;

		public EchoConnection(Socket _conn) {
			this.conn = _conn;
		}
		
		@Override
		public void run() {
			BufferedReader in;
			try {
				in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			} catch (IOException e1) {
				System.out.println("Error: "+e1);
				return;
			}
			OutputStream out;
			try {
				out = conn.getOutputStream();
			} catch (IOException e1) {
				System.out.println("Error: "+e1);
				return;
			}
			
			try {
				String line = in.readLine();
				if(line!=null) {
					System.out.println("ECHO: Echoing back: " + line);
					out.write((line+"\n").getBytes());
				}
			} catch (IOException e) {
				// we're done
			}
			System.out.println("ECHO: Closed connection with " + conn.getRemoteSocketAddress());
		}
	}

	public boolean isActive() {
		return active;
	}

	public void shutdown() {
		this.active = false;
	}
}
