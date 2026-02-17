package eu.unicore.xnjs.tsi.remote.server;

import java.io.Closeable;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.ServerSocketChannel;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.io.IOUtils;

import eu.emi.security.authn.x509.impl.SocketFactoryCreator2;
import eu.unicore.util.httpclient.HostnameMismatchCallbackImpl;
import eu.unicore.util.httpclient.IClientConfiguration;
import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.tsi.remote.TSIProperties;

/**
 * Helper to create XNJS server and client sockets for communication with TSI. 
 */
public class TSISocketFactory implements AutoCloseable, Closeable {

	private final XNJS xnjs;

	private ServerSocket server;

	private int myPort;

	private boolean disableSSL;

	private int connectTimeout;

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
		connectTimeout = 1000 * tsiProps.getIntValue(TSIProperties.TSI_CONNECT_TIMEOUT);
	}
	
	private ServerSocket createServer(int myPort)throws IOException{
		ServerSocketChannel ssc = ServerSocketChannel.open();
		ssc.bind(new InetSocketAddress(myPort));
		return ssc.socket();
	}

	public Socket accept()throws IOException{
		Socket s = server.accept();
		if(disableSSL) {
			return s;
		}
		else {
			SSLSocketFactory ssf = getSSLContext().getSocketFactory();
			InetSocketAddress peer = (InetSocketAddress)s.getRemoteSocketAddress();
			SSLSocket ssl = (SSLSocket)ssf.createSocket(s, peer.getHostName(), peer.getPort(), true);
			ssl.setUseClientMode(false);
			ssl.startHandshake();
			return ssl;
		}
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
		Socket s = null;
		if(disableSSL){
			s = new Socket();
		}
		else{
			s = getSSLContext().getSocketFactory().createSocket();
		}
		long now = System.currentTimeMillis();
		IOException ie = null;
		while(System.currentTimeMillis()<=now+connectTimeout) try {
			s.connect(new InetSocketAddress(source_addr, port), connectTimeout);
			break;
		}catch(ConnectException se) {
			try{
				ie = se;
				Thread.sleep(1000);
			}catch(Exception e) {}
		}
		if(ie!=null)throw ie;
		return s;
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