package de.fzj.unicore.xnjs.tsi.local;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.inject.Singleton;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.xnjs.XNJSProperties;
import eu.unicore.util.Log;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;

/**
 * config properties for the local (embedded) TSI
 *
 * @author schuller
 */
@Singleton
public class LocalTSIProperties extends PropertiesHelper {

	private static final Logger logger=Log.getLogger(Log.SERVICES, LocalTSIProperties.class);

	@DocumentationReferencePrefix
	public static final String PREFIX=XNJSProperties.PREFIX+"localtsi.";

	public final static String USE_SHELL="useShell";

	public final static String SHELL="shell";

	public static final String JOBLIMIT = "jobLimit";
	
	public final static String WORKER_THREADS = "workerThreads";
	
	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> META = new HashMap<String, PropertyMD>();
	static
	{
		META.put(JOBLIMIT, new PropertyMD("0").setInt().
				setDescription("Maximum number of concurrent jobs, if set to a value >0. Default is no limit."));
		META.put(SHELL, new PropertyMD("/bin/bash").
				setDescription("Default UNIX shell to use (if shell is used)."));
		META.put(USE_SHELL, new PropertyMD("true").setBoolean().
				setDescription("Should a UNIX shell be used to execute jobs."));
		META.put(WORKER_THREADS, new PropertyMD("4").setInt().setPositive().
				setDescription("Number of worker threads used to execute jobs."));
	}

	public LocalTSIProperties()
			throws ConfigurationException {
		this(new Properties());
	}

	public LocalTSIProperties(Properties properties)
			throws ConfigurationException {
		super(PREFIX, properties, META, logger);
	}
	
	public int getJobLimit(){
		return getIntValue(JOBLIMIT);
	}

	public boolean isUseShell(){
		return getBooleanValue(USE_SHELL);
	}
	
	public String getShell(){
		return getValue(SHELL);
	}
	
	public int getNumberOfThreads(){
		return getIntValue(WORKER_THREADS);
	}

}
