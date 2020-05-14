/*
 * Copyright (c) 2010 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 14-12-2010
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
package de.fzj.unicore.xnjs.incarnation;

import java.io.CharArrayWriter;
import java.io.Reader;
import java.io.StringReader;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import de.fzj.unicore.xnjs.ems.ExecutionException;



/**
 * This action evaluates the provided Groovy script. Original TSI script is feed to 
 * the Groovy stdin, Groovy stdout is treat as the filtered TSI script.
 * 
 * @author golbi
 */
public class GroovyFilter extends GroovyActionBase implements AfterAction
{
	public static final String ID = "groovy";
	
	public GroovyFilter(String groovyScript)
	{
		super(groovyScript);
	}

	@Override
	public String filter(RootCtxBean context, String script) throws ScriptException, ExecutionException
	{
		ScriptEngineManager factory = new ScriptEngineManager();
	        ScriptEngine engine = factory.getEngineByName("groovy");

	        Reader r = new StringReader(script);
	        CharArrayWriter w = new CharArrayWriter();
	        engine.getContext().setWriter(w);
	        
	        fillContext(context, engine);
	        engine.put("input", r);
	        
	        try
	        {
	        	engine.eval(groovyScript);
	        } catch (ScriptException se) 
	        {
	        	handleException(se);
	        }

		return w.toString();
	}
}
