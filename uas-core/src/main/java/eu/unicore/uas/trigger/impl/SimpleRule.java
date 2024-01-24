package eu.unicore.uas.trigger.impl;

import eu.unicore.uas.trigger.Action;
import eu.unicore.uas.trigger.Rule;

/**
 * 
 * @author schuller
 */
public class SimpleRule implements Rule {

	private final String name; 
	private final String match;
	private final Action action;
	
	public SimpleRule(String name, String match, Action action){
		this.match=match;
		this.name=name;
		this.action=action;
	}
	
	@Override
	public boolean matches(String filePath, TriggerContext context) {
		return filePath.matches(match);
	}

	@Override
	public Action getAction() {
		return action;
	}

	public String getName(){
		return name;
	}
	
	public void begin(){}
	
	public void commit(){}
	
	public String toString(){
		return "Rule <"+name+"> matches <"+match+"> Action "+action.toString();
	}
}
