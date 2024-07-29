package eu.unicore.xnjs.tsi.remote;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.unicore.util.configuration.ConfigurationException;

/**
 * parses TSI connector specification
 *
 * @author schuller 
 */
public class TSIConfigurator {

	private final TSIProperties tsiProperties;

	private final TSIConnectionFactory factory;

	private final Collection<String> tsiHostCategories = new HashSet<>();

	public TSIConfigurator(TSIProperties tsiProperties, TSIConnectionFactory factory) {
		this.tsiProperties = tsiProperties;
		this.factory = factory;
	}

	public void configure(Map<String,TSIConnector> connectors, Set<String>categories) {
		tsiHostCategories.clear();
		try {
			// standard machine
			String machine = tsiProperties.getTSIMachine();
			int port = tsiProperties.getTSIPort();
			List<TSIConnector> newConnectors =createConnectors(machine, port, null);
			// machines in categories
			updateTSIHostCategories(categories);
			for(String category: categories){
				machine = tsiProperties.getValue(TSIProperties.TSI_MACHINE+"."+category);
				newConnectors.addAll(createConnectors(machine, port, category));
			}
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
			}
		}catch(Exception ex) {
			throw new ConfigurationException("Error (re-)configuring remote TSI connector.",ex);
		}
	}

	private List<TSIConnector> createConnectors(String machine, int port, String category)
			throws Exception {
		List<TSIConnector> newConnectors = new ArrayList<>();
		// parse 'machine' to extract TSI addresses.
		// if the port is not included, use the port
		// given by the separate TSI_PORT property
		String[]tsiSpec = machine.split("[ ,]+");
		for(int i = 0; i < tsiSpec.length; i++) {
			String[] split=tsiSpec[i].split(":");
			String host=split[0];
			int p = split.length>1 ? Integer.parseInt(split[1]) : port;
			if(p==-1)throw new IllegalArgumentException("Missing port for TSI machine: "+host);
			TSIConnector tsiConnector = createTSIConnector(host, p, category);
			newConnectors.add(tsiConnector);
		}
		return newConnectors;
	}

	private TSIConnector createTSIConnector(String hostname, int port, String category) throws UnknownHostException {
		return new TSIConnector(factory, tsiProperties, InetAddress.getByName(hostname), port, hostname, category);
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
