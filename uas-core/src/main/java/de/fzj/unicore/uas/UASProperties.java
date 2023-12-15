package de.fzj.unicore.uas;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import com.google.inject.AbstractModule;

import de.fzj.unicore.uas.impl.sms.SMSBaseImpl;
import de.fzj.unicore.uas.impl.sms.StorageDescription;
import de.fzj.unicore.uas.impl.sms.StorageFactoryImpl;
import de.fzj.unicore.uas.impl.sms.StorageInfoProvider;
import de.fzj.unicore.uas.impl.sms.StorageManagementHomeImpl.StorageTypes;
import de.fzj.unicore.uas.impl.tss.TargetSystemFactoryImpl;
import de.fzj.unicore.uas.impl.tss.TargetSystemImpl;
import eu.unicore.services.Resource;
import eu.unicore.util.Log;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyChangeListener;
import eu.unicore.util.configuration.PropertyMD;

/**
 * Configuration of the settings specific to UNICORE core services.
 * Except of defining the typical properties, a methods to retrieve parsed storage and storage factory 
 * descriptions are provided.
 * 
 * @author K. Benedyczak
 * @author B. Schuller
 */
public class UASProperties extends PropertiesHelper {
	private static final Logger log = Log.getLogger(Log.CONFIGURATION, UASProperties.class);

	@DocumentationReferencePrefix
	public static final String PREFIX = "coreServices."; 

	@Deprecated
	public static final String TSF_XNJS_CONFIGFILE = "targetsystemfactory.xnjs.configfile";

	/**
	 * TSI mode: remote, embedded, or custom
	 */
	public static final String TSF_TSI_MODE = "targetsystemfactory.tsiMode";

	public static enum TSI_MODE {
		remote,
		embedded,
		custom,
	}

	public static final String TSF_TSI_CUSTOM_MODULE = "targetsystemfactory.tsiCustomModeModuleClass";

	/**
	 *  the TSF class
	 */
	public static final String TSF_CLASS = "targetsystemfactory.class";
	
	/**
	 *  the TSS class
	 */
	public static final String TSS_CLASS = "targetsystemfactory.tssClass";
	
	/**
	 * if set to <code>true</code>, the storages attached to a TSS will always
	 * have unique IDs. In the default case, storage names will be formed from
	 * the user's xlogin and the storage name, e.g. "a.user-Home".
	 */
	public static final String TSS_FORCE_UNIQUE_STORAGE_IDS = "targetsystem.uniqueStorageIds";

	@Deprecated
	public static final String SMS_PROTOCOLS = "sms.protocols";

	public static final String SMS_LS_LIMIT = "sms.lsLimit";

	public static final String SMS_ENABLED_ADDON_STORAGES="targetsystem.enabledStorages";

	public static final String SMS_ADDON_STORAGE_PREFIX="targetsystem.storage.";

	public static final String SMS_FACTORY_PREFIX = "sms.factory.";

	public static final String SMS_ENABLED_FACTORIES = "sms.enabledFactories";

	public static final String SMS_FACTORY_CLASS = "sms.factoryClass";

	/**
	 * When doing file transfers, UNICORE tries to detect whether two storage
	 * resources are accessing the same filesystem. If yes, the transfer is done
	 * by direct copying. Set to "true" to disable this feature.
	 */
	public final static String SMS_TRANSFER_FORCEREMOTE = "filetransfer.forceremote";

	@Deprecated
	public static final String SMS_DIRECT_FILETRANSFER = "filetransfer.direct";
	
	@Deprecated
	public static final String SMS_STAGING_MAXTHREADS = "filetransfer.maxthreads";

	public static final String FTS_HTTP_PREFER_POST = "filetransfer.httpPreferPost";

	/**
	 * prefix for configuring the set of "shared SMS" (see {@link CreateSMSOnStartup})
	 */
	public static final String SMS_SHARE_PREFIX = "sms.storage.";

	@Deprecated
	public static final String SMS_SHARE_PREFIX_old = "sms.share.";

	/**
	 * property listing the enabled "shared SMS" (see {@link CreateSMSOnStartup})
	 */
	public static final String SMS_ENABLED_SHARES = "sms.enabledStorages";

	@Deprecated
	public static final String SMS_ENABLED_SHARES_old = "sms.enabledShares";

	/**
	 * prefix for configuring the storage service accessing the job working directory
	 */
	public static final String USPACE_SMS_PREFIX = "sms.jobDirectories.";

	/**
	 * the property name for configuring the "default" MetadataManager implementation
	 */
	public static final String METADATA_MANAGER_CLASSNAME="metadata.managerClass";

	private Collection<StorageDescription> addOnStorages;
	private Map<String, StorageDescription> factories;

	@DocumentationReferenceMeta
	public static final Map<String, PropertyMD> META = new HashMap<>();
	static {

		META.put(TSS_FORCE_UNIQUE_STORAGE_IDS, new PropertyMD("false").setDeprecated().
				setDescription("Whether to use unique identifiers for all storages attached to TSS services. "
						+ "If set to true, the storages attached to a TSS will always have unique IDs. "
						+ "In the default case, storage names will be formed from the user's xlogin and the storage name, e.g. 'a.user-Home'. "
						+ "Unique identifiers are long and not possible to be predicted but are needed if many grid users are mapped to "
						+ "the same local account."));

		META.put(SMS_TRANSFER_FORCEREMOTE, new PropertyMD("false").
				setDescription("When doing file transfers, UNICORE tries to detect whether two storage resources are accessing the same (local) filesystem. If yes, the transfer is done by direct copying. Set to 'true' to disable this feature."));

		//note: if you want to change the maximum of the property, 
		//then also implementation of ListDirectory needs to be changed as it uses the constants.
		//default and min can be freely changed. 
		META.put(SMS_LS_LIMIT, new PropertyMD(SMSBaseImpl.MAX_LS_RESULTS+"").setBounds(1, SMSBaseImpl.MAX_LS_RESULTS).
				setDescription("Controls the default amount of ListDirectory results which are returned in a single query."));
		META.put(SMS_ENABLED_ADDON_STORAGES, new PropertyMD((String)null).
				setDescription("Space separated list of names of enabled additional storages. If this property is left undefined then all defined storages are enabled. If this property value is empty then all are disabled."));
		META.put(SMS_ADDON_STORAGE_PREFIX, new PropertyMD().setCanHaveSubkeys().
				setDescription("Properties with this prefix are used to configure additional storages. See documentation of TargetSystem storages for details."));
		META.put(SMS_FACTORY_PREFIX, new PropertyMD().setCanHaveSubkeys().
				setDescription("Properties with this prefix are used to configure storage factories. See documentation of storage factories for details."));
		META.put(SMS_FACTORY_CLASS, new PropertyMD(StorageFactoryImpl.class, StorageFactoryImpl.class).
				setDescription("Implementation class name for the StorageFactory."));		
		META.put(SMS_ENABLED_FACTORIES, new PropertyMD((String)null).
				setDescription("Space separated list of names of enabled storage factories. If this property is left undefined then all defined factories are enabled. If this property value is empty then all are disabled."));
		
		META.put(SMS_SHARE_PREFIX, new PropertyMD().setCanHaveSubkeys().
				setDescription("Properties with this prefix are used to configure storages. See documentation of storage factories for details."));		
		META.put(SMS_ENABLED_SHARES, new PropertyMD((String)null).
				setDescription("Space separated list of names of enabled storages. If this property is left undefined then all defined shares are enabled. "
						+ "If this property value is empty then all are disabled."));
		
		META.put(FTS_HTTP_PREFER_POST, new PropertyMD("false").
				setDescription("Controls whether to use HTTP POST for the HTTP filetransfers."));

		META.put(USPACE_SMS_PREFIX, new PropertyMD().setCanHaveSubkeys().
				setDescription("Properties with this prefix are used to configure the job working directory storage (i.e. the storage bound to each job). See documentation of storage factories for details."));
		
		META.put(TSF_CLASS, new PropertyMD(TargetSystemFactoryImpl.class, Resource.class).
				setDescription("The Java class to use for the target system factory."));
		META.put(TSS_CLASS, new PropertyMD(TargetSystemImpl.class,TargetSystemImpl.class).
				setDescription("The Java class to use for the target system."));

		META.put(TSF_TSI_MODE, new PropertyMD("remote").setEnum(TSI_MODE.remote).
				setDescription("Whether to use the UNICORE TSI server or a local TSI."));
		META.put(TSF_TSI_CUSTOM_MODULE, new PropertyMD().setClass(AbstractModule.class).
				setDescription("If tsiMode is 'custom', this defines the name of the module class."));

		// no reasonable default is possible, unless we merge the Lucene MM into core
		META.put(METADATA_MANAGER_CLASSNAME, new PropertyMD().
				setDescription("Metadata manager class name."));

		//register namespaces for other modules
		META.put("metadata.", new PropertyMD().setHidden().setCanHaveSubkeys());
		META.put("uftp.", new PropertyMD().setHidden().setCanHaveSubkeys());
		META.put("xtreemfs.", new PropertyMD().setHidden().setCanHaveSubkeys());
		META.put("extension.", new PropertyMD().setHidden().setCanHaveSubkeys());
		META.put("jclouds.", new PropertyMD().setHidden().setCanHaveSubkeys());
		

		// deprecated stuff
		META.put("bes.", new PropertyMD().setDeprecated().setHidden().setCanHaveSubkeys());
		META.put(TSF_XNJS_CONFIGFILE, new PropertyMD().setPath().setDeprecated().
				setDescription("(DEPRECATED, UNUSED)"));
		META.put(SMS_PROTOCOLS, new PropertyMD().setUpdateable().setDeprecated().
				setDescription("(DEPRECATED, UNUSED)"));
		META.put(SMS_DIRECT_FILETRANSFER, new PropertyMD("false").setDeprecated().
				setDescription("(DEPRECATED, UNUSED)"));
		META.put(SMS_SHARE_PREFIX_old, new PropertyMD().setCanHaveSubkeys().setDeprecated().
				setDescription("DEPRECATED, use coreServices.sms.storage.* instead"));		
		META.put(SMS_ENABLED_SHARES_old, new PropertyMD((String)null).setDeprecated().
				setDescription("DEPRECATED, use coreServices.sms.enabledStorages instead"));
		META.put(SMS_STAGING_MAXTHREADS, new PropertyMD().setDeprecated().
				setDescription("DEPRECATED, use XNJS.staging.threads"));
	}

	public UASProperties(Properties properties) throws ConfigurationException, IOException {
		super(PREFIX, properties, META, log);
		factories = parseStorages(SMS_FACTORY_PREFIX, SMS_ENABLED_FACTORIES, true);
		addOnStorages = parseStorages(SMS_ADDON_STORAGE_PREFIX, 
				SMS_ENABLED_ADDON_STORAGES, false).values();

		addPropertyChangeListener(new PropertyChangeListener() {
			private final String[] PROPS = {SMS_ADDON_STORAGE_PREFIX, SMS_FACTORY_PREFIX};

			@Override
			public void propertyChanged(String propertyKey) {
				if (propertyKey.equals(SMS_ADDON_STORAGE_PREFIX))
					updateStorages(addOnStorages, SMS_ADDON_STORAGE_PREFIX, false);
				else if (propertyKey.equals(SMS_FACTORY_PREFIX))
					updateStorages(factories.values(), SMS_FACTORY_PREFIX, true);
			}

			@Override
			public String[] getInterestingProperties() {
				return PROPS;
			}
		});
	}

	@Override
	protected void checkConstraints() throws ConfigurationException {
		super.checkConstraints();
		//this could be optimized little bit as creation of StorageDescriptions is not required for parsing... 
		parseStorages(SMS_FACTORY_PREFIX, SMS_ENABLED_FACTORIES, true);
		parseStorages(SMS_ADDON_STORAGE_PREFIX, SMS_ENABLED_ADDON_STORAGES, false);
	}

	/**
	 * @return a list of TSMSes configurations
	 */
	public Collection<StorageDescription> getAddonStorages() {
		return addOnStorages;
	}

	/**
	 * @return a map of SMSFactories configurations
	 */
	public Map<String, StorageDescription> getStorageFactories() {
		return factories;
	}

	/**
	 * Updates the existing storage descriptions
	 */
	private void updateStorages(Collection<StorageDescription> storages, 
			String prefixComponent, boolean factory) {
		for(StorageDescription storage: storages) {
			String pfx = prefixComponent + storage.getId() + ".";
			SMSProperties smsProps = factory ? new SMSProperties(pfx, properties) :
				new SMSProperties(pfx, properties);
			storage.update(smsProps.getBooleanValue(SMSProperties.FILTER_LISTING),
					smsProps.getBooleanValue(SMSProperties.CLEANUP),
					smsProps.getValue(SMSProperties.UMASK_KEY),
					smsProps.getValue(SMSProperties.DESCRIPTION),
					smsProps.getExtraSettings());
		}
	}


	/**
	 * @return a map with SMS or factory configurations, by name
	 * 
	 * @param prefixComponent - the property prefix
	 * @param enabledKey - the property key for getting the enabled storages
	 * @param disableExistenceCheck - whether to force disabling the directory existence check 
	 */
	public Map<String, StorageDescription> parseStorages(String prefixComponent, 
			String enabledKey, boolean disableExistenceCheck) {
		String enabledV = getValue(enabledKey); 
		Set<String> storageIds = getStorageIds(enabledV, prefixComponent, properties);

		Map<String, StorageDescription> ret = new HashMap<String, StorageDescription>();
		for(String id: storageIds) {
			try {
				String pfx = PREFIX+prefixComponent + id + ".";
				SMSProperties smsProps = new SMSProperties(pfx, properties);
				Class<? extends SMSBaseImpl> cl = smsProps.getClassValue(SMSProperties.CLASS, SMSBaseImpl.class);
				String clazz = cl != null ? cl.getName() : null; 
				String path = smsProps.getValue(SMSProperties.PATH);
				if(path == null)path = smsProps.getValue(SMSProperties.WORKDIR);
				String typeS = smsProps.getValue(SMSProperties.TYPE);
				if(typeS==null) {
					if(path!=null && path.contains("$")) {
						typeS = StorageTypes.VARIABLE.toString();
					}
					else {
						typeS = StorageTypes.FIXEDPATH.toString();
					}
				}
				StorageDescription asd = new StorageDescription(id,
						smsProps.getValue(SMSProperties.NAME),
						StorageTypes.valueOf(typeS),
						clazz); 
				configureCommon(asd, smsProps, path, !disableExistenceCheck); 
				ret.put(id, asd);
			}catch(Exception ex) {
				throw new ConfigurationException("Could not parse Storage <"+id+">", ex);
			}
		}
		return ret;
	}

	/**
	 * @return a SMS configuration
	 * @param prefix - the prefix for reading the properties
	 * @param id - the storage ID
	 * @param factory - whether the storage description is for a storagefactory
	 */
	public StorageDescription parseStorage(String prefix, String id, boolean factory) {
		String pfx = PREFIX + prefix ;

		SMSProperties smsProps = factory ? new SMSProperties(pfx, properties) :
			new SMSProperties(pfx, properties);
		Class<? extends SMSBaseImpl> cl = smsProps.getClassValue(SMSProperties.CLASS, SMSBaseImpl.class);
		String clazz = cl != null ? cl.getName() : null;
		String path = smsProps.getValue(SMSProperties.PATH);
		if(path == null)path = smsProps.getValue(SMSProperties.WORKDIR);
		String typeS = smsProps.getValue(SMSProperties.TYPE);
		if(typeS==null) {
			if(path!=null && path.contains("$")) {
				typeS = StorageTypes.VARIABLE.toString();
			}
			else {
				typeS = StorageTypes.FIXEDPATH.toString();
			}
		}
		StorageDescription asd = new StorageDescription(id,
				smsProps.getValue(SMSProperties.NAME),
				StorageTypes.valueOf(typeS),
				clazz);
		boolean checkExistence = !factory && smsProps.getBooleanValue(SMSProperties.CHECK_EXISTENCE);
		configureCommon(asd, smsProps, path, checkExistence);
		return asd;
	}
	
	
	protected void configureCommon(StorageDescription asd, SMSProperties smsProps, String path, boolean checkExist){
		asd.setFilterListing(smsProps.getBooleanValue(SMSProperties.FILTER_LISTING));
		asd.setCleanup(smsProps.getBooleanValue(SMSProperties.CLEANUP));
		asd.setAllowUserDefinedPath(smsProps.getBooleanValue(SMSProperties.ALLOW_USER_DEFINED_PATH));
		asd.setDisableMetadata(smsProps.getBooleanValue(SMSProperties.DISABLE_METADATA));
		asd.setEnableTrigger(smsProps.getBooleanValue(SMSProperties.ENABLE_TRIGGER));
		asd.setSharedTriggerUser(smsProps.getValue(SMSProperties.SHARED_TRIGGER_USER));
		asd.setDefaultUmask(smsProps.getValue(SMSProperties.UMASK_KEY));
		asd.setDescription(smsProps.getValue(SMSProperties.DESCRIPTION));
		asd.setAdditionalProperties(smsProps.getExtraSettings());
		Class<? extends StorageInfoProvider> infoClass=smsProps.getClassValue(
				SMSProperties.INFO_PROVIDER, StorageInfoProvider.class);
		asd.setInfoProviderClass(infoClass.getName());
		asd.setCheckExistence(checkExist);
		asd.setPathSpec(path);
	}



	/**
	 * @return a list of SMSs/factories configuration ids.
	 */
	public static Set<String> getStorageIds(String enabledTypesV, String prefixComponent, Properties properties) {
		boolean useAll = false;
		Set<String> enabledTypes = null;
		if (enabledTypesV == null)
			useAll = true;
		else {
			String[] enabledTypesA = enabledTypesV.trim().split(" +");
			enabledTypes = new HashSet<String>(enabledTypesA.length);
			Collections.addAll(enabledTypes, enabledTypesA);
		}


		String pfx = PREFIX+prefixComponent;
		Set<String> ret = new HashSet<String>();
		for(Object k: properties.keySet()){
			String key=((String)k).trim();
			if (!key.startsWith(pfx) || key.length() < pfx.length())
				continue;

			String id = key.substring(pfx.length());
			if (!id.contains("."))
				continue;
			id = id.substring(0, id.indexOf('.'));
			if (id.length() == 0)
				continue;

			if (!useAll && !enabledTypes.contains(id)) {
				log.debug("Skipping storage definition " + id + " as it is not enabled.");
				continue;
			}
			ret.add(id);
		}
		return ret;
	}

}
