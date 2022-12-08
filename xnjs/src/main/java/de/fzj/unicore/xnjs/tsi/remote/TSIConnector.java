package de.fzj.unicore.xnjs.tsi.remote;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.SSLEngine;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.xnjs.util.IOUtils;
import de.fzj.unicore.xnjs.util.LogUtil;
import eu.unicore.util.Log;
import eu.unicore.util.SSLSocketChannel;

/**
 * connects to a TSI daemon on a given host and port
 * 
 * @author schuller
 */
public class TSIConnector {
	
	private static final Logger log=LogUtil.getLogger(LogUtil.TSI,TSIConnector.class);

	private final String hostname;
	private final InetAddress address;
	private final int port;
	private final TSIProperties properties;
	private final TSIConnectionFactory factory;
	private final AtomicInteger counter = new AtomicInteger(0);

	/**
	 * 
	 * @param tsiAddress
	 * @param tsiPort
	 * @param hostname - the hostname used to lookup the address 
	 */
	public TSIConnector(TSIConnectionFactory factory, TSIProperties properties, InetAddress tsiAddress, int tsiPort, String hostname){
		this.factory = factory;
		this.properties = properties;
		this.address = tsiAddress;
		this.port = tsiPort;
		this.hostname = hostname;
	}
	
	public String getHostname() {
		return hostname;
	}
	
	public InetAddress getAddress() {
		return address;
	}
	
	public TSIConnection createNewTSIConnection(TSISocketFactory server)throws IOException{
		if(!isOK()){
			throw new IOException(statusMessage);
		}
		try{
			log.debug("Contacting TSI at {}:{}", address, port);
			TSIConnection c = doCreateNewTSIConnection(server);
			log.info("Created new TSI connection to {}:{} this is <{}>", address, port, 1+counter.get());
			OK();
			return c;
		}
		catch(IOException ex){
			String msg = Log.createFaultMessage("Can't create connection to "+this, ex);
			notOK(msg);
			throw ex;
		}
	}
	
	/**
	 * try to create a connection
	 * @param server
	 * @throws IOException
	 */
	private TSIConnection doCreateNewTSIConnection(TSISocketFactory server)throws IOException{
		TSIConnection newConn=null;
		// Ask shepherd for a new worker
		InetAddress actualTSIAddress=null;
		int connectTimeout = 1000 * properties.getIntValue(TSIProperties.TSI_CONNECT_TIMEOUT);
		int readTimeout = 1000 * properties.getIntValue(TSIProperties.TSI_TIMEOUT);
		int replyport = properties.getTSIMyPort();
		Socket commands_socket = null;
		Socket data_socket = null;
		synchronized(server) {
			server.setSoTimeout(connectTimeout);
			actualTSIAddress = signalShepherd(server, "newtsiprocess "+replyport+"\n");
			// Wait for TSI callback (commands first, then data)
			commands_socket = server.accept();
			try {
				data_socket = server.accept();
			} catch(IOException ioe) {
				IOUtils.closeQuietly(commands_socket);
				throw ioe;
			}
		}
		boolean no_check = properties.getBooleanValue(TSIProperties.TSI_NO_CHECK);
		// Make sure that pair comes from same machine
		if(!no_check && !commands_socket.getInetAddress().equals(data_socket.getInetAddress())) {
			String msg = "TSI problem: data/command socket address mismatch"
					+ "Data: "+data_socket.getInetAddress()
					+ "Cmd:  " +commands_socket.getInetAddress()
					+ ". Contact site administration!";
			IOUtils.closeQuietly(data_socket);
			IOUtils.closeQuietly(commands_socket);
			try {
				// just in case the connect/accept mechanism is messed up 
				// for some reason (like tsi restarts)
				synchronized(server) {
					server.reInit();
				}
			}catch(Exception ex) {}
			throw new IOException(msg);
		}

		// and want them both to be from the correct place
		if(!no_check && !commands_socket.getInetAddress().equals(actualTSIAddress)) {
			String msg = "Invalid new TSI connection (wrong machine). "
					+ "Expected: "+actualTSIAddress
					+ "Got: " +commands_socket.getInetAddress()
					+ ". Contact site administration!";
			IOUtils.closeQuietly(commands_socket, data_socket);
			try {
				// just in case the connect/accept mechanism is messed up 
				// for some reason (like tsi restarts)
				server.reInit();
			}catch(Exception ex) {}
			throw new IOException(msg);
		}

		newConn = new TSIConnection(commands_socket, data_socket, factory, this);
		newConn.setSocketTimeouts(readTimeout, true);
		newConn.setPingTimeout(connectTimeout);
		newConn.getTSIVersion();
		newConn.setConnectionID(address+":"+port+"_"+counter.incrementAndGet());
		return newConn;
	}

	public SocketChannel connectToService(TSISocketFactory server, String host, int port, String user, String group)throws IOException{
		if(!isOK()){
			throw new IOException(statusMessage);
		}
		try{
			if(host==null)host="localhost";
			log.debug("Contacting TSI at {}:{}", address, port);
			SocketChannel s = doConnectToService(server, host, port, user, group);
			log.info("Started port forwarding to {}:{}", host, port);
			OK();
			return s;
		}
		catch(IOException ex){
			String msg = Log.createFaultMessage("Can't create connection to "+this, ex);
			notOK(msg);
			throw ex;
		}
	}

	private SocketChannel doConnectToService(TSISocketFactory server, String host, int port, String user, String group)
			throws IOException {
		InetAddress actualTSIAddress=null;
		int connectTimeout = 1000 * properties.getIntValue(TSIProperties.TSI_CONNECT_TIMEOUT);
		int replyport = properties.getTSIMyPort();
		SocketChannel base = null;
		SocketChannel result = null;
		synchronized(server) {
			server.setSoTimeout(connectTimeout);
			String msg = String.format("start-forwarding %s %s:%s %s %s\n", replyport, host, port, user, group);
			actualTSIAddress = signalShepherd(server, msg);
			base = server.accept(false).getChannel();
			if(server.useSSL()) {
				SSLEngine engine = server.getSSLContext().createSSLEngine(hostname, port);
				engine.setUseClientMode(false);
				result = new SSLSocketChannel(base, engine, null);
				result.finishConnect();
			}
			else {
				result = base;
			}
		}
		boolean no_check = properties.getBooleanValue(TSIProperties.TSI_NO_CHECK);
		if(!no_check) {
			// want socket to be from the correct place
			InetSocketAddress remoteAddr = (InetSocketAddress)base.getRemoteAddress();
			if(!remoteAddr.getAddress().equals(actualTSIAddress)) {
				String msg = "Invalid new TSI forwarding socket (wrong machine). "
						+ "Expected: " + actualTSIAddress
						+ "Got: "  + remoteAddr.getAddress()
						+ ". Contact site administration!";
				IOUtils.closeQuietly(result);
				try {
					// just in case the connect/accept mechanism is messed up
					// for some reason (like tsi restarts)
					server.reInit();
				}catch(Exception ex) {}
				throw new IOException(msg);
			}
		}
		result.configureBlocking(false);
		return result;
	}

	public String toString(){
		return "TSI connector @ "+address+":"+port;
	}

	/**
	 * signal the TSI that we want a new TSI process
	 * @return the peer address (which may be different the from tsiHost parameter in some cases like DNS level redirects)
	 * @throws Exception
	 */
	private InetAddress signalShepherd(TSISocketFactory server, String message) throws IOException {
		log.debug("Signalling TSI at {}:{} : {}", address, port, message);
		Socket s = server.createSocket(address, port);
		s.getOutputStream().write(message.getBytes());
		s.getOutputStream().flush();
		final InetAddress actualTSIAddress=s.getInetAddress();
		// Read from the TSI daemon, just an ack that all is OK
		try {s.getInputStream().read(); s.close();} catch(IOException ex) {}
		return actualTSIAddress;
	}

	private boolean ok = true;

	private long disabledAt = 0;

	// waiting period in milliseconds, default is 1 minute
	private long waitingPeriod = 60 * 1000;

	private String statusMessage;
	
	/**
	 * Check the state of the circuit breaker. If "not OK", it will check whether the 
	 * waiting period has passed. If yes the circuit will be re-enabled.
	 * 
	 * @return <code>true</code> if the circuit breaker is OK
	 */
	public synchronized boolean isOK(){
		if(!ok){
			// check if waiting period has passed, and if yes
			// reset the state to "ok"
			if(disabledAt+waitingPeriod<System.currentTimeMillis()){
				OK();
			}
		}
		return ok;
	}

	public synchronized void notOK(String errorMessage){
		ok = false;
		disabledAt = System.currentTimeMillis();
		this.statusMessage = errorMessage;
	}

	/**
	 * reset the circuit breaker to "OK" mode
	 */
	private synchronized void OK(){
		ok = true;
		statusMessage = "OK";
	}

	public String getStatusMessage(){
		return statusMessage;
	}
}
