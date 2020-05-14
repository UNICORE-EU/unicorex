package de.fzj.unicore.uas.jclouds;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import de.fzj.unicore.uas.UASProperties;
import eu.unicore.util.Log;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;

public class JCloudsProperties extends PropertiesHelper {

	private static final Logger log = Log.getLogger(Log.SERVICES, JCloudsProperties.class);

	@DocumentationReferencePrefix
	public static final String PREFIX = UASProperties.PREFIX + "jclouds.";


	public static final String PROVIDER = "provider";
	public static final String IDENTITY = "identity";
	public static final String CREDENTIAL = "credential";
	public static final String GROUPNAME = "groupName";
	public static final String IMAGE_ID = "imageID";
	public static final String FLAVOR_ID = "flavorID"; 
	public static final String ENDPOINT = "endpoint";
	public static final String API_VERSION = "apiVersion";
	public static final String TRUSTCERTS = "trustCerts";
	public static final String TSI_INSTALL_SCRIPT = "tsiInstallScript";
	public static final String XNJS_CONFIG_TEMPLATE = "xnjsConfigTemplate";
	
	public static final String SECURITY_KEY_NAME = "securityKeyName";
	public static final String PRIVATE_KEY = "privateKey";
	
	@DocumentationReferenceMeta
	public static final Map<String, PropertyMD> META = new HashMap<String, PropertyMD>();
	static {

	}

	public JCloudsProperties(Properties properties) throws ConfigurationException {
		super(PREFIX, properties, META, log);
	}
}
