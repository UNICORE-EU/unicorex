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

	public static final String TSI_EXECUTABLE = "executable";

	public static final String SETUP_COMMAND = "setupCommand";

	public static final String ID_RESOLVERS = "identityResolver";

	static
	{
		META.put(TSI_EXECUTABLE, new PropertyMD().setMandatory().
				setDescription("Command used to run the TSI."));
		META.put(SETUP_COMMAND, new PropertyMD().
				setDescription("Command used to setup the TSI before the first use."));
		META.put(ID_RESOLVERS, new PropertyMD().setCanHaveSubkeys().setMandatory().
				setDescription("Configure identity resolvers. See documentation for details."));	
		META.put("unittesting", new PropertyMD("false").setBoolean().
				setDescription("(TODO unit testing mode)"));
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
		return getValue(TSI_EXECUTABLE);
	}

	public String getSetupCommand(){
		return getValue(SETUP_COMMAND);
	}

	public boolean isTesting() {
		return getBooleanValue("unittesting");
	}
}