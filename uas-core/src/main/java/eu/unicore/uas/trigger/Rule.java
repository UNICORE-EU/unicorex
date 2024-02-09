package eu.unicore.uas.trigger;

import eu.unicore.uas.trigger.impl.TriggerContext;

/**
 * a rule describes what is happening when certain conditions are met
 * 
 * @author schuller
 */
public interface Rule {

	/**
	 * invoked at the very start of processing
	 */
	public void begin();
	
	/**
	 * for informational use, each rule has a name
	 * 
	 * @return the name of the rule
	 */
	public String getName();
	
	/**
	 * checks whether this rule applies to the given file path
	 * 
	 * @param filePath - the full file path relative to storage root
	 * @param context - current context
	 * @return <code>true</code> if this rule matches
	 */
	public boolean matches(String filePath, TriggerContext context);
	
	/**
	 * get the action to be executed if the rule matches
	 */
	public TriggeredAction getAction();
	
	/**
	 * invoked at the very end of processing
	 */
	public void commit();
	

}
