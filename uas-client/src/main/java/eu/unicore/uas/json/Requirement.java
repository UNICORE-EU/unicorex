package eu.unicore.uas.json;

import org.json.JSONObject;

/**
 * allows to check whether a given endpoint matches a requirement,
 * for example runs the needed operating system
 *
 * @author schuller
 */
public interface Requirement {

	/**
	 * check the properties and decide if it fits the requirement
	 * 
	 * @param properties - the resource properties
	 * @return <code>true</code> if requirement is fulfilled, <code>false</code> otherwise
	 */
	public boolean isFulfilled(JSONObject properties);

	/**
	 * returns a human-readable description of the requirement for logging purposes
	 */
	public String getDescription();

}
