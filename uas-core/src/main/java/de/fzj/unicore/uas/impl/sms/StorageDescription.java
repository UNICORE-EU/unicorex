/*
 * Copyright (c) 2012 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package de.fzj.unicore.uas.impl.sms;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import de.fzj.unicore.uas.SMSProperties;
import de.fzj.unicore.uas.impl.sms.StorageManagementHomeImpl.StorageTypes;
import eu.unicore.util.configuration.ConfigurationException;

/**
 * Parameters used to configure storages which are based on {@link SMSBaseImpl}. Some of the options 
 * are not used by all actual storage implementations. See constructor documentation for details.
 * 
 * @author K. Benedyczak
 */
public class StorageDescription implements Serializable, Cloneable {
	
	private static final long serialVersionUID = 1L;
	
	private final String id;
	private String clazz;
	private final StorageTypes type;
	
	private String name;
	private String infoProviderClass = DefaultStorageInfoProvider.class.getName();
	private String pathSpec;
	private boolean allowUserDefinedPath;
	private boolean disableMetadata;
	
	//updateable
	private String description;
	private Map<String, String> additionalProperties;
	private boolean filterListing;
	private boolean enableTrigger;
	private String sharedTriggerUser;
	private boolean cleanup;
	private String defaultUmask = "077";

	// only used when created
	private transient boolean checkExistence = true;
	
	/**
	 * basic constructor, other settings need to be set via API
	 * @param id - storage ID
	 * @param name - storage name 
	 * @param type - storage type
	 * @param clazz - implementation class
	 */
	public StorageDescription(String id, String name, StorageTypes type, String clazz){
		this.id=id;
		this.name = name == null ? id : name;
		this.type=type;
		this.clazz=clazz;
	}
	
	public void setDisableMetadata(boolean disable){
		this.disableMetadata=disable;
	}
	
	public boolean isDisableMetadata(){
		return disableMetadata;
	}
	
	public void setEnableTrigger(boolean enable){
		this.enableTrigger = enable;
	}
	
	public boolean isEnableTrigger(){
		return enableTrigger;
	}
	
	public boolean isAllowUserdefinedPath(){
		return allowUserDefinedPath;
	}
	
	public void setAllowUserDefinedPath(boolean allow){
		this.allowUserDefinedPath=allow;
	}
	
	/**
	 * update the given settings
	 * 
	 * @param filterListing
	 * @param cleanup
	 * @param defaultUmask
	 * @param description
	 * @param additionalProperties
	 */
	public void update(Boolean filterListing, Boolean cleanup, String defaultUmask, 
			String description, Map<String, String> additionalProperties) {
		this.description = description == null ? "Filesystem" : description;
		if (additionalProperties == null)
			additionalProperties = Collections.emptyMap();
		this.additionalProperties = additionalProperties;
		this.filterListing = filterListing == null ? false : filterListing;
		this.defaultUmask = defaultUmask == null ? "077" : 
			defaultUmask;
		this.cleanup = cleanup == null ? true : cleanup;
		if (!SMSProperties.umaskPattern.matcher(this.defaultUmask).matches())
			throw new ConfigurationException("Specified umask must be an octal number from 0 to 777.");
		if (type.equals(StorageTypes.CUSTOM) && clazz == null)
			throw new ConfigurationException("No class defined for the CUSTOM storage");
	}
	
	public String getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description=description;
	}
	
	public StorageTypes getStorageType() {
		return type;
	}

	public String getStorageTypeAsString() {
		return type.toString();
	}

	public Map<String, String> getAdditionalProperties() {
		return additionalProperties;
	}
	
	public String getPathSpec() {
		return pathSpec;
	}

	@SuppressWarnings("unchecked") 
	public Class<? extends SMSBaseImpl> getStorageClass() {
		try{
			return clazz!=null ? (Class<? extends SMSBaseImpl>)(Class.forName(clazz)) : null;
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
	public boolean isFilterListing() {
		return filterListing;
	}

	public boolean isCleanupOnDestroy() {
		return cleanup;
	}
	
	public String getDefaultUmask() {
		return defaultUmask;
	}
	
	public void setDefaultUmask(String umask) {
		this.defaultUmask=umask;
	}
	
	@SuppressWarnings("unchecked")
	public Class<? extends StorageInfoProvider> getInfoProviderClass() {
		try{
			return infoProviderClass != null ? 
					(Class<? extends StorageInfoProvider>)Class.forName(infoProviderClass) 
					: null;
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}

	public void setInfoProviderClass(String clazz) {
		this.infoProviderClass = clazz;
	}
	
	/**
	 * whether to test the existence of the base directory when creating the storage
	 */
	public boolean isCheckExistence(){
		return checkExistence;
	}

	public void setCheckExistence(boolean checkExistence){
		this.checkExistence = checkExistence;
	}
	
	public boolean isCleanup() {
		return cleanup;
	}

	public void setCleanup(boolean cleanup) {
		this.cleanup = cleanup;
	}

	public void setPathSpec(String pathSpec) {
		this.pathSpec = pathSpec;
	}

	public void setAdditionalProperties(Map<String, String> additionalProperties) {
		this.additionalProperties = additionalProperties;
	}

	public void setFilterListing(boolean filterListing) {
		this.filterListing = filterListing;
	}

	public String getSharedTriggerUser() {
		return sharedTriggerUser;
	}

	public void setSharedTriggerUser(String sharedTriggerUser) {
		this.sharedTriggerUser = sharedTriggerUser;
	}

	public String toString(){
		return "Storage description: " + "name="+name
				+ ", type="+type
				+ ", pathSpec=" + pathSpec 
				+ ", defaultUmask=" + defaultUmask 
				+ ", filterListing="+filterListing
				+ ", metadata="+!disableMetadata
				+ ", triggering="+enableTrigger
				+ (enableTrigger && sharedTriggerUser!=null 
					? ", triggerUserID="+sharedTriggerUser : "")
				;
	}
	
	public void setStorageClass(String clazz){
		this.clazz = clazz;
	}

	public StorageDescription clone() {
		Map<String, String> clonedAdd = new HashMap<String, String>();
		clonedAdd.putAll(additionalProperties);
		StorageDescription ret = new StorageDescription(id, name, type, clazz);
		ret.setPathSpec(pathSpec);
		ret.setFilterListing(filterListing);
		ret.setCleanup(cleanup);
		ret.setDisableMetadata(disableMetadata);
		ret.setEnableTrigger(enableTrigger);
		ret.setSharedTriggerUser(sharedTriggerUser);
		ret.setDefaultUmask(defaultUmask);
		ret.setDescription(description);
		ret.setAdditionalProperties(clonedAdd);
		if (infoProviderClass != null)
			ret.setInfoProviderClass(infoProviderClass);
		ret.setCheckExistence(checkExistence);
		ret.setAllowUserDefinedPath(allowUserDefinedPath);
		ret.setStorageClass(clazz);
		return ret;
	}
}