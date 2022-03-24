package de.fzj.unicore.xnjs.io;

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
 * config properties for the IO / data staging components
 *
 * @author schuller
 */
@Singleton
public class IOProperties extends PropertiesHelper {

	private static final Logger logger=Log.getLogger(Log.SERVICES, IOProperties.class);

	@DocumentationReferencePrefix
	public static final String PREFIX=XNJSProperties.PREFIX+"staging.";

	public static final String FT_THREADS = "threads";

	public static final String STAGING_FS_GRACE = "filesystemGraceTime";
	public static final String STAGING_FS_WAIT = "addWaitingLoop";

	public static final String WGET = "wget";
	public static final String WGET_PARAMS = "wgetParameters";
	public static final String CURL = "curl";
	public static final String SCP_WRAPPER = "scpWrapper";
	public static final String GLOBUS_URL_COPY = "gridftp";
	public static final String GLOBUS_URL_COPY_PARAMS = "gridftpParameters";
	public static final String MAIL_HOST = "mailHost";
	public static final String MAIL_PORT = "mailPort";
	public static final String MAIL_USER = "mailUser";
	public static final String MAIL_PWD = "mailPassword";
	public static final String MAIL_SSL ="mailEnableSSL";
	public static final String MAIL_WRAPPER="mailSendScript";
	
	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> META = new HashMap<String, PropertyMD>();
	static
	{
		META.put(FT_THREADS, new PropertyMD("4").setInt().setPositive().
				setDescription("Number of worker threads to use for data staging."));
		
		META.put(STAGING_FS_GRACE, new PropertyMD("10").setInt().setPositive().
				setDescription("Grace time (in seconds) when waiting for files to appear on shared filesystems."));
		META.put(STAGING_FS_WAIT, new PropertyMD("false").setBoolean().
				setDescription("Whether to add a waiting loop for files to appear on shared filesystems."));
		
		META.put(WGET, new PropertyMD().
				setDescription("Location of the 'wget' executable used for HTTP stage-ins. " +
						"If null, Java code will be used for HTTP."));
		META.put(WGET_PARAMS, new PropertyMD().
				setDescription("Additional options for 'wget'."));
		META.put(GLOBUS_URL_COPY, new PropertyMD("globus-url-copy").
				setDescription("Location of the 'globus-url-copy' executable used for GridFTP staging."));
		META.put(GLOBUS_URL_COPY_PARAMS, new PropertyMD("").
				setDescription("Additional options for 'globus-url-copy'."));
		META.put(CURL, new PropertyMD().
				setDescription("Location of the 'curl' executable used for FTP stage-ins. " +
						"If null, Java code will be used for FTP."));
		META.put(SCP_WRAPPER, new PropertyMD("scp-wrapper.sh").
				setDescription("Location of the wrapper script used for scp staging."));
		META.put(MAIL_HOST, new PropertyMD("localhost").
				setDescription("Outgoing mail host (SMTP) used for staging-out via email."));
		META.put(MAIL_PORT, new PropertyMD("25").
				setDescription("Outgoing mail (SMTP) port number."));
		META.put(MAIL_USER, new PropertyMD().
				setDescription("Outgoing mail (SMTP) user name."));
		META.put(MAIL_PWD, new PropertyMD().
				setDescription("Outgoing mail (SMTP) password."));
		META.put(MAIL_SSL, new PropertyMD("false").
				setDescription("Outgoing mail (SMTP): enable SSL connection."));
		META.put(MAIL_WRAPPER, new PropertyMD().
				setDescription("Script to be used for sending outgoing mail (instead of using SMTP)."));
	}

	public IOProperties()
			throws ConfigurationException {
		this(new Properties());
	}

	public IOProperties(Properties properties)
			throws ConfigurationException {
		super(PREFIX, properties, META, logger);
	}

	// old config keys
	private static final String[]oldNames = {"XNJS.staging.filesystem.grace", 
		"wget","wget.parameters","curl",
		"scp-wrapper.sh",
		"globus-url-copy",
		"globus-url-copy.parameters",
		"mail.smtp.host","mail.smtp.port","mail.smtp.user","mail.smtp.password","mail.smtp.ssl",
		"mail-wrapper.sh",
		};
	// new config keys (without the prefix)
	private static final String[]newNames = {STAGING_FS_GRACE, 
		WGET,WGET_PARAMS,CURL,
		SCP_WRAPPER,
		GLOBUS_URL_COPY,GLOBUS_URL_COPY_PARAMS,
		MAIL_HOST,MAIL_PORT,MAIL_USER,MAIL_PWD,MAIL_SSL,
		MAIL_WRAPPER,
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
