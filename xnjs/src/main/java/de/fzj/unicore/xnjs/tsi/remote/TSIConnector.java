package de.fzj.unicore.xnjs.tsi.remote;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.xnjs.util.IOUtils;
import de.fzj.unicore.xnjs.util.LogUtil;
import eu.unicore.util.Log;

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
			log.info("Creating new TSI connection to "+address+":"+port+" this is <"+(1+counter.get())+">");
			TSIConnection c = doCreateNewTSIConnection(server);
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
		server.setSoTimeout(connectTimeout);
		actualTSIAddress=signalShepherd(server, "newtsiprocess "+replyport+"\n");

		// Wait for connection requests (commands first, then data)
		Socket commands_socket = server.accept();
		Socket data_socket = server.accept();

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
				server.reInit();
			}catch(Exception ex) {}
			throw new IOException(msg);
		}

		// and want them both to be from the correct place
		if(!no_check && !commands_socket.getInetAddress().equals(actualTSIAddress)) {
			String msg = "Invalid new TSI connection (wrong machine). "
					+ "Expected: "+actualTSIAddress
					+ "Got: " +commands_socket.getInetAddress()
					+ ". Contact site administration!";
			IOUtils.closeQuietly(data_socket);
			IOUtils.closeQuietly(commands_socket);
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
	
	public String toString(){
		return "TSI connector @ "+address+":"+port;
	}

	/**
	 * signal the TSI that we want a new TSI process
	 * @return the peer address (which may be different the from tsiHost parameter in some cases like DNS level redirects)
	 * @throws Exception
	 */
	private InetAddress signalShepherd(TSISocketFactory server, String message) throws IOException {
		if(log.isDebugEnabled()){
			log.debug("Signalling TSI at "+address+":"+port+" : "+message);
		}
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
