package eu.unicore.xnjs.tsi.remote.single;

import java.io.File;

import org.apache.logging.log4j.Logger;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import eu.unicore.security.Client;
import eu.unicore.xnjs.tsi.remote.IConnector;
import eu.unicore.xnjs.util.LogUtil;

public class Connector implements IConnector {

	private static final Logger logger = LogUtil.getLogger(LogUtil.TSI, Connector.class);

	private final String hostname;

	private final String category;
	
	private final PerUserTSIProperties properties;

	private final PerUserTSIConnectionFactory factory;

	private final IdentityStore identityStore;

	public Connector(String hostname, String category, PerUserTSIProperties properties, 
			PerUserTSIConnectionFactory factory, IdentityStore identityStore) {
		this.hostname = hostname;
		this.category = category;
		this.properties = properties;
		this.factory = factory;
		this.identityStore = identityStore;
	}

	@Override
	public String getHostname() {
		return hostname;
	}

	@Override
	public String getCategory() {
		return category;
	}

	public PerUserTSIConnection createConnection(Client user) throws Exception {
		return new PerUserTSIConnection(factory, this, user);
	}

	public boolean isOK() {
		// TODO
		return true;
	}

	public void notOK(String message) {
		// TODO
	}

	public void activate(PerUserTSIConnection conn) throws Exception {
		if(identityStore==null || properties.isTesting()){
			runLocally(conn);
		}
		else {
			if(conn.getSession()==null || !conn.getSession().isConnected()) {
				logger.info("Creating new SSH session for <{}>", conn.getUserDescription());
				conn.setSession(createSession(conn.getClient()));
			}
			Session session = conn.getSession();
			ChannelExec channel = (ChannelExec) session.openChannel("exec");
			channel.setCommand(properties.getCommand());
			conn.setInput(channel.getInputStream());
			conn.setOutput(channel.getOutputStream());
			channel.connect();
			conn.setCloseCallback(()->channel.disconnect());
		}
	}

	private Session createSession(Client client) throws Exception {
		Session session = null;
		JSch.setLogger(new Log());
		JSch jsch = new JSch();
		identityStore.addIdentity(jsch, client);
		String user = client.getSelectedXloginName();
		session = jsch.getSession(user, hostname);
		session.setConfig("StrictHostKeyChecking", "no");
		session.setConfig("PreferredAuthentications", "publickey");
		session.connect();
		return session;
	}

	private void runLocally(PerUserTSIConnection conn) throws Exception {
		ProcessBuilder pb = new ProcessBuilder();
		File tsiExec = new File(properties.getCommand());
		pb.command(tsiExec.getAbsolutePath());
		final Process p = pb.start();
		conn.setInput(p.getInputStream());
		conn.setOutput(p.getOutputStream());
		conn.setCloseCallback(()->p.destroyForcibly());
	}
}