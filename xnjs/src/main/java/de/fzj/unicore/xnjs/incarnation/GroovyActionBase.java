/*
 * Copyright (c) 2010 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 17-12-2010
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
package de.fzj.unicore.xnjs.incarnation;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.util.LogUtil;

public class GroovyActionBase implements Action
{
	private static Logger log = LogUtil.getLogger(LogUtil.XNJS, GroovyFilter.class);
	protected String groovyScript;
	protected PropertyDescriptor[] properties;
	
	public GroovyActionBase(String groovyScript)
	{
		this.groovyScript = groovyScript;
		BeanInfo bi;
		try
		{
			bi = Introspector.getBeanInfo(RootCtxBean.class);
		} catch (IntrospectionException e) //really shouldn't happen
		{
			throw new RuntimeException(e);
		}
		properties = bi.getPropertyDescriptors();
	}

	protected void fillContext(RootCtxBean rootCtx, ScriptEngine engine)
	{
		for (PropertyDescriptor property: properties)
		{
			try
			{
				Object o = property.getReadMethod().invoke(rootCtx);
				engine.put(property.getName(), o);
			} catch (Exception e)
			{
				log.error("BUG: can't set property " + property.getName()
						+ " for groovy script evaluation", e);
			}
		}
	}
	
	protected void handleException(ScriptException se) throws ScriptException, ExecutionException
	{
		Throwable cause = se.getCause();
		while (cause != null && (cause instanceof ScriptException))
			cause = cause.getCause();
		if (cause == null)
			throw se;
		if (cause instanceof ExecutionException)
			throw (ExecutionException)cause;
		throw se;
	}
	
	@Override
	public String getActionDefinition()
	{
		return groovyScript;
	}
}
