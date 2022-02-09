package de.fzj.unicore.xnjs;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.inject.Singleton;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.xnjs.tsi.TSI;
import de.fzj.unicore.xnjs.tsi.local.LocalTS;
import eu.unicore.util.Log;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;

/**
 * config properties for basic XNJS configuration
 *
 * @author schuller
 */
@Singleton
public class XNJSProperties extends PropertiesHelper {

	private static final Logger logger=Log.getLogger(Log.SERVICES, XNJSProperties.class);

	@DocumentationReferencePrefix
	public static final String PREFIX="XNJS.";

	public static final String AUTOSUBMIT_WHEN_READY="autosubmit";
	public static final String SERVICES="services";
	public static final String FILESPACE="filespace";
	public static final String FILESPACE_UMASK="filespaceUmask";
	
	public static final String STATEDIR="statedir";
	public static final String IDBFILE="idbfile";
	public static final String IDBTYPE = "idbtype";
	public static final String TSICLASS="tsiclass";

	public static final String XNJSWORKERS="numberofworkers";

	public static final String RESUBMIT_COUNT = "bssResubmitCount";
	public static final String RESUBMIT_DELAY = "bssResubmitDelay";

	public static final String INCARNATION_TWEAKER_CONFIG = "incarnationTweakerConfig";

	public static final String ALLOW_USER_EXECUTABLE = "allowUserExecutable";
	
	public static final String STRICT_USERINPUT_CHECKING = "strictUserInputChecking";

	public static final String STAGING_FS_GRACE = "staging.filesystemGraceTime";
	public static final String STAGING_FS_WAIT = "staging.addWaitingLoop";

	public static final String SWEEP_LIMIT = "parameterSweepLimit";

	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> META = new HashMap<String, PropertyMD>();
	static
	{
		META.put(RESUBMIT_COUNT, new PropertyMD("3").setInt().setPositive().
				setDescription("How often should UNICORE/X try to submit a job to the BSS."));
		META.put(RESUBMIT_DELAY, new PropertyMD("10").setInt().setPositive().
				setDescription("Minimum delay (in seconds) between attempts to submit a job to the BSS."));
		META.put(AUTOSUBMIT_WHEN_READY, new PropertyMD("false").setBoolean().
				setDescription("Automatically submit a job to the BSS without waiting for an explicit client start."));
		META.put(FILESPACE, new PropertyMD().
				setDescription("Directory on the TSI for the job directories. Must be world read/write/executable."));
		META.put(FILESPACE_UMASK, new PropertyMD("0002").
				setDescription("Umask to be used for creating the base directory for job directories."));
		META.put(STATEDIR, new PropertyMD().setDeprecated().
				setDescription("Directory on the UNICORE/X machine for storing XNJS state."));
		META.put(IDBFILE, new PropertyMD().
				setDescription("IDB configuration.").setCanHaveSubkeys());
		META.put(IDBTYPE, new PropertyMD("json").
				setDescription("IDB format: 'json' (default) or 'xml'"));
		META.put(XNJSWORKERS, new PropertyMD("4").setInt().setNonNegative().
				setDescription("Number of XNJS worker threads."));
		META.put(TSICLASS, new PropertyMD().setClass(TSI.class).setDefault(LocalTS.class.getName()).
				setDeprecated().
				setDescription("(DEPRECATED) Java class name of the TSI implementation."));
		META.put(INCARNATION_TWEAKER_CONFIG, new PropertyMD().setDefault(null).
				setDescription("Path to configuration file for the incarnation tweaker subsystem. " +
						"If not set, the subsystem will be disabled."));
		META.put(ALLOW_USER_EXECUTABLE, new PropertyMD("true").setBoolean().
				setDescription("Whether to allow user-defined executables. If set to false, only applications defined in the IDB may be run."));
		META.put(STRICT_USERINPUT_CHECKING, new PropertyMD("false").setBoolean().
				setDescription("Whether to be restrictive in checking user-supplied arguments and environment variables. Set to true if you do not want ANY user code to run on your TSI node."));
		META.put(STAGING_FS_WAIT, new PropertyMD("false").setBoolean().
				setDescription("Whether to add a waiting loop for files to appear on shared filesystems."));
		META.put(STAGING_FS_GRACE, new PropertyMD("10").setInt().setPositive().
				setDescription("Grace time (in seconds) when waiting for files to appear on shared filesystems."));
		META.put("localtsi", new PropertyMD().setCanHaveSubkeys().
				setDescription("Properties for configuring the embedded Java TSI (if used). See separate docs."));
		META.put("staging", new PropertyMD().setCanHaveSubkeys().
				setDescription("Properties for configuring the data staging and I/O components. See separate docs."));
		META.put(SWEEP_LIMIT, new PropertyMD("200").setInt().setNonNegative().
				setDescription("Upper limit for number of jobs generated in a single parameter sweep."));
	}

	public XNJSProperties()
			throws ConfigurationException {
		this(new Properties());
	}

	public XNJSProperties(Properties properties)
			throws ConfigurationException {
		super(PREFIX, properties, META, logger);
	}
	
	public boolean isAutoSubmitWhenReady(){
		return getBooleanValue(XNJSProperties.AUTOSUBMIT_WHEN_READY);
	}
	
	public int getResubmitCount(){
		return getIntValue(XNJSProperties.RESUBMIT_COUNT);
	}
	
	public int getResubmitDelay(){
		return getIntValue(XNJSProperties.RESUBMIT_DELAY);
	}
	
	// old config keys
	private static final String[]oldNames = {"XNJS.staging.filesystem.grace", 
		"xnjs.jobExecution.allowUserExecutable",
	};
	// new config keys (without the prefix)
	private static final String[]newNames = {STAGING_FS_GRACE, 
		ALLOW_USER_EXECUTABLE,
	};

	@Override
	protected void findUnknown(Properties properties)
			throws ConfigurationException {
		assert oldNames.length == newNames.length;

		// backwards compatibility fixes
		for(int i=0;i<oldNames.length;i++){
			String name = oldNames[i];
			String v = properties.getProperty(name);
			if(v!=null){
				properties.remove(name);
				String newName = PREFIX+newNames[i];
				properties.put(newName, v);
				logger.warn("Old property name "+name+" is DEPRECATED, superseded by <"+newName+">");
			}
		}
		super.findUnknown(properties);
	}

}
