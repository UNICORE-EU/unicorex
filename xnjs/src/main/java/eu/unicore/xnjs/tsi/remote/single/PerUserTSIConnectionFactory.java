package eu.unicore.xnjs.tsi.remote.single;

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
import eu.unicore.xnjs.tsi.TSIUnavailableException;
import eu.unicore.xnjs.tsi.remote.TSIConnection;
import eu.unicore.xnjs.tsi.remote.TSIConnectionFactory;
import eu.unicore.xnjs.tsi.remote.TSIMessages;
import eu.unicore.xnjs.tsi.remote.TSIProperties;
import eu.unicore.xnjs.util.LogUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * Creates and pools connections to a UNICORE TSI server. Multiple TSI nodes are supported.
 * 
 * @author schuller
 */
@Singleton
public class PerUserTSIConnectionFactory implements TSIConnectionFactory, PropertyChangeListener {

	private static final Logger log=LogUtil.getLogger(LogUtil.TSI,PerUserTSIConnectionFactory.class);

	// count how many connections are currently alive
	private final AtomicInteger liveConnections=new AtomicInteger(0);

	private final Map<String,Connector> connectors = new HashMap<>();
	private Connector[] connectorList;

	private final Set<String> tsiHostCategories = new HashSet<>();

	private final TSIProperties tsiProperties;

	private final PerUserTSIProperties perUserTsiProperties;

	// TSI machine
	private String machineID = "";

	private volatile boolean isRunning = false;

	private String tsiVersion = null;

	private final ConnectionPool connectionPool;

	@Inject
	public PerUserTSIConnectionFactory(XNJS xnjs){
		this.tsiProperties = xnjs.get(TSIProperties.class);
		this.perUserTsiProperties = xnjs.get(PerUserTSIProperties.class);
		this.connectionPool = new ConnectionPool(tsiProperties);
		start();
	}

	@Override
	public UserTSIConnection getTSIConnection(String user, String group, String preferredHost, int timeout)
			throws TSIUnavailableException{
		if(!isRunning)throw new TSIUnavailableException();
		if(user==null)throw new IllegalArgumentException("Required UNIX user ID is null (security setup problem?)");
		UserTSIConnection conn = connectionPool.get(user, preferredHost);
		if(conn==null){
			conn = createNewTSIConnection(user, preferredHost);
		}
		return conn;
	}

	@Override
	public UserTSIConnection getTSIConnection(Client client, String preferredHost, int timeout)
			throws TSIUnavailableException{
		if(!isRunning)throw new TSIUnavailableException();
		String user = client.getXlogin().getUserName();
		String group = TSIMessages.prepareGroupsString(client);
		return getTSIConnection(user, group, preferredHost, timeout);
	}

	protected synchronized UserTSIConnection createNewTSIConnection(String user, String preferredHost) throws TSIUnavailableException {
		int limit = tsiProperties.getIntValue(TSIProperties.TSI_WORKER_LIMIT);
		if(limit>0 && liveConnections.get()>=limit){
			throw new TSIUnavailableException(preferredHost);
		}
		log.debug("Creating new TSIConnection to <{}>", preferredHost);
		UserTSIConnection connection = preferredHost==null ?
				doCreate(user) : doCreate(user, preferredHost);
		liveConnections.incrementAndGet();
		return connection;
	}

	// index of last used TSI connector
	private RollingIndex pos = null;

	private UserTSIConnection doCreate(String user) throws TSIUnavailableException {
		// try all configured TSI hosts at least once
		for(int i=0;i<connectorList.length;i++){
			Connector c = connectorList[pos.next()];
			try{
				return c.createConnection(user);
			}catch(Exception ex){
				log.debug("{} is not available: {}", c, ex.getMessage());
			}
		}
		// all TSIs are unavailable
		throw new TSIUnavailableException();
	}

	private UserTSIConnection doCreate(String user, String preferredHost) throws TSIUnavailableException {
		List<String>candidates = getTSIHostNames(preferredHost, connectors.values());
		Exception lastException = null;
		// try all matching TSI hosts at least once
		for(String name: candidates){
			Connector c = connectors.get(name);
			try{
				return c.createConnection(user);
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
			connectionPool.offer((UserTSIConnection)connection);
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
			log.info("UNICORE per-user TSI-via-SSH connector");
			log.info("TSI connection: {}", getConnectionStatus());
			tsiProperties.addPropertyChangeListener(this);
		}
		catch(Exception ex){
			throw new RuntimeException("Cannot setup UNICORE TSI connector" ,ex);
		}
		try {
			// TODO
			// force initialisation of the Execution class
			// xnjs.get(IExecution.class);
		}catch(Exception ex) {}
	}

	protected void configure() throws ConfigurationException {
		try {
			new Configurator(tsiProperties, perUserTsiProperties, this).
				configure(connectors, tsiHostCategories);
			connectorList = connectors.values().toArray(Connector[]::new);
			StringBuilder machineSpec = new StringBuilder();
			for(String name: connectors.keySet()) {
				Connector cc = connectors.get(name);
				if(machineSpec.length()>0)machineSpec.append(",");
				machineSpec.append(cc.getHostname());
			}
			machineID = machineSpec.toString();
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
		connectionPool.shutdown();
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
			if(connectors.get(h).isOK())numOK++;
		}
		if(numOK>0){
			sb.append("OK [PER-USER-TSI").append(" (").append(numOK).append("/").append(numTotal)
			.append(" nodes up)");
			if(numTotal-numOK>0) {
				for(Connector c: connectors.values()) {
					if(!c.isOK()) {
						sb.append(" DOWN:").append(c.getHostname());
					}
				}
			}
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
		for(Connector c: connectors.values()) {
			res.put(c.toString(), c.isOK()?"OK":"DOWN");
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
				try(UserTSIConnection conn = getTSIConnection("nobody", null, h, -1)){
					tsiVersion = conn.getTSIVersion();
					if(tsiVersion!=null)break;
				}
				catch(Exception e){}
			}
		}
		return tsiVersion;
	}

	@Override
	public SocketChannel connectToService(String serviceAddress, String tsiHost, String user, String group)
			throws TSIUnavailableException, IOException{
		throw new TSIUnavailableException();
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

	public static List<String> getTSIHostNames(String preferredHost, Collection<Connector> connectors) {
		List<String>candidates = new ArrayList<>();
		String categoryPattern = null;
		String hostnamePattern = preferredHost;
		if(preferredHost!=null && preferredHost.contains(":")) {
			categoryPattern = preferredHost.split(":")[1];
		}
		for(Connector conn: connectors){
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
