package eu.unicore.uas.json;

import org.json.JSONObject;

/**
 * allows to check whether a given target system matches a requirement, for example
 * runs the needed operating system
 *   
 * @author schuller
 */
public interface Requirement {

	/**
	 * check the properties and decide if it fits the requirement
	 * 
	 * @param tss - the TSS resource properties document
	 * @return <code>true</code> if the TSS is OK to use, <code>false</code> otherwise
	 */
	public boolean isFulfilled(JSONObject properties);
	
	/**
	 * returns a human-readable description of the requirement for logging purposes
	 */
	public String getDescription();
	
}
