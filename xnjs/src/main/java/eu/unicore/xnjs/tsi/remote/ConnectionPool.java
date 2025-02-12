package eu.unicore.xnjs.tsi.remote;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Pooling of TSIConnections for a number of TSI hosts
 *
 * @author schuller
 */
public class ConnectionPool {

	final Map<String, List<TSIConnection>> pool = new HashMap<>();

	private final Collection<TSIConnector> connectors = new HashSet<>();

	// how many connections to "keep alive" in the pool
	private final int keepConnections;

	public ConnectionPool(TSIProperties properties) {
		keepConnections = properties.getIntValue(TSIProperties.TSI_POOL_SIZE);
	}

	public void configure(Collection<TSIConnector> connectors) {
		synchronized (pool) {
			this.connectors.clear();
			this.connectors.addAll(connectors);
		}
	}

	public TSIConnection get(String preferredHost){
		List<String>candidates = getTSIHostNames(preferredHost);
		synchronized(pool){
			for(String host: candidates) {
				List<TSIConnection> connections = getOrCreateConnectionList(host);
				while(connections.size()>0) {
					TSIConnection conn = connections.remove(0);
					if(!conn.isAlive()) {
						conn.shutdown();
					}
					else{
						return conn;
					}
				}
			}
		}
		return null;
	}

	public boolean offer(TSIConnection connection){
		synchronized (pool) {
			List<TSIConnection> pooled = getOrCreateConnectionList(connection.getTSIHostName());
			if(pooled.size()<keepConnections && connection.getConnector().isOK()){
				pooled.add(connection);
				return true;
			}
			else{
				connection.shutdown();
				return false;
			}
		}
	}

	private List<String> getTSIHostNames(String preferredHost) {
		List<String>candidates = new ArrayList<>();
		String categoryPattern = null;
		String hostnamePattern = preferredHost;
		if(preferredHost!=null && preferredHost.contains(":")) {
			categoryPattern = preferredHost.split(":")[1];
		}
		for(TSIConnector conn: connectors){
		String name = conn.getHostname();
			if(categoryPattern!=null) {
				if(DefaultTSIConnectionFactory.matches(categoryPattern, conn.getCategory())) {
					candidates.add(name);
				}
			}
			else if(DefaultTSIConnectionFactory.matches(hostnamePattern, name)) {
				candidates.add(name);
			}
		}
		if(candidates.size()==0){
			throw new IllegalArgumentException("No TSI is configured at '"+preferredHost+"'");
		}
		if(candidates.size()>1)Collections.shuffle(candidates);
		return candidates;
	}

	private List<TSIConnection> getOrCreateConnectionList(String hostname){
		List<TSIConnection> connections = pool.get(hostname);
		if(connections==null) {
			connections = new ArrayList<>();
			pool.put(hostname, connections);
		}
		return connections;
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

	public void shutdown() {
		for(List<TSIConnection> cs: pool.values()){
			for(TSIConnection c: cs) {
				try{c.shutdown();}catch(Exception ex){}
			}
		}
		pool.clear();
	}
}
