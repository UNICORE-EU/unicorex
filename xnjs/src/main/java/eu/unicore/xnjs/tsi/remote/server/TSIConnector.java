package eu.unicore.xnjs.tsi.remote.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.Logger;

import eu.unicore.util.Log;
import eu.unicore.util.Pair;
import eu.unicore.xnjs.tsi.remote.IConnector;
import eu.unicore.xnjs.tsi.remote.TSIConnectionFactory;
import eu.unicore.xnjs.tsi.remote.TSIProperties;
import eu.unicore.xnjs.util.LogUtil;

/**
 * Connects to a TSI daemon on a given host and port
 *
 * @author schuller
 */
public class TSIConnector implements IConnector {

	private static final Logger log = LogUtil.getLogger(LogUtil.TSI,TSIConnector.class);

	private final String hostname;
	private final InetAddress address;
	private final int port;
	private final String category;
	private final TSIProperties properties;
	private final TSIConnectionFactory factory;
	private final AtomicInteger counter = new AtomicInteger(0);

	/**
	 * create a new TSI connector
	 *
	 * @param factory
	 * @param properties
	 * @param tsiAddress - address of the TSI to connect to
	 * @param tsiPort - port of the TSI to connect to
	 * @param hostname - hostname from configuration
	 * @param category - category from configuration
	 */
	public TSIConnector(TSIConnectionFactory factory, TSIProperties properties,
			InetAddress tsiAddress, int tsiPort, String hostname, String category){
		this.factory = factory;
		this.properties = properties;
		this.address = tsiAddress;
		this.port = tsiPort;
		this.hostname = hostname;
		this.category = category;
		this.waitingPeriod = 5 * properties.getStatusUpdateInterval();
	}

	@Override
	public String getHostname() {
		return hostname;
	}

	public InetAddress getAddress() {
		return address;
	}

	@Override
	public String getCategory() {
		return category;
	}

	public ServerTSIConnection createNewTSIConnection(TSISocketFactory server)throws IOException{
		if(!isOK()){
			throw new IOException(statusMessage);
		}
		try{
			log.debug("Contacting TSI at {}:{}", address, port);
			ServerTSIConnection c = doCreateNewTSIConnection(server);
			log.info("Created new TSI connection to {}:{} this is <{}>", address, port, counter.get());
			OK();
			return c;
		}
		catch(IOException ex){
			notOK(Log.createFaultMessage("Can't create connection to "+this, ex));
			throw ex;
		}
	}

	public void set(TSISocketFactory server, String key, String value) throws IOException {
		try {
			server.set(key, value, address, port);
		}catch(IOException ex) {
			notOK(Log.createFaultMessage("Can't set parameter on TSI"+this, ex));
			throw ex;
		}
	}

	/**
	 * try to create a connection
	 * @param server
	 * @throws IOException
	 */
	private ServerTSIConnection doCreateNewTSIConnection(TSISocketFactory server)throws IOException{
		Pair<Socket,Socket> socks = server.createSocketPair(address, port);
		int connectTimeout = 1000 * properties.getIntValue(TSIProperties.TSI_CONNECT_TIMEOUT);
		int readTimeout = 1000 * properties.getIntValue(TSIProperties.TSI_TIMEOUT);
		ServerTSIConnection newConn = new ServerTSIConnection(socks.getM1(), socks.getM2(), this);
		newConn.setTimeouts(readTimeout, true);
		newConn.setPingTimeout(connectTimeout);
		newConn.getTSIVersion();
		newConn.setConnectionID(address+":"+port+"_"+counter.incrementAndGet());
		return newConn;
	}

	public SocketChannel connectToService(TSISocketFactory server, String serviceAddress, String user, String group)throws IOException{
		if(!isOK()){
			throw new IOException(statusMessage);
		}
		try{
			log.debug("Contacting TSI at {}", address);
			SocketChannel s = server.connectToService(serviceAddress, user, group, address, port);
			log.info("Started port forwarding to {}", serviceAddress);
			OK();
			return s;
		}
		catch(IOException ex){
			String msg = Log.createFaultMessage("Can't create connection to "+this, ex);
			notOK(msg);
			throw ex;
		}
	}

	public String toString(){
		return "TSI connector @ "+address+":"+port;
	}

	private boolean ok = true;

	private long disabledAt = 0;

	// waiting period in seconds
	private long waitingPeriod = 60;

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
			if(disabledAt+(1000*waitingPeriod)<System.currentTimeMillis()){
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

	public TSIConnectionFactory getFactory() {
		return factory;
	}
}