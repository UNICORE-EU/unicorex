/*
 * Copyright (c) 2010 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 14-12-2010
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
package de.fzj.unicore.xnjs.incarnation;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import de.fzj.unicore.xnjs.beans.inctweak.Action;
import de.fzj.unicore.xnjs.beans.inctweak.AfterScriptDocument.AfterScript;
import de.fzj.unicore.xnjs.beans.inctweak.BeforeScriptDocument.BeforeScript;
import de.fzj.unicore.xnjs.beans.inctweak.IncarnationTweakerDocument;
import de.fzj.unicore.xnjs.ems.ExecutionException;
import de.fzj.unicore.xnjs.incarnation.IncarnationTweaker.ErrorHolder;
import de.fzj.unicore.xnjs.util.LogUtil;

/**
 * Handles reading the configuration of the incarnation tweaker.
 * 
 * @author golbi
 */
public class TweakerConfiguration {
	private static final Logger log = LogUtil.getLogger(LogUtil.XNJS, TweakerConfiguration.class);
	
	private File file;
	private static final int DEF_UPDATE_CHECK = 10000;
	private static final int DEF_MAX_WAIT_FOR_SCRIPT = 10000;
	private long lastRulesLoadTime;
	private RulesContainer<BeforeAction> beforeRules;
	private RulesContainer<AfterAction> afterRules;
	private ExpressionParser spelParser;
	private int maxWaitForScript;
	
	public TweakerConfiguration(File file, ScheduledExecutorService executor, 
			int updateInterval, int maxWaitForScript) throws IOException, XmlException, ParseException {
		init(file, maxWaitForScript);
		startConfigWatcher(updateInterval, executor);
	}

	public TweakerConfiguration(File file, ScheduledExecutorService executor) throws IOException, XmlException, ParseException {
		init(file, DEF_MAX_WAIT_FOR_SCRIPT);
		startConfigWatcher(DEF_UPDATE_CHECK, executor);
	}
	
	private void init(File file, int maxWait) throws IOException, XmlException, ParseException {
		this.file = file;
		this.maxWaitForScript = maxWait;
		spelParser = new SpelExpressionParser();
		parse();
	}

	private void startConfigWatcher(long interval, ScheduledExecutorService executor)
	{
		Runnable r = new FileWatcher();
		executor.scheduleWithFixedDelay(r, interval, interval, TimeUnit.MILLISECONDS);
	}

	
	private void parse() throws IOException, XmlException, ParseException 
	{
		IncarnationTweakerDocument mainDoc = IncarnationTweakerDocument.Factory.parse(
				new BufferedInputStream(new FileInputStream(file)));
		de.fzj.unicore.xnjs.beans.inctweak.IncarnationTweakerDocument.IncarnationTweaker main = mainDoc.getIncarnationTweaker();
		RulesContainer<BeforeAction> bRules; 
		RulesContainer<AfterAction> aRules; 
		
		BeforeScript bs = main.getBeforeScript();
		bRules = parseBeforeSection(bs);
			
		AfterScript as = main.getAfterScript();
		aRules = parseAfterSection(as);

		test(bRules, aRules);
		
		updateContainers(bRules, aRules);
		lastRulesLoadTime = System.currentTimeMillis();
	}
	
	private void test(RulesContainer<BeforeAction> bRules, RulesContainer<AfterAction> aRules) throws IOException
	{
		log.info("Will perform a dry run on just loaded incarnation rules.");
		TestContextBean root = new TestContextBean();
		ErrorHolder eh = new ErrorHolder();
		try
		{
			IncarnationTweaker.preScriptInternal(root, bRules, eh);
		} catch (ExecutionException e)
		{
			log.debug("Dry run of the BEFORE-SCRIPT section finished with the " +
					"ExecutionException. This might be the expected behavior.", e);
		}
		if (eh.isError())
			throw new IOException("Configuration was read correctly but dry run of the" +
					" incarnation rules from the BEFORE-SCRIPT section finished " +
					"with an error. See previous log " +
					"messages for details and recheck your configuration.");
		
		root = new TestContextBean();
		try
		{
			IncarnationTweaker.postScriptInternal(root, "#!/bin/bash", aRules, eh);
		} catch (ExecutionException e)
		{
			log.debug("Dry run of the AFTER-SCRIPT section finished with the " +
					"ExecutionException. This might be the expected behavior.", e);
		}
		if (eh.isError())
			throw new IOException("Configuration was read correctly but dry run of the" +
					" incarnation rules from the AFTER-SCRIPT section finished with an error. See previous log " +
					"messages for details and recheck your configuration.");
		log.info("Dry run of incarnation rules suceeded.");		
	}
	
	private synchronized void updateContainers(RulesContainer<BeforeAction> bRules, 
			RulesContainer<AfterAction> aRules)
	{
		beforeRules = bRules;
		afterRules = aRules;
	}
	
	public synchronized RulesContainer<BeforeAction> getBeforeRules()
	{
		return beforeRules;
	}

	public synchronized RulesContainer<AfterAction> getAfterRules()
	{
		return afterRules;
	}

	
	private RulesContainer<BeforeAction> parseBeforeSection(BeforeScript bs) 
		throws ParseException, IOException
	{
		List<Rule<BeforeAction>> bRules = 
			new ArrayList<Rule<BeforeAction>>();
		RulesContainer<BeforeAction> beforeRules = 
			new RulesContainer<BeforeAction>(bRules);
		if (bs == null)
			return beforeRules;
		de.fzj.unicore.xnjs.beans.inctweak.RuleDocument.Rule[] xmlRules = bs.getRuleArray();
		if (xmlRules == null || xmlRules.length == 0)
			return beforeRules;
		for (de.fzj.unicore.xnjs.beans.inctweak.RuleDocument.Rule xmlRule: xmlRules)
		{
			String rawCond = xmlRule.getCondition();
			if (rawCond == null)
				throw new ParseException("Rule without a condition found", -1);
			Expression e = parseSpEL(spelParser, rawCond);
			boolean stopOnHit = xmlRule.isSetFinishOnHit() ? 
					xmlRule.getFinishOnHit() : false;

			List<BeforeAction> actions = new ArrayList<BeforeAction>();

			Action[] xmlActions = xmlRule.getActionArray();
			if (xmlActions == null || xmlActions.length == 0)
				throw new ParseException("Rule without action found", -1);
			for (Action xmlAction: xmlActions)
			{
				BeforeAction action = getBeforeAction(xmlAction);
				actions.add(action);
			}
			bRules.add(new Rule<BeforeAction>(e, stopOnHit, actions));
		}
		
		return beforeRules;
	}

	
	private RulesContainer<AfterAction> parseAfterSection(AfterScript bs) 
		throws ParseException, IOException
	{
		List<Rule<AfterAction>> aRules = 
			new ArrayList<Rule<AfterAction>>();
		RulesContainer<AfterAction> afterRules = 
			new RulesContainer<AfterAction>(aRules);
		if (bs == null)
			return afterRules;

		de.fzj.unicore.xnjs.beans.inctweak.RuleDocument.Rule[] xmlRules = bs.getRuleArray();
		if (xmlRules == null || xmlRules.length == 0)
			return afterRules;
		for (de.fzj.unicore.xnjs.beans.inctweak.RuleDocument.Rule xmlRule: xmlRules)
		{
			String rawCond = xmlRule.getCondition();
			if (rawCond == null)
				throw new ParseException("Rule without a condition found", -1);
			Expression e = parseSpEL(spelParser, rawCond);
			boolean stopOnHit = xmlRule.isSetFinishOnHit() ? 
					xmlRule.getFinishOnHit() : false;

			List<AfterAction> actions = new ArrayList<AfterAction>();

			Action[] xmlActions = xmlRule.getActionArray();
			if (xmlActions == null || xmlActions.length == 0)
				throw new ParseException("Rule without action found", -1);
			for (Action xmlAction: xmlActions)
			{
				AfterAction action = getAfterAction(xmlAction);
				actions.add(action);
			}
			aRules.add(new Rule<AfterAction>(e, stopOnHit, actions));
		}

		return afterRules;
	}

	private BeforeAction getBeforeAction(Action xmlAction) throws ParseException, IOException
	{
		String value = xmlAction.getStringValue();
		if (value == null)
			throw new ParseException("Rule with empty action contents found", -1);
		String type = xmlAction.getType();
		if (type == null || type.equals(SpelAction.ID))
			return new SpelAction(value);
		else if (type.equals(GroovyAction.ID))
			return new GroovyAction(value);
		else if (type.equals(SpelScriptAction.ID))
			return new SpelScriptAction(value, maxWaitForScript);
		else if (type.equals(GroovyFileAction.ID))
			return new GroovyFileAction(value);
		throw new ParseException("Found action with an unknown type: <" 
				+ type + ">", -1);
	}

	private AfterAction getAfterAction(Action xmlAction) throws ParseException, IOException
	{
		String value = xmlAction.getStringValue();
		if (value == null)
			throw new ParseException("Rule with empty action contents found", -1);
		String type = xmlAction.getType();
		if (type == null || type.equals(SpelFilter.ID))
			return new SpelFilter(value, maxWaitForScript);
		else if (type.equals(GroovyFilter.ID))
			return new GroovyFilter(value);
		else if (type.equals(GroovyFileFilter.ID))
			return new GroovyFileFilter(value);
		throw new ParseException("Found action with an unknown type: <" 
				+ type + ">", -1);
	}

	public static Expression parseSpEL(ExpressionParser spelParser, String expr) 
		throws ParseException
	{
		return parseSpEL(spelParser, expr, null);
	}
	
	public static Expression parseSpEL(ExpressionParser spelParser, String expr, ParserContext parserCtx) 
		throws ParseException
	{
		try
		{
			return parserCtx == null ? spelParser.parseExpression(expr) : 
				spelParser.parseExpression(expr, parserCtx);
		} catch(org.springframework.expression.ParseException e)
		{
			throw new ParseException(e.getMessage(), e.getPosition());
		} catch(Exception ee)
		{
			throw new ParseException("Other problem parsing SpEL expression '" + 
					expr + "': " + ee.toString(), -1);
		}
	}
	
	public long getLastRulesLoadTime()
	{
		return lastRulesLoadTime;
	}

	private class FileWatcher implements Runnable
	{
		private final File target;
		private long lastAccessed;
		
		public FileWatcher()
		{
			target = file;
			lastAccessed = target.lastModified();
		}
		
		public void run()
		{
			if (target.lastModified() <= lastAccessed)
				return;
			lastAccessed=target.lastModified();
			log.info("Dynamic incarnation configuration file was modified, re-configuring.");
			try
			{
				parse();
			} catch (Exception e)
			{
				log.error("Error reading new incarnation configuration (file " 
					+ file + "): "	+ e.toString(), e);
			} 
		}
	}
}









