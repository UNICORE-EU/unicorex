package eu.unicore.xnjs.tsi.remote.single;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;

import eu.unicore.security.Client;
import eu.unicore.security.SecurityException;
import eu.unicore.services.utils.CircuitBreaker;
import eu.unicore.xnjs.tsi.remote.IConnector;
import eu.unicore.xnjs.util.LogUtil;
import net.schmizz.sshj.DefaultConfig;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import net.schmizz.sshj.userauth.method.AuthPublickey;

public class Connector implements IConnector {

	private static final Logger logger = LogUtil.getLogger(LogUtil.TSI, Connector.class);

	private final String hostname;

	private final int port;

	private final String category;
	
	private final PerUserTSIProperties properties;

	private final PerUserTSIConnectionFactory factory;

	private final IdentityStore identityStore;

	private final Map<String, AtomicInteger> usageCounters = new HashMap<>();

	public Connector(String hostname, int port, String category, PerUserTSIProperties properties, 
			PerUserTSIConnectionFactory factory, IdentityStore identityStore) {
		this.hostname = hostname;
		this.port = port;
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

	public PerUserTSIConnection createConnection(Client client) throws Exception {
		PerUserTSIConnection conn = new PerUserTSIConnection(createSSHClient(client), factory, this, client);
		String userName = client.getSelectedXloginName();
		if(userName==null)throw new SecurityException("Required Unix username is null");
		if(!isOK(userName)){
			throw new IOException(getStatusMessage(userName));
		}
		AtomicInteger i = usageCounters.get(userName);
		if(i==null) {
			i = new AtomicInteger();
			usageCounters.put(userName, i);
		}
		String setupCommand = properties.getSetupCommand();
		if(i.get()==0 && setupCommand!=null)
		{
			SSHClient ssh = conn.getSSH();
			try (Session session = ssh.startSession()) {
				logger.debug("Runnung setup command --> {}", setupCommand);
				Command cmd = session.exec(setupCommand);
				String output = IOUtils.toString(cmd.getInputStream(), "UTF-8");
				String error = IOUtils.toString(cmd.getErrorStream(), "UTF-8");
				cmd.join(10, TimeUnit.SECONDS);
				logger.debug("Setup command returned with exit status <{}> and output: '{} {}'", cmd.getExitStatus(), output, error);
				if(cmd.getExitStatus()!=0) {
					throw new IOException("TSI setup command failed: "+output+ " "+error);
				}
				int num = i.incrementAndGet();
				logger.info("Created new TSIConnection to <{}> for user <{}>, this is <{}>",
						hostname, userName, num);
			}
		}
		return conn;
	}

	private final Map<String, CircuitBreaker> cbs = new HashMap<>();
	private final Map<String, String> msg = new HashMap<>();

	private CircuitBreaker getCB(String user) {
		synchronized (cbs) {
			CircuitBreaker cb = cbs.get(user);
			if(cb==null) {
				cb = new CircuitBreaker();
				cbs.put(user, cb);
			}
			return cb;
		}
	}

	public boolean isOK() {
		return true;
	}

	public boolean isOK(String userName) {
		return getCB(userName).isOK();
	}

	public void notOK(String userName, String message) {
		getCB(userName).notOK();
		msg.put(userName, message);
	}

	public String getStatusMessage(String userName){
		return msg.getOrDefault(userName, "N/A");
	}

	@Override
	public String toString() {
		return String.format("Connector %s:%s%s", hostname, port, (category!=null? "["+category+"]":"") );
	}

	public void activate(PerUserTSIConnection conn) throws Exception {
		if(identityStore==null || properties.isTesting()){
			runLocally(conn);
		}
		else {
			SSHClient ssh = conn.getSSH();
			Session session = ssh.startSession();
			Command cmd = session.exec(properties.getCommand());
			conn.setInput(cmd.getInputStream());
			conn.setOutput(cmd.getOutputStream());
			conn.setError(cmd.getErrorStream());
			conn.setCloseCallback(()->deactivate(conn, cmd, session));
		}
	}

	private void deactivate(PerUserTSIConnection conn, Command cmd, Session session) {
		IOUtils.closeQuietly(cmd, session);
	}

	private SSHClient createSSHClient(Client client) throws Exception {
		if(factory.isTesting()) {
			return null;
		}
		DefaultConfig c = new DefaultConfig();
		c.setVerifyHostKeyCertificates(false);
		SSHClient ssh = new SSHClient(c);
		ssh.addHostKeyVerifier(new PromiscuousVerifier());
		ssh.connect(hostname, port);
		KeyProvider kProv = identityStore.getIdentity(client);
		String user = client.getSelectedXloginName();
		ssh.auth(user, new AuthPublickey(kProv));
		return ssh;
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