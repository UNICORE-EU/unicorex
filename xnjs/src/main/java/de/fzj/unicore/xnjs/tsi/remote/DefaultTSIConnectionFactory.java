/*********************************************************************************
 * Copyright (c) 2006 Forschungszentrum Juelich GmbH 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * (1) Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the disclaimer at the end. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * 
 * (2) Neither the name of Forschungszentrum Juelich GmbH nor the names of its 
 * contributors may be used to endorse or promote products derived from this 
 * software without specific prior written permission.
 * 
 * DISCLAIMER
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *********************************************************************************/

package de.fzj.unicore.xnjs.tsi.remote;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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

/**
 * Creates and pools connections to a UNICORE TSI server. Multiple TSI nodes are supported.
 * 
 * @author schuller
 */
@Singleton
public class DefaultTSIConnectionFactory implements TSIConnectionFactory {

	private static final Logger log=LogUtil.getLogger(LogUtil.TSI,DefaultTSIConnectionFactory.class);

	protected TSIProperties tsiProperties;

	private final Map<String, List<TSIConnection>> pool = new HashMap<>();
	
	// count pool content by TSI host name
	private final Map<String,AtomicInteger> connectionCounter = new HashMap<>(); 

	private TSIConnector[]connectors;
	private String[] tsiHostnames;
	//current "position" in the TSI connector pool
	private int pos=0;
	private final Map<String,TSIConnector> connectorsByName = new HashMap<>();

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
		for(int i=0;i<connectors.length;i++){
			TSIConnector c = connectors[pos];
			pos++;
			if(pos>=connectors.length)pos=0;

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
		if(connectors.length>1){
			msg = Log.createFaultMessage("None of the configured TSIs is available.",lastException);
		}
		else{
			msg = Log.createFaultMessage("TSI unavailable", lastException);
		}
		throw new TSIUnavailableException(msg);
	}

	private List<String> getTSIHostNames(String preferredHost) {
		List<String>candidates = new ArrayList<>();
		for(int i=0;i<connectors.length;i++){
			if(matches(preferredHost, connectors[i].getHostname())){
				candidates.add(connectors[i].getHostname());
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
			connector = connectorsByName.get(name);
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
			TSIConnector c = connectorsByName.get(name);
			try{
				return c.createNewTSIConnection(server);
			}catch(Exception ex){
				log.debug("{} is not available: {}", c, ex.getMessage());
				lastException=ex;
			}
		}
		//no luck, all TSIs are unavailable
		String msg;
		if(connectors.length>1){
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
			assert(tsiProperties!=null);
			
			machine=tsiProperties.getTSIMachine();
			int port = tsiProperties.getTSIPort();
			int replyport=tsiProperties.getTSIMyPort();
			server=new TSISocketFactory(configuration, replyport);

			String[]tsiSpec=machine.split("[ ,]+");

			connectors = new TSIConnector[tsiSpec.length];
			tsiHostnames = new String[tsiSpec.length];

			// parse machine to extract TSI addresses
			// if machine does not include the port, use the 
			// port given by the separate TSI_PORT property
			StringBuilder machineSpec=new StringBuilder();
			for(int i = 0; i < tsiSpec.length; ++i) {
				String[] split=tsiSpec[i].split(":");
				String host=split[0];
				int p = split.length>1 ? Integer.parseInt(split[1]) : port;
				if(p==-1)throw new IllegalArgumentException("Missing port for TSI machine: "+host);
				connectors[i] = createTSIConnector(host, p);
				tsiHostnames[i] = host;
				connectorsByName.put(host, connectors[i]);
				if(i>0)machineSpec.append(", ");
				machineSpec.append(host).append(":").append(p);
				connectionCounter.put(host, new AtomicInteger(0));
			}

			boolean ssl=server.useSSL();
			log.info("TSI connection factory starting:\n" +
					"  ** Talking to TSI at "+machineSpec+"\n"+
					"  ** SSL is "+(ssl?"enabled":"disabled")+"\n"+
					"  ** Listening on port "+replyport+"\n"+
					"  ** User id for querying list of jobs on BSS: '"+tsiProperties.getBSSUser()+"'");

			machine=machineSpec.toString(); // to also include port
			tsiDescription=machine+", XNJS listens on port "+replyport;
			keepConnections = tsiProperties.getIntValue(TSIProperties.TSI_POOL_SIZE);
			log.info("TSI connection: {}", getConnectionStatus());
			isRunning = true;
		}
		catch(Exception ex){
			throw new RuntimeException("Cannot setup TSI Connection Factory" ,ex);
		}
		try {
			 configuration.get(IExecution.class);
		}catch(Exception ex) {}
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
		int numTotal = connectors.length;
		
		for(String h: tsiHostnames){
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
	public String[] getTSIHosts(){
		return tsiHostnames;
	}

	@Override
	public String getTSIMachine(){
		return machine;
	}

	@Override
	public Map<String,String>getTSIConnectorStates(){
		Map<String,String> res = new HashMap<>();
		for(TSIConnector c: connectors) {
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
			for(String h: tsiHostnames){
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
			for(TSIConnector connector: connectors) {
				return connector.connectToService(server, serviceHost, servicePort, user, group);
			}
			// unreachable
			throw new TSIUnavailableException("No TSI connectors");
		}
	}

}
