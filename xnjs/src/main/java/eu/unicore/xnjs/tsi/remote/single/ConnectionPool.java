package eu.unicore.xnjs.tsi.remote.single;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import eu.unicore.xnjs.tsi.remote.TSIProperties;

/**
 * Pooling of UserTSIConnection for users
 *
 * @author schuller
 */
public class ConnectionPool {

	final Map<String, List<UserTSIConnection>> pool = new HashMap<>();

	private final Collection<Connector> connectors = new HashSet<>();

	public ConnectionPool(TSIProperties properties) {}

	public void configure(Collection<Connector> connectors) {
		synchronized (pool) {
			this.connectors.clear();
			this.connectors.addAll(connectors);
		}
	}

	public UserTSIConnection get(String user, String preferredHost){
		List<String>candidates = getTSIHostNames(preferredHost);
		synchronized(pool){
			List<UserTSIConnection> connections = getOrCreateConnectionList(user);
			Iterator<UserTSIConnection> iter = connections.iterator();
			while(iter.hasNext()) {
				UserTSIConnection conn = iter.next();
				if(!conn.isAlive()) {
					conn.shutdown();
					iter.remove();
				}
				else if(candidates.contains(conn.getTSIHostName())) {
					iter.remove();
					return conn;
				}
			}
		}
		return null;
	}

	public boolean offer(UserTSIConnection connection){
		synchronized (pool) {
			List<UserTSIConnection> pooled = getOrCreateConnectionList(connection.getTSIHostName());
			pooled.add(connection);
			return true;
		}
	}

	private List<String> getTSIHostNames(String preferredHost) {
		List<String>candidates = new ArrayList<>();
		String categoryPattern = null;
		String hostnamePattern = preferredHost;
		if(preferredHost!=null && preferredHost.contains(":")) {
			categoryPattern = preferredHost.split(":")[1];
		}
		for(Connector conn: connectors){
		String name = conn.getHostname();
			if(categoryPattern!=null) {
				if(SSHTSIConnectionFactory.matches(categoryPattern, conn.getCategory())) {
					candidates.add(name);
				}
			}
			else if(SSHTSIConnectionFactory.matches(hostnamePattern, name)) {
				candidates.add(name);
			}
		}
		if(candidates.size()==0){
			throw new IllegalArgumentException("No TSI is configured at '"+preferredHost+"'");
		}
		if(candidates.size()>1)Collections.shuffle(candidates);
		return candidates;
	}

	private List<UserTSIConnection> getOrCreateConnectionList(String user){
		List<UserTSIConnection> connections = pool.get(user);
		if(connections==null) {
			connections = new ArrayList<>();
			pool.put(user, connections);
		}
		return connections;
	}

	public void shutdown() {
		for(List<UserTSIConnection> cs: pool.values()){
			for(UserTSIConnection c: cs) {
				try{c.shutdown();}catch(Exception ex){}
			}
		}
		pool.clear();
	}
}