package eu.unicore.xnjs.tsi.remote.single;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.Logger;

import eu.unicore.security.Client;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.PropertyChangeListener;
import eu.unicore.util.configuration.PropertyGroupHelper;
import eu.unicore.xnjs.XNJS;
import eu.unicore.xnjs.tsi.TSIUnavailableException;
import eu.unicore.xnjs.tsi.remote.TSIConnection;
import eu.unicore.xnjs.tsi.remote.TSIConnectionFactory;
import eu.unicore.xnjs.tsi.remote.TSIMessages;
import eu.unicore.xnjs.tsi.remote.TSIProperties;
import eu.unicore.xnjs.tsi.remote.server.DefaultTSIConnectionFactory;
import eu.unicore.xnjs.tsi.remote.server.DefaultTSIConnectionFactory.RollingIndex;
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

	private final IdentityStore identityStore;

	@Inject
	public PerUserTSIConnectionFactory(XNJS xnjs){
		this.tsiProperties = xnjs.get(TSIProperties.class);
		this.perUserTsiProperties = xnjs.get(PerUserTSIProperties.class);
		this.connectionPool = new ConnectionPool(tsiProperties);
		this.identityStore = xnjs.get(IdentityStore.class, true);
		start(xnjs);
	}

	protected PerUserTSIConnection getTSIConnection(String user, String group, String preferredHost, int timeout)
			throws TSIUnavailableException{
		if(!isRunning)throw new TSIUnavailableException();
		if(user==null)throw new IllegalArgumentException("Required UNIX user ID is null (security setup problem?)");
		return connectionPool.get(user, preferredHost);
	}

	@Override
	public PerUserTSIConnection getTSIConnection(Client client, String preferredHost, int timeout)
			throws TSIUnavailableException{
		if(!isRunning)throw new TSIUnavailableException();
		String user = client.getXlogin().getUserName();
		if(user==null)throw new IllegalArgumentException("Required UNIX user ID is null (security setup problem?)");
		String group = TSIMessages.prepareGroupsString(client);
		PerUserTSIConnection conn = getTSIConnection(user, group, preferredHost, timeout);
		if(conn==null){
			conn = createNewTSIConnection(client, preferredHost);
		}
		if(conn!=null)conn.activate();
		return conn;
	}

	protected synchronized PerUserTSIConnection createNewTSIConnection(Client user, String preferredHost) throws TSIUnavailableException {
		int limit = tsiProperties.getIntValue(TSIProperties.TSI_WORKER_LIMIT);
		if(limit>0 && liveConnections.get()>=limit){
			throw new TSIUnavailableException(preferredHost);
		}
		log.info("Creating new TSIConnection for <{}> to <{}>", user, preferredHost);
		PerUserTSIConnection connection = preferredHost==null ?
				doCreate(user) : doCreate(user, preferredHost);
		liveConnections.incrementAndGet();
		return connection;
	}

	// index of last used TSI connector
	private RollingIndex pos = null;

	private PerUserTSIConnection doCreate(Client user) throws TSIUnavailableException {
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

	private PerUserTSIConnection doCreate(Client user, String preferredHost) throws TSIUnavailableException {
		List<String>candidates = DefaultTSIConnectionFactory.getTSIHostNames(preferredHost, connectors.values());
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
			connectionPool.offer((PerUserTSIConnection)connection);
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
	public synchronized void start(XNJS xnjs) {
		if(isRunning)return;
		try {
			configure();
			setupIdentityResolvers(xnjs);
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

	private void configure() throws ConfigurationException {
		try {
			new Configurator(tsiProperties, perUserTsiProperties, this, identityStore).
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

	private void setupIdentityResolvers(XNJS xnjs) throws ConfigurationException {
		try {
			Properties props = perUserTsiProperties.getRawProperties();
			String basePrefix = PerUserTSIProperties.PREFIX+PerUserTSIProperties.ID_RESOLVERS+".";
			int num = 1;
			while(true) {
				String prefix = basePrefix+num+".";
				if(props.getProperty(prefix+"class")==null) {
					break;
				}
				identityStore.registerResolver(createResolver(prefix, props, xnjs));
				num++;
			}
		} catch(Exception ex) {
			throw new ConfigurationException("Error setting up IdentityStore", ex);
		}
	}

	private IdentityResolver createResolver(String prefix, Properties props, XNJS xnjs) throws Exception {
		String clazz = props.getProperty(prefix+"class");
		IdentityResolver r = (IdentityResolver)Class.forName(clazz).getConstructor().newInstance();
		Map<String,String>params = new PropertyGroupHelper(props, new String[]{prefix}).getFilteredMap();
			params.remove(prefix+"class");
		mapParams(r,params);
		Method xnjsSetter = findSetter(r.getClass(), "xnjs");
		if (xnjsSetter != null && xnjsSetter.getParameterTypes()[0].isAssignableFrom(XNJS.class))
		{
			try {
				xnjsSetter.invoke(r, new Object[]{xnjs});
			}catch(Exception ex) {
				log.warn(ex);
			}
		}
		return r;
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
				try(PerUserTSIConnection conn = getTSIConnection("nobody", null, h, -1)){
					tsiVersion = conn.getTSIVersion();
					if(tsiVersion!=null)break;
				}
				catch(Exception e){}
			}
		}
		return tsiVersion;
	}

	@Override
	public SocketChannel connectToService(String serviceAddress, String tsiHost, Client client)
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

	boolean isTesting() {
		return perUserTsiProperties.isTesting();
	}

	private Method findSetter(Class<?> clazz, String paramName){
		for(Method m: clazz.getMethods()){
			if(m.getName().equalsIgnoreCase("set"+paramName) &&
					m.getParameterTypes().length > 0)return m;
		}
		return null;
	}

	/**
	 * Set properties on the given object using the parameter map.
	 * The method attempts to find matching setters for the parameter names 
	 * 
	 * @param obj
	 * @param params
	 * @param logger - can be null, if non-null, errors and warnings will be logged
	 */
	private void mapParams(Object obj, Map<String,String>params){
		Class<?> clazz = obj.getClass();
		for(Map.Entry<String,String> en: params.entrySet()){
			String s = en.getKey();
			String paramName = s.substring(s.lastIndexOf(".")+1);
			Method m = findSetter(clazz, paramName);
			if(m==null){
				log.warn("Can't map parameter <"+s+">");
				continue;
			}
			try{
				setParam(obj,m,en.getValue());
			}
			catch(Exception ex){
				log.warn("Can't set value <"+en.getValue()+"> for parameter <"+s+">");
			}
		}
	}

	private void setParam(Object obj, Method m, String valueString)throws Exception{
		Object arg=valueString;
		if(m.getParameterTypes()[0].isAssignableFrom(int.class)){
			arg=Integer.parseInt(valueString);
		}
		else if(m.getParameterTypes()[0].isAssignableFrom(Integer.class)){
			arg=Integer.parseInt(valueString);
		}
		else if(m.getParameterTypes()[0].isAssignableFrom(long.class)){
			arg=Long.parseLong(valueString);
		}
		else if(m.getParameterTypes()[0].isAssignableFrom(Long.class)){
			arg=Long.parseLong(valueString);
		}
		else if(m.getParameterTypes()[0].isAssignableFrom(boolean.class)){
			arg=Boolean.valueOf(valueString);
		}
		else if(m.getParameterTypes()[0].isAssignableFrom(Boolean.class)){
			arg=Boolean.valueOf(valueString);
		}
		m.invoke(obj, new Object[]{arg});
	}
}
