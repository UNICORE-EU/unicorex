package de.fzj.unicore.xnjs.tsi.remote;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.ServerSocketChannel;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.util.IOUtils;
import eu.emi.security.authn.x509.impl.SocketFactoryCreator2;
import eu.unicore.util.httpclient.HostnameMismatchCallbackImpl;
import eu.unicore.util.httpclient.IClientConfiguration;
import eu.unicore.util.httpclient.ServerHostnameCheckingMode;

/**
 * Helper to create XNJS server and client sockets for communication with TSI. 
 */
public class TSISocketFactory implements AutoCloseable, Closeable {

	private final XNJS xnjs;

	private ServerSocket server;

	private int myPort;

	private boolean disableSSL;
	
	public TSISocketFactory(XNJS xnjs)throws Exception{
		this.xnjs = xnjs;
		reInit();
	}

	/**
	 * close and re-open the server socket - use wisely
	 */
	public void reInit() throws Exception {
		TSIProperties tsiProps = xnjs.get(TSIProperties.class);
		disableSSL = tsiProps.getBooleanValue(TSIProperties.TSI_DISABLE_SSL);
		IClientConfiguration security = xnjs.get(IClientConfiguration.class);
		if (!disableSSL && !security.isSslEnabled()) {
				throw new IllegalStateException("Can not enable SSL for XNJS: " +
						"no SSL configuration has been defined.");
		}
		myPort = tsiProps.getTSIMyPort();
		IOUtils.closeQuietly(server);
		sslContext = null;
		server = createServer(myPort);
	}
	
	private ServerSocket createServer(int myPort)throws IOException{
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

	private SSLContext sslContext = null;

	public Socket createSocket(InetAddress source_addr, int port) throws IOException{
		if(disableSSL){
			return new Socket(source_addr,port);
		}
		else{
			return getSSLContext().getSocketFactory().createSocket(source_addr, port);
		}
	}

	public Socket createSocket(String host, int port) throws IOException,UnknownHostException {
		if(disableSSL){
			return new Socket(host,port);
		}
		else{
			return getSSLContext().getSocketFactory().createSocket(host, port);
		}
	}

	public Socket createSocket(String host, int port, InetAddress localHost,
			int localPort) throws IOException, UnknownHostException {
		if(disableSSL){
			return new Socket(host,port,localHost,localPort);
		}
		else{
			return getSSLContext().getSocketFactory().createSocket(host,port,localHost,localPort);
		}
	}

	public Socket createSocket(InetAddress address, int port,
			InetAddress localAddress, int localPort) throws IOException {
		if(useSSL()){
			return getSSLContext().getSocketFactory().createSocket(address,port,localAddress,localPort);
		}
		else{
			return new Socket(address,port,localAddress,localPort);
		}
	}

	public synchronized SSLContext getSSLContext() throws IOException{
		if(sslContext==null){
			IClientConfiguration security = xnjs.get(IClientConfiguration.class);
			sslContext = new SocketFactoryCreator2(security.getCredential(),
					security.getValidator(),
					new HostnameMismatchCallbackImpl(ServerHostnameCheckingMode.WARN))
					.getSSLContext();
		}
		return sslContext;
	}

	public boolean useSSL(){
		return !disableSSL;
	}
	
}