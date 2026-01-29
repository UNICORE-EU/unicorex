package eu.unicore.xnjs.tsi.remote.single;

import java.io.File;

public class Connector {

	private final String hostname;

	private final String category;
	
	private final PerUserTSIProperties properties;

	private final PerUserTSIConnectionFactory factory;

	public Connector(String hostname, String category, PerUserTSIProperties properties, PerUserTSIConnectionFactory factory) {
		this.hostname = hostname;
		this.category = category;
		this.properties = properties;
		this.factory = factory;
	}

	public String getHostname() {
		return hostname;
	}

	public String getCategory() {
		return category;
	}

	public UserTSIConnection createConnection(String user) throws Exception {
		// TODO
		return launchLocally(user);
	}

	public boolean isOK() {
		// TODO
		return true;
	}

	public void notOK(String message) {
		// TODO
	}

	private UserTSIConnection launchLocally(String user) throws Exception {
		ProcessBuilder pb = new ProcessBuilder();
		File tsiExec = new File(properties.getCommand());
		pb.command(tsiExec.getAbsolutePath());
		final Process p = pb.start();
		return new UserTSIConnection(p.getInputStream(),p.getOutputStream(), factory, this, user,
				()->p.destroyForcibly());
	}

}