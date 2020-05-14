/*
 * Copyright (c) 2012 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package eu.unicore.uas.metadata;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.tika.parser.Parser;

import de.fzj.unicore.uas.UASProperties;
import de.fzj.unicore.uas.metadata.MetadataManager;
import de.fzj.unicore.uas.util.LogUtil;
import eu.unicore.uas.metadata.utils.ExtensionParser;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;

/**
 * Configuration of the metadata subsystem.
 * @author K. Benedyczak
 */
public class MetadataProperties extends PropertiesHelper {
	
	private static final Logger log = LogUtil.getLogger(LogUtil.DATA, MetadataProperties.class);
	
	@DocumentationReferencePrefix
	public static final String PREFIX = UASProperties.PREFIX + "metadata.";
	
	public static final String MANAGER_CLASSNAME = "managerClass";
	public static final String PARSER_CLASSNAME = "parserClass";
	public static final String LUCENE_INDEX_DIR = "luceneDirectory";
	
	@DocumentationReferenceMeta
	public static final Map<String, PropertyMD> META = new HashMap<String, PropertyMD>();
	static {
		META.put(MANAGER_CLASSNAME, new PropertyMD(LuceneMetadataManager.class, MetadataManager.class).
				setDescription("Class name of the metadata manager class."));
		META.put(PARSER_CLASSNAME, new PropertyMD(ExtensionParser.class, Parser.class).
				setDescription("Class name of the metadata extractor."));
		META.put(LUCENE_INDEX_DIR, new PropertyMD("/tmp/data/luceneIndexFiles/").setPath().
				setDescription("Directory name where the Lucene index should be located."));
	}
	
	public MetadataProperties(Properties properties) throws ConfigurationException {
		super(PREFIX, properties, META, log);
	}
}
