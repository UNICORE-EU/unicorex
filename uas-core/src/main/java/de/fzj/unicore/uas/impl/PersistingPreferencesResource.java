/*
 * Copyright (c) 2012 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */

package de.fzj.unicore.uas.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.uas.util.LogUtil;
import de.fzj.unicore.wsrflite.InitParameters;
import de.fzj.unicore.wsrflite.security.util.AuthZAttributeStore;
import eu.unicore.security.SecurityTokens;

/**
 * Adds persistence of the initial user's preferences to the resource.
 * Such preferences are used as default preferences assuming that the user has not
 * specified other.
 *  
 * @author K. Benedyczak
 */
public abstract class PersistingPreferencesResource extends BaseResourceImpl {
	
	private static final Logger logger=LogUtil.getLogger(LogUtil.SERVICES,PersistingPreferencesResource.class);

	public PersistingPreferencesResource(){
		super();
	}
	
	@Override
	public PersistingPrefsModel getModel(){
		return (PersistingPrefsModel)model; 
	}
	
	/**
	 * Saves the initial user's preferences
	 */
	@Override
	public void initialise(InitParameters initParams) throws Exception {
		PersistingPrefsModel m = getModel();
		if(m==null){
			m = new PersistingPrefsModel();
			setModel(m);
		}
		
		super.initialise(initParams);
		SecurityTokens secTokens = AuthZAttributeStore.getTokens();
		if (secTokens == null)
			return;
		
		Map<String, String[]> actualPreferences = secTokens.getUserPreferences();
		Map<String, String[]> storedPreferences = new HashMap<String, String[]>();
		storedPreferences.putAll(actualPreferences);
		m.setStoredPreferences(storedPreferences);
		if (logger.isDebugEnabled())
			logger.debug("Persisted user preferences, used to create the WS-Resource "
					+ getServiceName() + " [" + getUniqueID() + "]: " +
					storedPreferences.keySet());

	}
	
	/**
	 * Applies the saved user's preferences (before the whole 
	 * USE authorization pipeline) whenever the user has not selected a preference 
	 * explicitly.
	 */
	@Override
	public void updateSecurityTokensBeforeAIP(SecurityTokens secTokens) {
		Map<String, String[]> storedPreferences = getModel().getStoredPreferences(); 
		if (storedPreferences == null || storedPreferences.size() == 0)
			return;
		
		Map<String, String[]> actualPreferences = secTokens.getUserPreferences();
			
		for (Map.Entry<String, String[]> entry: storedPreferences.entrySet()) {
			if (!actualPreferences.containsKey(entry.getKey())) {
				actualPreferences.put(entry.getKey(), entry.getValue());
				if (logger.isDebugEnabled())
					logger.debug("Using saved user's preference as a default for "
						+ getServiceName() + " [" + getUniqueID() + "]: "
						+ entry.getKey() + ": " + Arrays.toString(entry.getValue()));
			}
		}
	}
}