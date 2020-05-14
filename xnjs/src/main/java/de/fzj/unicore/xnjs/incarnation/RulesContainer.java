/*
 * Copyright (c) 2010 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 14-12-2010
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
package de.fzj.unicore.xnjs.incarnation;

import java.util.Collections;
import java.util.List;

/**
 * Contains rules that are to be invoked before TSI script creation. Immutable.
 * Two instances of this class are used: one to be invoked before TSI script creation
 * and 2nd to be invoked after the script is created to further change it.
 *  
 * @author golbi
 */
public class RulesContainer<T extends Action>
{
	private List<Rule<T>> rules;
	
	public RulesContainer(List<Rule<T>> rules)
	{
		this.rules = Collections.unmodifiableList(rules);
	}
	
	public List<Rule<T>> getRulesList()
	{
		return rules;
	}
}
