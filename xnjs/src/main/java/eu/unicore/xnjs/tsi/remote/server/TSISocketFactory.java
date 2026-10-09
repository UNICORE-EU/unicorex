package eu.unicore.xnjs.tsi.remote.server;

import java.io.Closeable;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;

import eu.unicore.util.Pair;
import eu.unicore.util.SSLSocketChannel;
import eu.unicore.util.httpclient.HostnameMismatchCallbackImpl;
import eu.unicore.util.httpclient.IClientConfiguration;
import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.tsi.remote.TSIProperties;
import eu.unicore.xnjs.util.LogUtil;
import io.imunity.tanl.x509.impl.SocketFactoryCreator2;

/**
 * Helper class handling all communications with the main TSI server process(es)
 * using a single callback server socket
 */
public class TSISocketFactory implements Closeable {

	private static final Logger log=LogUtil.getLogger(LogUtil.TSI, TSISocketFactory.class);

	private final XNJS xnjs;

	private final TSIProperties tsiProps;

	private ServerSocket server;

	private int myPort;

	private boolean disableSSL;

	private int connectTimeout;

	public TSISocketFactory(XNJS xnjs)throws Exception{
		this.xnjs = xnjs;
		this.tsiProps = xnjs.get(TSIProperties.class);
		reInit();
	}

	/**
	 * send a message to the main TSI process
	 * 
	 * @return the peer address (which may be different the from address parameter in some cases like DNS level redirects)
	 * @throws Exception
	 */
	private InetAddress messageTSI(String message, InetAddress address, int port) throws IOException {
		log.debug("Messaging main TSI process at {}:{} <{}>", address, port, message);
		Socket s = createSocket(address, port);
		s.getOutputStream().write(message.getBytes());
		s.getOutputStream().flush();
		// Read from the TSI daemon, just an ack that all is OK
		try {s.getInputStream().read(); s.close();} catch(IOException ex) {}
		return s.getInetAddress();
	}

	/**
	 * create a command/data socket pair on the given TSI host
	 * @param address - TSI host
	 * @param port - TSI port
	 * @return Pair of command and data sockets
	 * @throws IOException
	 */
	public synchronized Pair<Socket, Socket> createSocketPair(InetAddress address, int port)throws IOException{
		InetAddress actualTSIAddress = null;
		int replyport = tsiProps.getTSIMyPort();
		Socket commands_socket = null;
		Socket data_socket = null;
		actualTSIAddress = messageTSI("newtsiprocess "+replyport+"\n", address, port);
		// Wait for TSI callback (commands first, then data)
		commands_socket = accept();
		try {
			data_socket = accept();
		} catch(IOException ioe) {
			IOUtils.closeQuietly(commands_socket);
			throw ioe;
		}
		boolean no_check = tsiProps.getBooleanValue(TSIProperties.TSI_NO_CHECK);
		// Make sure that pair comes from same machine
		if(!no_check && !commands_socket.getInetAddress().equals(data_socket.getInetAddress())) {
			String err = "TSI problem: data/command socket address mismatch"
					+ " Data: "+data_socket.getInetAddress()
					+ " Cmd:  " +commands_socket.getInetAddress()
					+ ". Contact site administration!";
			IOUtils.closeQuietly(data_socket, commands_socket);
			try {
				// just in case the connect/accept mechanism is messed up 
				// for some reason (like tsi restarts)
				reInit();
			}catch(Exception ex) {}
			throw new IOException(err);
		}

		// and want them both to be from the correct place
		if(!no_check && !commands_socket.getInetAddress().equals(actualTSIAddress)) {
			String err = "Invalid new TSI connection (wrong machine). "
					+ "Expected: "+actualTSIAddress
					+ " Got: " +commands_socket.getInetAddress()
					+ ". Contact site administration!";
			IOUtils.closeQuietly(commands_socket, data_socket);
			try {
				// just in case the connect/accept mechanism is messed up 
				// for some reason (like tsi restarts)
				reInit();
			}catch(Exception ex) {}
			throw new IOException(err);
		}
		return new Pair<>(commands_socket, data_socket);
	}

	/**
	 * port forwarding: create a SocketChannel for communicating to the
	 * application at "serviceAddress"
	 *
	 * @param serviceAddress
	 * @param user
	 * @param group
	 * @param tsiHost
	 * @param tsiPort
	 * @return
	 * @throws IOException
	 */
	public synchronized SocketChannel connectToService(String serviceAddress, String user, String group,
			InetAddress tsiHost, int tsiPort)
			throws IOException {
		InetAddress actualTSIAddress=null;
		int connectTimeout = 1000 * tsiProps.getIntValue(TSIProperties.TSI_CONNECT_TIMEOUT);
		int replyport = tsiProps.getTSIMyPort();
		SocketChannel base = null;
		SocketChannel result = null;
		server.setSoTimeout(connectTimeout);
		String msg = String.format("start-forwarding %s %s %s %s\n", replyport, serviceAddress, user, group);
		actualTSIAddress = messageTSI(msg, tsiHost, tsiPort);
		base = accept(false).getChannel();
		if(useSSL()) {
			SSLEngine engine = getSSLContext().createSSLEngine(tsiHost.getHostName(), tsiPort);
			engine.setUseClientMode(false);
			result = new SSLSocketChannel(base, engine, null);
			result.finishConnect();
		}
		else {
			result = base;
		}
		boolean no_check = tsiProps.getBooleanValue(TSIProperties.TSI_NO_CHECK);
		if(!no_check) {
			// want socket to be from the correct place
			InetSocketAddress remoteAddr = (InetSocketAddress)base.getRemoteAddress();
			if(!remoteAddr.getAddress().equals(actualTSIAddress)) {
				String err = "Invalid new TSI forwarding socket (wrong machine). "
						+ "Expected: " + actualTSIAddress
						+ "Got: "  + remoteAddr.getAddress()
						+ ". Contact site administration!";
				IOUtils.closeQuietly(result);
				try {
					// just in case the connect/accept mechanism is messed up
					// for some reason (like tsi restarts)
					reInit();
				}catch(Exception ex) {}
				throw new IOException(err);
			}
		}
		result.configureBlocking(false);
		return result;
	}

	/**
	 * set a variable on the TSI main process
	 * @param key
	 * @param value
	 * @param tsiHost
	 * @param tsiPort
	 * @throws IOException
	 */
	public synchronized void set(String key, String value, InetAddress tsiHost, int tsiPort) throws IOException {
		messageTSI("set "+key+" "+value+"\n", tsiHost, tsiPort);
	}
	
	/**
	 * close and re-open the server socket - use wisely
	 */
	public synchronized void reInit() throws Exception {
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
		server.setSoTimeout(connectTimeout);
	}
	
	private ServerSocket createServer(int myPort)throws IOException{
		ServerSocketChannel ssc = ServerSocketChannel.open();
		ssc.bind(new InetSocketAddress(myPort));
		return ssc.socket();
	}

	private Socket accept()throws IOException{
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
	private Socket accept(boolean init)throws IOException{
		if(init)return accept();
		else {
			return server.accept();
		}
	}

	public void close()throws IOException{
		IOUtils.closeQuietly(server);
	}

	private SSLContext sslContext = null;

	private Socket createSocket(InetAddress source_addr, int port) throws IOException{
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

	private SSLContext getSSLContext() throws IOException{
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