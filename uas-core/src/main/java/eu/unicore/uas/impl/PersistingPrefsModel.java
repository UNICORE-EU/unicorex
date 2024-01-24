package eu.unicore.uas.impl;

import java.util.Map;

/**
 * Adds persistence of the initial user's preferences to the resource.
 * Such preferences are used as default preferences assuming that the user has not
 * specified other.
 */
public class PersistingPrefsModel extends UASBaseModel {

	private static final long serialVersionUID = 1L;
	
	private Map<String, String[]> storedPreferences;

	public Map<String, String[]> getStoredPreferences() {
		return storedPreferences;
	}

	public void setStoredPreferences(Map<String, String[]> storedPreferences) {
		this.storedPreferences = storedPreferences;
	}

}
