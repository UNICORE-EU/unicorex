/*
 * Copyright (c) 2012 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package de.fzj.unicore.uas.xtreemfs;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.uas.UASProperties;
import eu.unicore.util.Log;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;

public class XtreemProperties extends PropertiesHelper {
	
	private static final Logger log = Log.getLogger(Log.SERVICES+".xtreemfs", XtreemProperties.class);
	
	@DocumentationReferencePrefix
	public static final String PREFIX = UASProperties.PREFIX + "xtreemfs.";
	
	/**
	 * the path on the TSI filesystem where XtreemFS is mounted locally
	 */
	public static final String XTREEMFS_LOCAL_MOUNT="mountpoint";
	
	
	/**
	 * the URL of a remote SMS providing XtreemFS access
	 */
	public static final String XTREEMFS_REMOTE_URL="url";
	
	@DocumentationReferenceMeta
	public static final Map<String, PropertyMD> META = new HashMap<String, PropertyMD>();
	static {
		META.put(XTREEMFS_LOCAL_MOUNT, new PropertyMD().
				setDescription("The path on the TSI filesystem where XtreemFS is mounted locally."));
		META.put(XTREEMFS_REMOTE_URL, new PropertyMD().
				setDescription("The URL of a remote SMS providing XtreemFS access."));
	}
	
	public XtreemProperties(Properties properties) throws ConfigurationException {
		super(PREFIX, properties, META, log);
	}
}