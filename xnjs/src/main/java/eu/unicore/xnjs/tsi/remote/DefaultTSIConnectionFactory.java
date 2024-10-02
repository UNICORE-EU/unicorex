package eu.unicore.xnjs.tsi.remote;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Logger;

import eu.unicore.security.Client;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.PropertyChangeListener;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.tsi.IExecution;
import eu.unicore.xnjs.tsi.TSIUnavailableException;
import eu.unicore.xnjs.util.LogUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * Creates and pools connections to a UNICORE TSI server. Multiple TSI nodes are supported.
 * 
 * @author schuller
 */
@Singleton
public class DefaultTSIConnectionFactory implements TSIConnectionFactory, PropertyChangeListener {

	private static final Logger log=LogUtil.getLogger(LogUtil.TSI,DefaultTSIConnectionFactory.class);

	private final XNJS xnjs;

	//count how many connections are currently alive
	private final AtomicInteger liveConnections=new AtomicInteger(0);

	private final Map<String,TSIConnector> connectors = new HashMap<>();
	private TSIConnector[] connectorList;

	private final Set<String> tsiHostCategories = new HashSet<>();

	private final TSIProperties tsiProperties;

	private final ConnectionPool connectionPool;

	private TSISocketFactory server=null;

	// TSI machine
	private String machineID="";

	private String tsiDescription="";

	private volatile boolean isRunning = false;

	private String tsiVersion = null;

	@Inject
	public DefaultTSIConnectionFactory(XNJS xnjs){
		this.xnjs = xnjs;
		this.tsiProperties = xnjs.get(TSIProperties.class);
		this.connectionPool = new ConnectionPool(tsiProperties);
		start();
	}

	@Override
	public TSIConnection getTSIConnection(String user, String group, String preferredHost, int timeout)
			throws TSIUnavailableException{
		if(!isRunning)throw new TSIUnavailableException();
		if(user==null)throw new IllegalArgumentException("Required UNIX user ID is null (security setup problem?)");
		TSIConnection conn = connectionPool.get(preferredHost);
		if(conn==null){
			conn = createNewTSIConnection(preferredHost);
		}
		conn.setUser(user, group);
		conn.startUse();
		return conn;
	}

	@Override
	public TSIConnection getTSIConnection(Client client, String preferredHost, int timeout)
			throws TSIUnavailableException{
		if(!isRunning)throw new TSIUnavailableException();
		String user = client.getXlogin().getUserName();
		String group = TSIMessages.prepareGroupsString(client);
		return getTSIConnection(user, group, preferredHost, timeout);
	}

	protected synchronized TSIConnection createNewTSIConnection(String preferredHost) throws TSIUnavailableException {
		int limit = tsiProperties.getIntValue(TSIProperties.TSI_WORKER_LIMIT);
		if(limit>0 && liveConnections.get()>=limit){
			throw new TSIUnavailableException(preferredHost);
		}
		log.debug("Creating new TSIConnection to <{}>", preferredHost);
		TSIConnection connection = null;
		connection = preferredHost==null? doCreate() : doCreate(preferredHost);
		liveConnections.incrementAndGet();
		return connection;
	}

	// index of last used TSI connector
	private RollingIndex pos = null;

	private TSIConnection doCreate() throws TSIUnavailableException {
		// try all configured TSI hosts at least once
		for(int i=0;i<connectorList.length;i++){
			TSIConnector c = connectorList[pos.next()];
			try{
				return c.createNewTSIConnection(server);
			}catch(Exception ex){
				log.debug("{} is not available: {}", c, ex.getMessage());
			}
		}
		// all TSIs are unavailable
		throw new TSIUnavailableException();
	}

	private TSIConnector getConnector(String preferredHost) {
		List<String>candidates = getTSIHostNames(preferredHost, connectors.values());
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
		List<String>candidates = getTSIHostNames(preferredHost, connectors.values());
		Exception lastException = null;
		// try all matching TSI hosts at least once
		for(String name: candidates){
			TSIConnector c = connectors.get(name);
			try{
				return c.createNewTSIConnection(server);
			}catch(Exception ex){
				log.debug("{} is not available: {}", c, ex.getMessage());
				lastException=ex;
			}
		}
		throw new TSIUnavailableException(preferredHost, lastException);
	}

	@Override
	public void done(TSIConnection connection){
		if(connection!=null && !connection.isShutdown()){
			connection.endUse();
			connectionPool.offer(connection);
		}
	}

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
			configure();
			isRunning = true;
			log.info("UNICORE TSI connector (SSL: {}, listening on port {}, BSS user: '{}')",
					server.useSSL(), tsiProperties.getTSIMyPort(), tsiProperties.getBSSUser());
			log.info("TSI connection: {}", getConnectionStatus());
			tsiProperties.addPropertyChangeListener(this);
		}
		catch(Exception ex){
			throw new RuntimeException("Cannot setup UNICORE TSI connector" ,ex);
		}
		try {
			// force initialisation of the Execution class
			xnjs.get(IExecution.class);
		}catch(Exception ex) {}
	}

	protected void configure() throws ConfigurationException {
		try {
			if(server==null) {
				server = new TSISocketFactory(xnjs);
			}else {
				synchronized (server) {
					server.reInit();
				}
			}
			new TSIConfigurator(tsiProperties, this).configure(connectors, tsiHostCategories);
			connectorList = connectors.values().toArray(TSIConnector[]::new);
			StringBuilder machineSpec = new StringBuilder();
			for(String name: connectors.keySet()) {
				TSIConnector cc = connectors.get(name);
				if(machineSpec.length()>0)machineSpec.append(",");
				machineSpec.append(cc.getHostname());
			}
			machineID = machineSpec.toString();
			machineSpec.append(", XNJS listens on port ").append(tsiProperties.getTSIMyPort());
			tsiDescription = machineSpec.toString();
			pos = new RollingIndex(connectors.size());
			connectionPool.configure(connectors.values());
		}catch(Exception ex) {
			throw new ConfigurationException("Error (re-)configuring remote TSI connector.",ex);
		}
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
		connectionPool.shutdown();
	}

	/**
	 * how many connections do we have available
	 */
	public int getNumberOfPooledConnections(){
		return connectionPool.getNumberOfPooledConnections();
	}

	public int getLiveConnections(){
		return liveConnections.intValue();
	}

	public TSIProperties getTSIProperties() {
		return tsiProperties;
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
	public Collection<String> getTSIHostCategories(){
		return Collections.unmodifiableSet(tsiHostCategories);
	}

	@Override
	public String getTSIMachine(){
		return machineID;
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
			// no connectors
			throw new TSIUnavailableException();
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

	public static List<String> getTSIHostNames(String preferredHost, Collection<TSIConnector> connectors) {
		List<String>candidates = new ArrayList<>();
		String categoryPattern = null;
		String hostnamePattern = preferredHost;
		if(preferredHost!=null && preferredHost.contains(":")) {
			categoryPattern = preferredHost.split(":")[1];
		}
		for(TSIConnector conn: connectors){
			String name = conn.getHostname();
			if(categoryPattern!=null) {
				if(matches(categoryPattern, conn.getCategory())) {
					candidates.add(name);
				}
			}
			else if(matches(hostnamePattern, name)) {
				candidates.add(name);
			}
		}
		if(candidates.size()==0){
			throw new IllegalArgumentException("No TSI is configured at '"+preferredHost+"'");
		}
		if(candidates.size()>1)Collections.shuffle(candidates);
		return candidates;
	}

	public static boolean matches(String preferredHost, String actualHost) {
		if(preferredHost==null)return true;
		if(actualHost==null)return false;
		if(preferredHost.contains("*") || preferredHost.contains("?")) {
			return FilenameUtils.wildcardMatch(actualHost, preferredHost);
		}
		return actualHost.equals(preferredHost) || actualHost.startsWith(preferredHost+".");
	}


	public static class RollingIndex {
		private final int max;
		private int count = -1; // want to start at zero

		public RollingIndex(int max) {
			this.max = max;
		}

		public synchronized int next() {
			count = (count + 1) % max;
			return count;
		}
	}
}
