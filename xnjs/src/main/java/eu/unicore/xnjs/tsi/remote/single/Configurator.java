package eu.unicore.xnjs.tsi.remote.single;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.xnjs.tsi.remote.TSIProperties;

/**
 * parses TSI connector specification
 *
 * @author schuller 
 */
public class Configurator {

	private final TSIProperties tsiProperties;

	private final PerUserTSIProperties perUserTSIProperties;

	private final PerUserTSIConnectionFactory factory;

	private final Collection<String> tsiHostCategories = new HashSet<>();

	private final IdentityStore identityStore;

	public Configurator(TSIProperties tsiProperties, PerUserTSIProperties perUserTSIProperties, 
			PerUserTSIConnectionFactory factory, IdentityStore identityStore) {
		this.tsiProperties = tsiProperties;
		this.perUserTSIProperties = perUserTSIProperties;
		this.factory = factory;
		this.identityStore = identityStore;
	}

	public void configure(Map<String, Connector> connectors, Set<String>categories) {
		tsiHostCategories.clear();
		try {
			// standard machine
			String machine = tsiProperties.getTSIMachine();
			int port = tsiProperties.getTSIPort();
			List<Connector> newConnectors = createConnectors(machine, port, null);
			// machines in categories
			updateTSIHostCategories(categories);
			for(String category: categories){
				machine = tsiProperties.getValue(TSIProperties.TSI_MACHINE+"."+category);
				newConnectors.addAll(createConnectors(machine, port, category));
			}
			Collection<String>names = new ArrayList<>();
			for(Connector tc: newConnectors) {
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
			for(Connector tc: newConnectors) {
				connectors.put(tc.getHostname(), tc);
			}
		}catch(Exception ex) {
			throw new ConfigurationException("Error (re-)configuring remote TSI connector.",ex);
		}
	}

	private List<Connector> createConnectors(String machine, int port, String category)
			throws Exception {
		List<Connector> newConnectors = new ArrayList<>();
		// parse 'machine' to extract TSI addresses.
		// if the port is not included, use the port
		// given by the separate TSI_PORT property
		String[]tsiSpec = machine.split("[ ,]+");
		for(int i = 0; i < tsiSpec.length; i++) {
			String[] split=tsiSpec[i].split(":");
			String host=split[0];
			int p = split.length>1 ? Integer.parseInt(split[1]) : port;
			if(p==-1)throw new IllegalArgumentException("Missing port for TSI machine: "+host);
			Connector tsiConnector = createTSIConnector(host, p, category);
			newConnectors.add(tsiConnector);
		}
		return newConnectors;
	}

	private Connector createTSIConnector(String hostname, int port, String category) throws UnknownHostException {
		return new Connector(hostname, category, perUserTSIProperties, factory, identityStore);
	}

	public void updateTSIHostCategories(Set<String>tsiHostCategories){
		String pfx = TSIProperties.PREFIX+TSIProperties.TSI_MACHINE+".";
		tsiHostCategories.clear();
		for(Object k: tsiProperties.getRawProperties().keySet()){
			String key=((String)k).trim();
			if (!key.startsWith(pfx) || key.equals(pfx))
				continue;
			tsiHostCategories.add(key.substring(pfx.length()));
		}
	}
}
