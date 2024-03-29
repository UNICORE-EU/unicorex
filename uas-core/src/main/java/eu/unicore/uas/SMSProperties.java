package eu.unicore.uas;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Logger;

import eu.unicore.uas.impl.sms.DefaultStorageInfoProvider;
import eu.unicore.uas.impl.sms.SMSBaseImpl;
import eu.unicore.uas.impl.sms.StorageInfoProvider;
import eu.unicore.uas.impl.sms.StorageManagementHomeImpl.StorageTypes;
import eu.unicore.uas.util.LogUtil;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyGroupHelper;
import eu.unicore.util.configuration.PropertyMD;

/**
 * This class is used to handle configuration of a particular SMS
 * <p>
 * Note for documentation generation: an artificial prefix must be manually set, as this class is used
 * many times with different prefixed, determined at runtime.
 * 
 * @author K. Benedyczak
 */
public class SMSProperties extends PropertiesHelper {
	
	private static final Logger log = LogUtil.getLogger(LogUtil.CONFIG, SMSProperties.class);

	// Subkeys of general-purpose configuration properties of storages and storage factories.
	public static final String NAME="name";
	public static final String TYPE="type";
	public static final String PATH="path";
	// alternative (deprecated) way to specify path
	public static final String WORKDIR="workdir";
	public static final String CLASS="class";
	public static final String PROTOCOLS="protocols";
	
	public static final String FILTER_LISTING="filterFiles";
	public static final String CLEANUP="cleanup";
	public static final String DISABLE_METADATA="disableMetadata";
	public static final String CHECK_EXISTENCE="checkExistence";
	public static final String ALLOW_USER_DEFINED_PATH="allowUserDefinedPath";
	
	// triggering stuff
	public static final String ENABLE_TRIGGER="enableTrigger";
	public static final String ALLOW_TRIGGER="allowTrigger";
	public static final String SHARED_TRIGGER_USER="triggerUserID";
	
	public static final String UMASK_KEY = "defaultUmask";
	public static final String DESCRIPTION="description";

	// info provider needed for custom SMS types
	public static final String INFO_PROVIDER="infoProviderClass";
	
	public static final String EXTRA_PREFIX="settings.";

	public static final Pattern umaskPattern = Pattern.compile("[0]?[0-7]?[0-7]?[0-7]");
	
	@DocumentationReferenceMeta
	public static final Map<String, PropertyMD> META = new HashMap<>();
	static {
		META.put(NAME, new PropertyMD().
				setDescription("Storage name. If not set then the internal unique identifier is used."));
		META.put(TYPE, new PropertyMD().
				setDescription("Storage type. FIXEDPATH: mapped to a fixed directory, "
						+ "VARIABLE: resolved using an environmental variable lookup, "
						+ "CUSTOM: specified class is used."));
		META.put(PATH, new PropertyMD().
				setDescription("Denotes the storage base path."));
		META.put(CLASS, new PropertyMD().setClass(SMSBaseImpl.class).
				setDescription("Storage implementation class used (and mandatory) in case of the CUSTOM type."));
		META.put(FILTER_LISTING, new PropertyMD("false").setUpdateable().
				setDescription("If set to true then this SMS will filter returned files in response of the ListDirectory command: only files owned or accessible by the caller will be returned."));
		META.put(CLEANUP, new PropertyMD("false").setUpdateable().
				setDescription("Whether files of the storage should be removed when the storage is destroyed. This is mostly useful for storage factories."));
		META.put(DISABLE_METADATA, new PropertyMD("false").
				setDescription("Whether the metadata service should be disabled for this storage."));
		META.put(ENABLE_TRIGGER, new PropertyMD("false").
				setDescription("Whether the triggering feature should be enabled for this storage."));
		META.put(ALLOW_TRIGGER, new PropertyMD("true").
				setDescription("(if creating via factory) If user is allowed to enable the triggering feature."));
		META.put(SHARED_TRIGGER_USER, new PropertyMD().
				setDescription("For data triggering on shared storages, use this user ID for the controlling process."));
		META.put(CHECK_EXISTENCE, new PropertyMD("true").
				setDescription("Whether the existence of the base directory should be checked when creating the storage."));
		META.put(ALLOW_USER_DEFINED_PATH, new PropertyMD("true").
				setDescription("(if creating via factory) Whether the allow the user to set the storage base directory."));
		META.put(UMASK_KEY, new PropertyMD("027").
				setDescription("Default (initial) umask for files in the storage. Must be an octal number."));
		META.put(DESCRIPTION, new PropertyMD("Filesystem").setUpdateable().
				setDescription("Description of the storage. It will be presented to the users."));
		META.put(INFO_PROVIDER, new PropertyMD(DefaultStorageInfoProvider.class, StorageInfoProvider.class).
				setDescription("(Very) advanced setting, providing information about storages produced by the SMS factory."));
		META.put(EXTRA_PREFIX, new PropertyMD().setCanHaveSubkeys().setUpdateable().
				setDescription("Useful for CUSTOM storage types: allows to set additional settings (if needed) by such storages. "
						+ "Please refer to documentation of a particular custom storage type for details. "
						+ "Note that while in general updates of the properties at runtime are propagated "
						+ "to the chosen implementation, it is up to it to use the updated values or ignore changes."));
		META.put(WORKDIR, new PropertyMD().
				setDescription("(DEPRECATED, use 'path' instead)"));
		META.put(PROTOCOLS, new PropertyMD().setUpdateable().
				setDescription("(DEPRECATED, ignored)"));
		
	}
	
	public SMSProperties(String prefix, Properties properties) throws ConfigurationException {
		super(prefix, properties, META, log);
	}

	protected SMSProperties(String prefix, Properties properties, Map<String, PropertyMD> meta, Logger log) throws ConfigurationException {
		super(prefix, properties, meta, log);
		String workdir = getValue(WORKDIR);
		String path = getValue(PATH);
		if(workdir!=null && path ==null){
			setProperty(PATH, workdir);
		}
	}

	@Override
	protected void checkConstraints() throws ConfigurationException {
		super.checkConstraints();
		String type = getValue(TYPE);
		String clazz = getValue(CLASS);
		if (type != null && type.equalsIgnoreCase(StorageTypes.CUSTOM.toString()) && clazz == null)
			throw new ConfigurationException("For the CUSTOM storage type the class must be always set.");
	}
	
	@Override
	protected void checkPropertyConstraints(PropertyMD meta, String key) throws ConfigurationException {
		super.checkPropertyConstraints(meta, key);
		if (key.equals(UMASK_KEY)) {
			String newUmask = getValue(key);
			if (newUmask != null && !umaskPattern.matcher(newUmask).matches())
				throw new ConfigurationException("Specified umask must be an octal number from 0 to 777.");
		}
	}
	
	public Map<String, String> getExtraSettings() {
		PropertyGroupHelper helper = new PropertyGroupHelper(properties, prefix+EXTRA_PREFIX);
		Map<String, String> filteredMap = helper.getFilteredMap();
		Map<String, String> cutMap = new HashMap<String, String>();
		int len = prefix.length()+EXTRA_PREFIX.length();
		for (String key: filteredMap.keySet()) {
			cutMap.put(key.substring(len), filteredMap.get(key));
		}
		return cutMap; 
	}
	
	@Override
	protected void findUnknown(Properties properties) throws ConfigurationException {
		// backwards compatibility fixes
		String n = prefix+"disableTrigger";
		String v = properties.getProperty(n);
		if(v!=null){
			properties.remove(n);
			String newName = prefix+ENABLE_TRIGGER;
			boolean val = Boolean.parseBoolean(v);
			properties.put(newName, String.valueOf(!val));
			log.warn("Property "+n+" is DEPRECATED, superseded by <"+newName+">");
		}
		super.findUnknown(properties);
	}

}
