package eu.unicore.uas.trigger.impl;

import eu.unicore.uas.trigger.TriggeredAction;
import eu.unicore.uas.trigger.Rule;

/**
 * 
 * @author schuller
 */
public class SimpleRule implements Rule {

	private final String name; 
	private final String match;
	private final TriggeredAction action;
	
	public SimpleRule(String name, String match, TriggeredAction action){
		this.match=match;
		this.name=name;
		this.action=action;
	}
	
	@Override
	public boolean matches(String filePath, TriggerContext context) {
		return filePath.matches(match);
	}

	@Override
	public TriggeredAction getAction() {
		return action;
	}

	public String getName(){
		return name;
	}
	
	public void begin(){}
	
	public void commit(){}
	
	public String toString(){
		return "Rule <"+name+"> matches <"+match+"> Action "+String.valueOf(action);
	}
}
