package de.fzj.unicore.xnjs.tsi.remote;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Logger;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.tsi.IExecution;
import de.fzj.unicore.xnjs.tsi.TSIUnavailableException;
import de.fzj.unicore.xnjs.util.LogUtil;
import eu.unicore.security.Client;
import eu.unicore.util.Log;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.PropertyChangeListener;

/**
 * Creates and pools connections to a UNICORE TSI server. Multiple TSI nodes are supported.
 * 
 * @author schuller
 */
@Singleton
public class DefaultTSIConnectionFactory implements TSIConnectionFactory, PropertyChangeListener {

	private static final Logger log=LogUtil.getLogger(LogUtil.TSI,DefaultTSIConnectionFactory.class);

	protected TSIProperties tsiProperties;

	private final Map<String, List<TSIConnection>> pool = new HashMap<>();
	
	// count pool content by TSI host name
	private final Map<String,AtomicInteger> connectionCounter = new HashMap<>(); 

	private final Map<String,TSIConnector> connectors = new HashMap<>();

	//current "position" in the TSI connector pool
	private int pos=0;

	private TSISocketFactory server=null;

	//TSI machine as given in config file
	private String machine="";

	private String tsiDescription="";

	//count how many connections are currently alive
	private final AtomicInteger liveConnections=new AtomicInteger(0);
	
	// how many connections to "keep alive" in the pool
	private int keepConnections = 4;

	private volatile boolean isRunning = false;

	private String tsiVersion=null;

	protected final XNJS configuration;

	@Inject
	public DefaultTSIConnectionFactory(XNJS config){
		this.configuration=config;
		start();
	}

	@Override
	public TSIConnection getTSIConnection(String user, String group, String preferredHost, int timeout)throws TSIUnavailableException{
		if(!isRunning)throw new TSIUnavailableException();
		if(user==null)throw new IllegalArgumentException("Required UNIX user ID is null (security setup problem?)");
		TSIConnection conn = getFromPool(preferredHost);
		if(conn==null){
			conn = createNewTSIConnection(preferredHost);
		}
		if(group==null)group="NONE";
		conn.setIdLine(user+" "+group);
		conn.startUse();
		return conn;
	}

	// get a live connection to the preferred host, or null if none available
	protected TSIConnection getFromPool(String preferredHost){
		TSIConnection conn;
		while(true) {
			conn = doGetFromPool(preferredHost);
			if(conn==null || conn.isAlive()) break;
			log.debug("Removing connection {}", conn.getConnectionID());
			conn.shutdown();
		}
		return conn;
	}

	// get a connection to the preferred host, or null if none available
	private TSIConnection doGetFromPool(String preferredHost) {
		TSIConnection conn = null;
		List<String>candidates = getTSIHostNames(preferredHost);
		synchronized(pool){
			for(String host: candidates) {
				List<TSIConnection> connections = getOrCreateConnectionList(host);
				if(connections.size()>0) {
					conn = connections.remove(0);
					break;
				}
			}	
		}
		if(conn!=null){
			connectionCounter.get(conn.getTSIHostName()).decrementAndGet();
		}
		return conn;
	}

	public static boolean matches(String preferredHost, String actualHost) {
		if(preferredHost==null)return true;
		if(preferredHost.contains("*") || preferredHost.contains("?")) {
			return FilenameUtils.wildcardMatch(actualHost, preferredHost);
		}
		return actualHost.equals(preferredHost) || actualHost.startsWith(preferredHost+".");
	}
	
	
	@Override
	public TSIConnection getTSIConnection(Client client, String preferredHost, int timeout)throws TSIUnavailableException{
		if(!isRunning)throw new TSIUnavailableException("TSI server is shutting down.");
		String user = client.getXlogin().getUserName();
		String group = TSIMessages.prepareGroupsString(client);
		return getTSIConnection(user,group,preferredHost,timeout);
	}

	protected synchronized TSIConnection createNewTSIConnection(String preferredHost) throws TSIUnavailableException {
		int limit = tsiProperties.getIntValue(TSIProperties.TSI_WORKER_LIMIT);
		if(limit>0 && liveConnections.get()>=limit){
			throw new TSIUnavailableException("Too many TSI processes are " +
					"currently in use (configured limit of <"+limit+"> was reached)");
		}
		log.debug("Creating new TSIConnection to <{}>", preferredHost);
		TSIConnection connection = null;
		connection = preferredHost==null? doCreate() : doCreate(preferredHost);
		liveConnections.incrementAndGet();
		return connection;
	}

	private TSIConnection doCreate() throws TSIUnavailableException {
		Exception lastException=null;
		//try all configured TSI hosts at least once
		TSIConnector[]conns = connectors.values().toArray(
				new TSIConnector[connectors.size()]);
		for(int i=0;i<conns.length;i++){
			TSIConnector c = conns[pos];
			pos++;
			if(pos>=conns.length)pos=0;

			//and try to make a connection
			try{
				return c.createNewTSIConnection(server);
			}catch(Exception ex){
				log.debug("{} is not available: {}", c, ex.getMessage());
				lastException=ex;
			}
		}
		//no luck, all TSIs are unavailable
		String msg;
		if(conns.length>1){
			msg = Log.createFaultMessage("None of the configured TSIs is available.",lastException);
		}
		else{
			msg = Log.createFaultMessage("TSI unavailable", lastException);
		}
		throw new TSIUnavailableException(msg);
	}

	private List<String> getTSIHostNames(String preferredHost) {
		List<String>candidates = new ArrayList<>();
		for(String name: connectors.keySet()){
			if(matches(preferredHost, name)){
				candidates.add(name);
			}
		}
		if(candidates.size()==0){
			throw new IllegalArgumentException("No TSI is configured at '"+preferredHost+"'");
		}
		if(candidates.size()>1)Collections.shuffle(candidates);
		return candidates;
	}
	
	private TSIConnector getConnector(String preferredHost) {
		List<String>candidates = getTSIHostNames(preferredHost);
		TSIConnector connector = null;
		for(String name: candidates){
			connector = connectors.get(name);
			if(connector!=null)break;
		}
		if(connector==null){
			throw new IllegalArgumentException("No TSI is configured at '"+preferredHost+"'");
		}
		return connector;
		
	}
	
	private TSIConnection doCreate(String preferredHost) throws TSIUnavailableException {
		List<String>candidates = getTSIHostNames(preferredHost);
		Exception lastException = null;
		//try all matching TSI hosts at least once
		for(String name: candidates){
			TSIConnector c = connectors.get(name);
			try{
				return c.createNewTSIConnection(server);
			}catch(Exception ex){
				log.debug("{} is not available: {}", c, ex.getMessage());
				lastException=ex;
			}
		}
		//no luck, all TSIs are unavailable
		String msg;
		if(connectors.size()>1){
			msg = Log.createFaultMessage("None of the requested TSIs is available.",lastException);
		}
		else{
			msg = Log.createFaultMessage("TSI unavailable", lastException);
		}
		throw new TSIUnavailableException(msg);
	}

	@Override
	public void done(TSIConnection connection){
		try{
			if(connection!=null && !connection.isShutdown()){
				connection.endUse();
				String name = connection.getTSIHostName();
				synchronized (pool){
					AtomicInteger count = connectionCounter.get(name);
					if(count.get()<keepConnections){
						getOrCreateConnectionList(name).add(connection);
						count.incrementAndGet();
					}
					else{
						connection.shutdown();
					}
				}
			}
		}catch(Exception ex){
			log.error(ex);
		}
	}

	private List<TSIConnection> getOrCreateConnectionList(String hostname){
		List<TSIConnection> connections = pool.get(hostname);
		if(connections==null) {
			connections = new ArrayList<>();
			pool.put(hostname, connections);
		}
		return connections;
	}

	//notify of connection death
	public void notifyConnectionDied(){
		liveConnections.decrementAndGet();
	}

	public boolean isRunning() {
		return isRunning;
	}
	/**
	 * startup the factory and create connectors to all configured TSI hosts
	 */
	public synchronized void start() {
		if(isRunning)return;
		try {
			tsiProperties = configuration.get(TSIProperties.class);
			configure();
			log.info("TSI connection factory:\n" +
					"  ** Talking to TSI at "+machine+"\n"+
					"  ** SSL is "+(server.useSSL()?"enabled":"disabled")+"\n"+
					"  ** Listening on port "+tsiProperties.getTSIMyPort()+"\n"+
					"  ** User id for querying list of jobs on BSS: '"+tsiProperties.getBSSUser()+"'");
			log.info("TSI connection: {}", getConnectionStatus());
			tsiProperties.addPropertyChangeListener(this);
			isRunning = true;
		}
		catch(Exception ex){
			throw new RuntimeException("Cannot setup TSI Connection Factory" ,ex);
		}
		try {
			 configuration.get(IExecution.class);
		}catch(Exception ex) {}
	}
	
	protected void configure() throws ConfigurationException {
		try {
			String newMachine = tsiProperties.getTSIMachine();
			int port = tsiProperties.getTSIPort();
			if(server==null) {
				server = new TSISocketFactory(configuration);
			}else {
				synchronized (server) {
					server.reInit();
				}
			}
			StringBuilder machineSpec = new StringBuilder();
			Collection<TSIConnector> newConnectors = createConnectors(newMachine, port, machineSpec);
			Collection<String>names = new ArrayList<>();
			for(TSIConnector tc: newConnectors) {
				names.add(tc.getHostname());
			}
			// handle removed TSI addresses
			Iterator<String>hosts = connectors.keySet().iterator();
			while(hosts.hasNext()) {
				String host = hosts.next();
				if(!names.contains(host)) {
					hosts.remove();
				}
			}
			// add new ones
			for(TSIConnector tc: newConnectors) {
				connectors.put(tc.getHostname(), tc);
				AtomicInteger oldCount = connectionCounter.get(tc.getHostname());
				if(oldCount==null) {
					connectionCounter.put(tc.getHostname(), new AtomicInteger(0));
				}
			}
			machine = machineSpec.toString(); // to also include port
			tsiDescription = machine+", XNJS listens on port "+tsiProperties.getTSIMyPort();
			keepConnections = tsiProperties.getIntValue(TSIProperties.TSI_POOL_SIZE);
		}catch(Exception ex) {
			throw new ConfigurationException("Error (re-)configuring remote TSI connector.",ex);
		}
	}

	protected Collection<TSIConnector> createConnectors(String machine, int port, StringBuilder machineSpec)
			throws Exception {
		Collection<TSIConnector> newConnectors = new ArrayList<>();
		// parse 'machine' to extract TSI addresses.
		// if the port is not included, use the port
		// given by the separate TSI_PORT property
		String[]tsiSpec = machine.split("[ ,]+");
		for(int i = 0; i < tsiSpec.length; ++i) {
			String[] split=tsiSpec[i].split(":");
			String host=split[0];
			int p = split.length>1 ? Integer.parseInt(split[1]) : port;
			if(p==-1)throw new IllegalArgumentException("Missing port for TSI machine: "+host);
			TSIConnector tsiConnector = createTSIConnector(host, p);
			newConnectors.add(tsiConnector);
			if(i>0)machineSpec.append(", ");
			machineSpec.append(host).append(":").append(p);
		}
		return newConnectors;
	}
	
	protected TSIConnector createTSIConnector(String hostname, int port) throws UnknownHostException {
		return new TSIConnector(this, tsiProperties, InetAddress.getByName(hostname), port, hostname);
	}

	/**
	 * shutdown the factory
	 */
	public void stop() {
		if(!isRunning)return;
		isRunning = false;
		//kill incoming socket
		try{
			log.info("Shutting down TSI listener socket");
			server.close();
		}catch(Exception ex){}
		synchronized (pool) {
			//kill pooled connections
			for(List<TSIConnection> cs: pool.values()){
				for(TSIConnection c: cs) {
					try{c.shutdown();}catch(Exception ex){}
				}
			}
			pool.clear();
		}
	}

	/**
	 * how many connections do we have available
	 */
	public int getNumberOfPooledConnections(){
		int size = 0;
		for(List<TSIConnection> l: pool.values()) {
			size += l.size();
		}
		return size;
	}

	public int getLiveConnections(){
		return liveConnections.intValue();
	}

	public String getConnectionStatus(){
		if(!isRunning){
			return "N/A [not started]";
		}
		StringBuilder sb=new StringBuilder();
		int numOK = 0;
		int numTotal = connectors.size();
		
		for(String h: connectors.keySet()){
			try(TSIConnection conn = getTSIConnection("nobody", null, h, -1)){
				String version = conn.getTSIVersion();
				numOK++;
				tsiVersion = version;
			}
			catch(Exception e){}
		}
		if(numOK>0){
			sb.append("OK [TSI v").append(tsiVersion).append(" (").append(numOK).append("/").append(numTotal)
			.append(" nodes up) at ").append(tsiDescription).append("]");
		}
		else{
			sb.append("NOT OK [none of the configured TSIs is available").append("]");
		}
		sb.append("]");
		return sb.toString();
	}

	@Override
	public Collection<String> getTSIHosts(){
		return connectors.keySet();
	}

	@Override
	public String getTSIMachine(){
		return machine;
	}

	@Override
	public Map<String,String>getTSIConnectorStates(){
		Map<String,String> res = new HashMap<>();
		for(TSIConnector c: connectors.values()) {
			res.put(c.toString(), c.getStatusMessage());
		}
		return res;
	}

	/**
	 * gets the TSI version as returned by the TSI_PING command
	 */
	@Override
	public synchronized String getTSIVersion(){
		if(tsiVersion==null){
			for(String h: connectors.keySet()){
				try(TSIConnection conn = getTSIConnection("nobody", null, h, -1)){
					tsiVersion = conn.getTSIVersion();
					if(tsiVersion!=null)break;
				}
				catch(Exception e){}
			}
		}
		return tsiVersion;
	}

	TSISocketFactory getTSISocketFactory() {
		return server;
	}
	
	@Override
	public SocketChannel connectToService(String serviceHost, int servicePort, String tsiHost, String user, String group)
			throws TSIUnavailableException, IOException{
		if(tsiHost!=null) {
			return getConnector(tsiHost).connectToService(server, serviceHost, servicePort, user, group);
		}else {
			for(TSIConnector connector: connectors.values()) {
				return connector.connectToService(server, serviceHost, servicePort, user, group);
			}
			// unreachable
			throw new TSIUnavailableException("No TSI connectors");
		}
	}

	public void changeSetting(String tsiHost, String key, String value) throws IOException, TSIUnavailableException {
		if(tsiHost!=null) {
			getConnector(tsiHost).set(server, key, value);
		}else {
			for(TSIConnector connector: connectors.values()) {
				connector.set(server, key, value);
			}
		}
	}

	@Override
	public String[] getInterestingProperties() {
		return null;
	}

	@Override
	public void propertyChanged(String propertyKey) {
		log.info("Re-configuring");
		configure();
	}

}
