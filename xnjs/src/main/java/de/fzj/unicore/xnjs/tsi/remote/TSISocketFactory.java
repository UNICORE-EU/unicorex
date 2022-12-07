package de.fzj.unicore.xnjs.tsi.remote;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.ServerSocketChannel;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.io.XNJSSocketFactory;
import de.fzj.unicore.xnjs.util.IOUtils;

/**
 * Small helper to create XNJS server and client sockets for communication with TSI. 
 */
public class TSISocketFactory extends XNJSSocketFactory implements AutoCloseable, Closeable {
	
	private ServerSocket server;

	private final int myPort;

	// this is used to handle the case that we have an SSL config defined, 
	// but still want to DISable SSL for the XNJS/TSI link
	private final boolean disableSSL;
	
	public TSISocketFactory(XNJS xnjs, int myPort)throws Exception{
		super(xnjs);
		this.myPort=myPort;
		TSIProperties tsiProps = xnjs.get(TSIProperties.class);
		disableSSL=tsiProps.getBooleanValue(TSIProperties.TSI_DISABLE_SSL);
		server = createServer();
	}

	/**
	 * close and re-open the server socket - use wisely
	 */
	public void reInit() throws Exception {
		IOUtils.closeQuietly(server);
		server = createServer();
	}
	
	private ServerSocket createServer()throws IOException{
		ServerSocketChannel ssc = ServerSocketChannel.open();
		ssc.bind(new InetSocketAddress(myPort));
		return ssc.socket();
	}

	public Socket accept()throws IOException{
		Socket s = server.accept();
		if(useSSL()) {
			SSLSocketFactory ssf = getSSLContext().getSocketFactory();
			InetSocketAddress peer = (InetSocketAddress)s.getRemoteSocketAddress();
			SSLSocket ssl = (SSLSocket)ssf.createSocket(s, peer.getHostName(), peer.getPort(), true);
			ssl.setUseClientMode(false);
			ssl.startHandshake();
			return ssl;
		}
		else return s;
	}
	
	/**
	 * wait for a connection and return the socket
	 * 
	 * if init is <code>false</code> the raw socket is returned, without
	 * any SSL support
	 
	 */ 
	public Socket accept(boolean init)throws IOException{
		if(init)return accept();
		else {
			return server.accept();
		}
	}

	public void setSoTimeout(int timeout)throws SocketException{
		server.setSoTimeout(timeout);
	}

	public void close()throws IOException{
		IOUtils.closeQuietly(server);
	}

	public boolean useSSL(){
		return super.useSSL() && !disableSSL;
	}
}