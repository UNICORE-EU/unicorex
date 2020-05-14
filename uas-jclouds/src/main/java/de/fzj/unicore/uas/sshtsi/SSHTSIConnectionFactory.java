package de.fzj.unicore.uas.sshtsi;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import de.fzj.unicore.xnjs.XNJS;
import de.fzj.unicore.xnjs.tsi.TSIUnavailableException;
import de.fzj.unicore.xnjs.tsi.remote.DefaultTSIConnectionFactory;
import de.fzj.unicore.xnjs.tsi.remote.TSIConnection;
import de.fzj.unicore.xnjs.tsi.remote.TSIConnector;
import de.fzj.unicore.xnjs.util.LogUtil;
import net.schmizz.keepalive.KeepAliveProvider;
import net.schmizz.sshj.DefaultConfig;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.LocalPortForwarder;
import net.schmizz.sshj.connection.channel.forwarded.RemotePortForwarder;
import net.schmizz.sshj.connection.channel.forwarded.RemotePortForwarder.Forward;
import net.schmizz.sshj.connection.channel.forwarded.SocketForwardingConnectListener;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;


/**
 * Connects to a TSI server through an SSH tunnel
 * 
 * @author Christian Boettcher
 */
public class SSHTSIConnectionFactory extends DefaultTSIConnectionFactory {

	private static final Logger log = LogUtil.getLogger(LogUtil.TSI, SSHTSIConnectionFactory.class);
	
	// default configuration for every ssh-tunnel
	private static DefaultConfig defaultconfig = new DefaultConfig();
	static {
		defaultconfig.setKeepAliveProvider(KeepAliveProvider.KEEP_ALIVE);
	}
	public static final int DEFAULT_TSI_PORT = 4433;
	
	protected int callback_port;
	// contains the SSHClients to the respective hosts in remoteMachines[]
	protected SSHClient[] clients;
	protected int[] localPorts;
	protected String[] remoteMachines;
	protected int sshPort;
	protected Thread[] forwardThreads;
	protected String keypath;
	protected String keypass;
	protected String username;
	protected SSHTSIProperties remoteTSIProperties;
	protected KeyProvider remoteRootKP;
	
	@Inject
	public SSHTSIConnectionFactory(XNJS config) {
		super(config);
	}
	
	/**
	 * checks if the keypair can be safely used and returns KeyProvider
	 */
	public KeyProvider checkKeypair() throws IOException {
		KeyProvider kp = null;
		if (keypass==null || keypass.isEmpty()) {
			kp = new SSHClient(defaultconfig).loadKeys(keypath);
		} else {
			kp = new SSHClient(defaultconfig).loadKeys(keypath, keypass);
		}
		return kp;
	}
	
	protected void startInit() {
		// There is going to be one SSH connection per hostname
		// no connection is initiated here
		try {
			remoteTSIProperties = configuration.get(SSHTSIProperties.class);
			String tsiRemoteMachines = remoteTSIProperties.getTSIMachine();
			remoteMachines = tsiRemoteMachines.split(",");
			sshPort = remoteTSIProperties.getSSHPort();
			localPorts = new int[remoteMachines.length];
			username = remoteTSIProperties.getUser();
			if(log.isDebugEnabled())log.debug("Building tunnels to '"+username+"@"+tsiRemoteMachines+"'");
			// parse "machine" for the tsi ports
			String[] host_port;
			int port;
			for (int i = 0; i < remoteMachines.length; i++) {
				host_port = remoteMachines[i].split(":");
				if (host_port.length > 1) {
					port = Integer.parseInt(host_port[1]);
				} else {
					/*
					 * As UNICORE checks if the TSI listens on the same port,
					 * that is listed in the xnjs_legacy.xml, the tunnel has to
					 * use the same ports on each machine
					 */
					port = remoteTSIProperties.getTSIPort();
				}
				localPorts[i] = port;
			}
			// parse remote hosts 
			for (int i = 0; i < remoteMachines.length; i++) {
				host_port = remoteMachines[i].split(":");
				remoteMachines[i] = host_port[0];
			}
			callback_port = remoteTSIProperties.getCallbackPort();
			clients = new SSHClient[remoteMachines.length];
			forwardThreads = new Thread[remoteMachines.length];
			keypass = remoteTSIProperties.getKeyPass();
			keypath = remoteTSIProperties.getKeyPath();
			
			//init ssh clients
			for (int i = 0; i < clients.length; i++) {
				clients[i] = new SSHClient(defaultconfig);
				clients[i].addHostKeyVerifier(new PromiscuousVerifier());
			}
			
			try {
				checkKeypair();
			} catch (IOException e) {
				log.error("Can not open key at \"" + keypath + "\"");
				throw e;
			}
		} catch (Exception ex) {
			log.error("Config is messed up, cannot setup Remote TSI Connection factory.",ex);
			throw new RuntimeException("Config is messed up, cannot setup Remote TSI Connection factory.", ex);
		}
	}
	@Override
	public void start() {
		startInit();
		// we have to init the remote connection before doing super.start()}, 
		// because we have to build the first tunnel during
		// startup to check the connection
		super.start();
		if (remoteMachines.length != getTSIHosts().length) {
			log.error("Number of remote machines is not equal to the configured number of tsiMachines.");
			throw new Error("Number of remote machines is not equal to the configured number of tsiMachines.");
		}
	}
	
	/**
	 * ensures the SSH-tunnel to the given host is open and returns a
	 * TSIConnection
	 * 
	 * @param hostname
	 * @return TSIConnection
	 */
	@Override
	protected TSIConnection getFromPool(String hostname, int timeout) {
		try{
			if (hostname == null) {
				buildAllTunnels();
			} else {
				buildTunnel(hostname);
			}
		}catch(IOException ex){
			LogUtil.logException("Cannot build SSH tunnel", ex);
			throw new RuntimeException(ex);
		}
		return super.getFromPool(hostname, timeout);
	}
	
	/**
	 * if a tunnel to the given hostname does not exist it is established
	 */
	protected void buildTunnel(String hostname) throws IOException {
		int pos = 0;
		for (int i = 0; i < remoteMachines.length; i++) {
			if (remoteMachines[i].equals(hostname)) {
				pos = i;
				break;
			}
		}
		buildTunnel(pos);
	}
	
	/**
	 * checks if tunnel to the host at the specified position in the
	 * remoteMachines array exists and creates it if it doesn't
	 * 
	 * @param pos
	 * @throws IOException
	 */
	protected synchronized void buildTunnel(int pos) throws IOException {
		SSHClient ssh = clients[pos];
		// if the ssh client is still connected and authenticated, we can assume
		// that the tunnels are still working (can we?TODO)
		if (ssh.isConnected()) {
			if (ssh.isAuthenticated()) {
				return;
			} else { // if not authenticated, authenticate
				ssh.authPublickey(username, checkKeypair());
			}
		} else { // if not connected, connect and authenticate
			ssh.connect(remoteMachines[pos], sshPort);
			ssh.authPublickey(username, checkKeypair());
		}
		LocalPortForwarder lpf;
		RemotePortForwarder rpf;
		int port = localPorts[pos];
		// forward TSI callback-Requests on the remote server to the local
		// callback-port
		rpf = ssh.getRemotePortForwarder();
		rpf.bind(new Forward(callback_port),
				new SocketForwardingConnectListener(new InetSocketAddress("localhost", callback_port)));
		
		final ServerSocket ss = new ServerSocket();
		ss.setReuseAddress(true);
		ss.bind(new InetSocketAddress("0.0.0.0", port));
		// forward local requests to the tsi to the remote server
		final LocalPortForwarder.Parameters params = new LocalPortForwarder.Parameters("0.0.0.0", port, "0.0.0.0",
				port);
		
		lpf = ssh.newLocalPortForwarder(params, ss);
		startlocalForwardThread(lpf, pos);
		clients[pos] = ssh;
		
	}
	
	/**
	 * starts listening on the TSI-port and forwards to the remote TSI machine
	 * if there is a thread listening already, this method does nothing and
	 * returns
	 * 
	 * @param lpf
	 */
	protected void startlocalForwardThread(final LocalPortForwarder lpf, int pos) {
		Thread old = forwardThreads[pos];
		// if old thread is still alive, do nothing
		if (old != null && old.isAlive()) {
			return;
		}
		Runnable r = new Runnable() {
			@Override
			public void run() {
				try {
					lpf.listen();
				} catch (IOException e) {
					log.error("Problem with forwarding local port");
					throw new Error("Problem with forwarding local port", e);
				}
			}
		};
		Thread t = new Thread(r);
		t.start();
		
		forwardThreads[pos] = t;
	}
	
	/**
	 * In the case that no preferred host is given, all tunnels have to be built
	 * to prevent failed connection attempts
	 */
	protected void buildAllTunnels() throws IOException {
		for (int i = 0; i < remoteMachines.length; i++) {
			buildTunnel(i);
		}
	}
	
	protected TSIConnector createTSIConnector(String hostname, int port) throws UnknownHostException {
		return new TSIConnector(this, tsiProperties, InetAddress.getByName("localhost"), port, hostname);
	}
	
	@Override
	protected synchronized TSIConnection createNewTSIConnection(String preferredHost) throws TSIUnavailableException {
		try{
			if (preferredHost == null) {
				buildAllTunnels();
			} else {
				buildTunnel(preferredHost);
			}
		}catch(IOException ex){
			throw new TSIUnavailableException(LogUtil.createFaultMessage("Cannot build SSH tunnel", ex));
		}
		return super.createNewTSIConnection(preferredHost);
	}

	
	public String getConnectionStatus(){
		return super.getConnectionStatus()+"[SSH-tunneled]";
	}
}
