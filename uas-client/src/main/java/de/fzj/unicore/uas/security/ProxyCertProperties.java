package de.fzj.unicore.uas.security;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.Logger;

import eu.unicore.util.Log;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;

public class ProxyCertProperties extends PropertiesHelper {
	
	private static final Logger log = Log.getLogger(Log.CONFIGURATION, ProxyCertProperties.class);
	
	public static final String PREFIX="unicore.proxy.";
	

	/**
	 * property key for defining the lifetime in seconds (default: 2*3600, i.e. two hours)
	 */
	public static final String PROXY_LIFETIME="lifetime";
	
	/**
	 * property key for defining the keysize (default: 1024)
	 */
	public static final String PROXY_KEYSIZE="keysize";
	
	/**
	 * property key defining an existing proxy file to be used
	 */
	public static final String PROXY_FILE="file";
	
	
	public final static Map<String, PropertyMD> META = new HashMap<String, PropertyMD>();
	
	static 
	{
		META.put(PROXY_LIFETIME, new PropertyMD("43200").setInt().setPositive().
				setDescription("The lifetime of the generated proxy in seconds."));
		META.put(PROXY_KEYSIZE, new PropertyMD("1024").setInt().setPositive().
				setDescription("The key size of the generated proxy."));
		META.put(PROXY_FILE, new PropertyMD().
				setDescription("A pre-existing proxy file to be used."));
	}
	
	public ProxyCertProperties(String prefix, Properties properties) throws ConfigurationException
	{
		super(prefix, properties, META, log);
	}	

	public ProxyCertProperties(Properties properties) throws ConfigurationException
	{
		super(PREFIX, properties, META, log);
	}
	
	public int getLifetime(){
		return getIntValue(PROXY_LIFETIME);
	}
	
	public int getKeysize(){
		return getIntValue(PROXY_KEYSIZE);
	}
	
	/**
	 * checks whether the filename option is set
	 */
	public boolean isSetFileName(){
		return getValue(PROXY_FILE)!=null;
	}
}
