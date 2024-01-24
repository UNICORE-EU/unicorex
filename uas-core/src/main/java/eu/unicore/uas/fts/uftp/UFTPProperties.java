package eu.unicore.uas.fts.uftp;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.Logger;

import eu.unicore.uas.UASProperties;
import eu.unicore.uas.util.LogUtil;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;

public class UFTPProperties extends PropertiesHelper {
	
	private static final Logger log = LogUtil.getLogger(LogUtil.DATA, UFTPProperties.class);
	
	@DocumentationReferencePrefix
	public static final String PREFIX = UASProperties.PREFIX + "uftp.";

	public static final String PARAM_ENABLE_UFTP = "enable";

	
	/**
	 * extra property for configuring the UFTP client side:
	 * <ul>
	 * <li>if set to "true", the UFTP client will be run on UNICORE/X, transferring data
	 * from/to the TSI</li>
	 * <li>if set to <code>false</code>, the UFTP client code will be run on the TSI</li>
	 * </ul>
	 */
	public static final String PARAM_CLIENT_LOCAL="client.local";

	/**
	 * property for configuring the path to the client executable (location of 'uftp.sh')
	 * TSI 8.3 and later has builtin UFTP support, and this should not be set
	 */
	public static final String PARAM_CLIENT_EXECUTABLE="client.executable";

	/**
	 * property for disabling ssl on the command port (useful for testing)
	 */
	public static final String PARAM_COMMAND_SSL_DISABLE="command.sslDisable";

	/**
	 * property for setting the command host
	 */
	public static final String PARAM_COMMAND_HOST="command.host";

	/**
	 * property for setting the command port
	 */
	public static final String PARAM_COMMAND_PORT="command.port";
	
	/**
	 * property for setting the command port socket timeout
	 */
	public static final String PARAM_COMMAND_TIMEOUT="command.socketTimeout";
	
	/**
	 * property for setting the file read/write buffer size in kbytes
	 */
	public static final String PARAM_BUFFERSIZE="buffersize";
	
	/**
	 * parameter for configuring server limit for number of streams 
	 */
	public static final String PARAM_STREAMS_LIMIT="streamsLimit";
	
	/** 
	 * client host, i.e. where the uftp client code runs
	 */
	public static final String PARAM_CLIENT_HOST="client.host";

	/** 
	 * client IP address(es) to send to UFTPD
	 */
	public static final String PARAM_CLIENT_IP="client.ip_addresses";

	/**
	 * requested number of parallel data streams
	 */
	public static final String PARAM_STREAMS="streams";

	/**
	 * server host. If not set, UFTP is disabled!
	 */
	public static final String PARAM_SERVER_HOST="server.host";

	/**
	 * server port 
	 */
	public static final String PARAM_SERVER_PORT="server.port";
	
	/**
	 * disable session mode (mostly intended for testing)
	 */
	public static final String PARAM_DISABLE_SESSION_MODE="disableSessionMode";
	
	/**
	 * enable encryption by setting this to "true"
	 */
	public static final String PARAM_ENABLE_ENCRYPTION="encryption";

	/**
	 * rate limit in bytes per second, set to "0" for no limit
	 */
	public static final String PARAM_RATE_LIMIT="rateLimit";

	@DocumentationReferenceMeta
	public static final Map<String, PropertyMD> META = new HashMap<String, PropertyMD>();
	static {
		
		META.put(PARAM_ENABLE_UFTP, new PropertyMD("true").setBoolean().
			setDescription("Controls whether UFTP should be enabled for this server."));
	
		META.put(PARAM_CLIENT_LOCAL, new PropertyMD("false").
				setDescription("Controls whether, the Java UFTP client code should be run directly within the JVM, which will work only if the UNICORE/X has access to the target file system, or, if set to false, in the TSI."));
		META.put(PARAM_CLIENT_EXECUTABLE, new PropertyMD().setDeprecated().
				setDescription("Configures the path to the client executable (location of 'uftp.sh') on the TSI."));
		META.put(PARAM_COMMAND_SSL_DISABLE, new PropertyMD("false").
				setDescription("Allows to disable SSL on the command port (useful for testing)."));
		META.put(PARAM_COMMAND_HOST, new PropertyMD("localhost").
				setDescription("The UFTPD command host."));
		META.put(PARAM_COMMAND_PORT, new PropertyMD("64435").setBounds(1, 65535).
				setDescription("The UFTPD command port."));
		META.put(PARAM_COMMAND_TIMEOUT, new PropertyMD("10").setBounds(0, 300).
				setDescription("The timeout (in seconds) for communicating with the command port."));
		META.put(PARAM_BUFFERSIZE, new PropertyMD("128").setPositive().
				setDescription("File read/write buffer size in kbytes."));
		META.put(PARAM_STREAMS_LIMIT, new PropertyMD("4").setPositive().
				setDescription("Server limit for number of streams (per client connection)."));
		META.put(PARAM_RATE_LIMIT, new PropertyMD("0").setInt().
				setDescription("Limit the bandwidth (bytes per second) used by a single transfer (0 means no limit)."));
		META.put(PARAM_CLIENT_HOST, new PropertyMD((String)null).
				setDescription("Client host. If not set and UFTP client is set to local, then the local interface address will be determined at runtime. If not set and non-local mode is configured, then the TSI machine will be used."));
		META.put(PARAM_CLIENT_IP, new PropertyMD((String)null).
				setDescription("Client IP address(es) to send to UFTPD. If not set, the client.host value will be used."));
		META.put(PARAM_STREAMS, new PropertyMD("1").
				setDescription("Requested number of parallel data streams."));
		META.put(PARAM_SERVER_HOST, new PropertyMD().
				setDescription("UFTPD listen host. If this is not set, UFTP is disabled."));
		META.put(PARAM_SERVER_PORT, new PropertyMD("64434").setBounds(1, 65535).
				setDescription("UFTPD listen port."));
		META.put(PARAM_ENABLE_ENCRYPTION, new PropertyMD("false").
				setDescription("Controls whether encryption should be enabled by default for server-server transfers."));
		META.put(PARAM_DISABLE_SESSION_MODE, new PropertyMD("false").
				setDescription("Controls multi-file transfers should be done one by one (NOT recommended)."));
	}
	
	public UFTPProperties(String prefix, Properties properties) throws ConfigurationException {
		super(prefix, properties, META, log);
	}
	
	public UFTPProperties(Properties properties) throws ConfigurationException {
		this(PREFIX, properties);
	}
	
}
