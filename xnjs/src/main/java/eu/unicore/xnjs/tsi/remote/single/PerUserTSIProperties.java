package eu.unicore.xnjs.tsi.remote.single;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.Logger;

import eu.unicore.util.Log;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;
import jakarta.inject.Singleton;

/**
 * config properties for running a TSI for each user request
 *
 * @author schuller
 */
@Singleton
public class PerUserTSIProperties extends PropertiesHelper {

	private static final Logger logger=Log.getLogger(Log.CONFIGURATION, PerUserTSIProperties.class);

	@DocumentationReferencePrefix
	public static final String PREFIX="PERUSERTSI.";

	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> META = new HashMap<>();

	public static final String TSI_LAUNCH = "executable";

	static
	{
		META.put(TSI_LAUNCH, new PropertyMD().
				setDescription("Command used to launch the TSI."));
	}

	public PerUserTSIProperties() throws ConfigurationException {
		this(new Properties());
	}

	public PerUserTSIProperties(Properties properties) throws ConfigurationException {
		super(PREFIX, properties, META, logger);
	}

	public Properties getRawProperties() {
		return this.properties;
	}

	public String getCommand(){
		return getValue(TSI_LAUNCH);
	}
	
}
