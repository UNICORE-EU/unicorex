/*
 * Copyright (c) 2010 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 14-12-2010
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
package de.fzj.unicore.xnjs.incarnation;

import java.io.File;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Singleton;

import org.apache.log4j.Logger;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import com.google.inject.Inject;

import de.fzj.unicore.xnjs.XNJSProperties;
import de.fzj.unicore.xnjs.ems.Action;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.idb.ApplicationInfo;
import de.fzj.unicore.xnjs.idb.IDB;
import de.fzj.unicore.xnjs.util.LogUtil;

/**
 * Provides processing of extended admin-defined incarnation logic.
 * 
 * @author golbi
 *
 */
@Singleton
public class IncarnationTweaker implements ITweaker {
	
	private static final Logger log = LogUtil.getLogger(LogUtil.XNJS, IncarnationTweaker.class);
	
	private boolean enabled = false;
	
	private TweakerConfiguration config;
	
	@Inject
	public IncarnationTweaker(XNJSProperties xnjsProperties, ScheduledExecutorService es){
		File configFile = xnjsProperties.getFileValue(XNJSProperties.INCARNATION_TWEAKER_CONFIG,false);
		if (configFile == null)
			return;
		try {
			config = new TweakerConfiguration(configFile, es);
		} catch (Exception e) {
			log.error("Can not load configuration of the incarnation tweaking subsystem," +
					" it WILL BE DISABLED. Cause: " + e.getMessage(), e);
			return;
		}
		
		enabled = true;
	}

	@Override
	public void preScript(ApplicationInfo appDescription, Action job, IDB idb) 
			throws ExecutionException 
	{
		if (!enabled)
			return;
		RootCtxBean root = new RootCtxBean(appDescription, job, idb);
		preScriptInternal(root, config.getBeforeRules(), new ErrorHolder());
	}


	/**
	 * Used from preScript or during configuration loading to verify if any errors are 
	 * found during a dry run. 
	 */
	static void preScriptInternal(RootCtxBean root, RulesContainer<BeforeAction> rulesC, ErrorHolder eh) throws ExecutionException 
	{
		StandardEvaluationContext ctx = new StandardEvaluationContext(root);
		List<Rule<BeforeAction>> rules = rulesC.getRulesList();
		int i=0;
		
		for (Rule<BeforeAction> r: rules)
		{
			i++;
			if (!checkCondition(r.getCondition(), ctx, i, "before", eh))
				continue;
			List<BeforeAction> actions = r.getActions();
			int j=0;
			for (BeforeAction action: actions)
			{
				j++;
				try
				{
					action.invoke(root);
				} catch (ExecutionException ee) 
				{ 
					throw ee;
				} catch (Exception e) 
				{
					log.error("Invocation of an action " + j + 
						" of the before script rule " + i + 
						" thrown an error: " + e + 
						". This error is ignored and processing is continued.", e);
					eh.setError(true);
				}
			}
			
			if (r.isFinishOnHit())
				break;
		}
	}

	@Override
	public String postScript(ApplicationInfo appDescription, Action job, 
			IDB idb, String script) throws ExecutionException 
	{
		if (!enabled)
			return script;
		RootCtxBean root = new RootCtxBean(appDescription, job, idb);
		return postScriptInternal(root, script, config.getAfterRules(), 
				new ErrorHolder());
	}

	static String postScriptInternal(RootCtxBean root, String script, 
			RulesContainer<AfterAction> rulesC, ErrorHolder eh) throws ExecutionException 
	{
		StandardEvaluationContext ctx = new StandardEvaluationContext(root);
		List<Rule<AfterAction>> rules = rulesC.getRulesList();
		int i=0;
		for (Rule<AfterAction> r: rules)
		{
			i++;
			if (!checkCondition(r.getCondition(), ctx, i, "after", eh))
				continue;
			
			List<AfterAction> actions = r.getActions();
			int j=0;
			for (AfterAction action: actions)
			{
				j++;
				try
				{
					log.debug("Will filter the original TSI script:\n" + script);
					String filtered = action.filter(root, script);
					script = filtered;
					log.debug("Replaced original script with:\n" + script);
				} catch (ExecutionException ee) 
				{ 
					throw ee;
				} catch (Exception e)
				{
					log.error("Invocation of an action " + j + 
						" of the after-script rule " + i + 
						" thrown an error: " + e + 
						". This error is ignored and processing is continued.", e);
					eh.setError(true);
				}
			}
			
			if (r.isFinishOnHit())
				break;
		}
		return script;
	}

	
	
	private static boolean checkCondition(Expression condition, StandardEvaluationContext ctx, int i, String info, ErrorHolder eh)
	{
		Object condResult;
		try
		{
			condResult = condition.getValue(ctx);
		} catch (Exception e)
		{
			log.error("Skipping the " + info + "-script rule number " + i + 
				" as evaluation of its condition finished with " +
				"an error: " + e.toString(), e);
			eh.setError(true);
			return false;
		}
		if (!(condResult instanceof Boolean) || condResult == null)
		{
			log.error("Skipping the " + info + "-script rule number " + i + 
				" as evaluation of its condition finished with " +
				"a non boolean result. Result type is: " + 
				condResult == null ? "null" : condResult.getClass().getName());
			eh.setError(true);
			return false;
		}
		boolean result = (Boolean) condResult;
		if (log.isDebugEnabled())
			log.debug(info + "-script rule number " + i + 
					" condition returned " + result);
		return result;
	}
	
	static class ErrorHolder
	{
		private boolean error = false;

		public void setError(boolean error)
		{
			this.error = error;
		}

		public boolean isError()
		{
			return error;
		}
	}
}
