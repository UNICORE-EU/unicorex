/*
 * Copyright (c) 2010 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 14-12-2010
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
package de.fzj.unicore.xnjs.incarnation;

/**
 * Generic action which is invoked when rule condition is hit. 
 * @author golbi
 */
public abstract interface Action
{
	/**
	 * Mostly for unit tests
	 */
	public abstract String getActionDefinition();
}
