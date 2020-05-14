/*
 * Copyright (c) 2010 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 14-12-2010
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
package de.fzj.unicore.xnjs.incarnation;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import de.fzj.unicore.xnjs.ems.ExecutionException;



/**
 * This action simply evaluates the Groovy expression. 
 * @author golbi
 */
public class GroovyAction extends GroovyActionBase implements BeforeAction
{
	public static final String ID = "groovy";

	public GroovyAction(String groovyScript)
	{
		super(groovyScript);
	}

	@Override
	public void invoke(RootCtxBean ctx) throws ExecutionException, ScriptException
	{
		ScriptEngineManager factory = new ScriptEngineManager();
		ScriptEngine engine = factory.getEngineByName("groovy");

		fillContext(ctx, engine);

		try
		{
			engine.eval(groovyScript);
		} catch (ScriptException se) 
		{
			handleException(se);
		}
	}
}
