package de.fzj.unicore.uas.sshtsi;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.log4j.Logger;
import de.fzj.unicore.xnjs.tsi.remote.TSIProperties;
import eu.unicore.util.Log;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;

public class SSHTSIProperties extends PropertiesHelper {

	private static final Logger logger = Log.getLogger(Log.SERVICES, SSHTSIProperties.class);
	
	@DocumentationReferencePrefix
	public static final String PREFIX = "REMOTETSI.";
	
	public static final String REMOTE_TSI_KEY_PATH = "keypath";
	
	public static final String REMOTE_TSI_KEY_PASS = "keypass";
	
	public static final String REMOTE_TSI_SSH_PORT= "sshport";
	
	public static final String REMOTE_TSI_USERNAME = "user";
	
	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> META = new HashMap<String, PropertyMD>();
	
	static {
		// additional ssh connection settings
		META.put(REMOTE_TSI_KEY_PATH, new PropertyMD("${USERHOME}/.ssh/id_rsa").setPath()
				.setDescription("The path for the ssh-key to be used for building the ssh tunnel."));
		META.put(REMOTE_TSI_KEY_PASS, new PropertyMD().setDescription("The passphrase for the ssh-key."));
		META.put(REMOTE_TSI_USERNAME, new PropertyMD("root").setDescription("The passphrase for the ssh-key."));
		META.put(REMOTE_TSI_SSH_PORT, new PropertyMD("22").setInt()
				.setDescription("The port of the remote SSH server."));
	}
	
	// usual TSI properties
	private TSIProperties tsiProperties;
	
	public SSHTSIProperties() throws ConfigurationException {
		this(new Properties());
	}
	
	public SSHTSIProperties(Properties properties) throws ConfigurationException {
		super(PREFIX, properties, META, logger);
		this.tsiProperties = new TSIProperties(properties);
	}
	
	/**
	 * Returns the path to the ssh key to be used for connecting to the remote
	 * machines.
	 */
	public String getKeyPath() {
		return getValue(REMOTE_TSI_KEY_PATH).replace("${USERHOME}", System.getProperty("user.home"));
	}
	
	/**
	 * Returns the password of the ssh key to be used for connecting to the
	 * remote machines.
	 */
	public String getKeyPass() {
		return getValue(REMOTE_TSI_KEY_PASS);
	}
	
	/**
	 * Returns the password of the ssh key to be used for connecting to the
	 * remote machines.
	 */
	public String getUser() {
		return getValue(REMOTE_TSI_USERNAME);
	}
	
	
	/**
	 * Returns a comma-seperated list of "host:port" pairs, indicating where the
	 * SSH Server of the remote machines is listening.
	 */
	public int getSSHPort() {
		return getIntValue(REMOTE_TSI_SSH_PORT);
	}
	
	
	/**
	 * Returns the callback port of the XNJS
	 */
	public int getCallbackPort() {
		return tsiProperties.getTSIMyPort();
	}
	
	/**
	 * Returns the TSI machines
	 */
	public String getTSIMachine() {
		return tsiProperties.getTSIMachine();
	}

	public int getTSIPort() {
		return tsiProperties.getIntValue(TSIProperties.TSI_PORT);
	}
}