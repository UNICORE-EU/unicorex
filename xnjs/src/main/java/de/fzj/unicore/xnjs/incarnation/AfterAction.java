/*
 * Copyright (c) 2010 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 14-12-2010
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
package de.fzj.unicore.xnjs.incarnation;

import de.fzj.unicore.xnjs.ems.ExecutionException;


/**
 * Generic action that is invoked to modify the already produced 
 * TSI script.
 * @author golbi
 */
public interface AfterAction extends Action
{
	public String filter(RootCtxBean context, String script) 
			throws ExecutionException, Exception;
}
