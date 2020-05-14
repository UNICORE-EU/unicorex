/*
 * Copyright (c) 2010 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 14-12-2010
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
package de.fzj.unicore.xnjs.incarnation;

import java.text.ParseException;

import org.springframework.expression.Expression;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;


/**
 * This action evaluates SpEL expression, result is treated as a command line. The
 * command is simply invoked.
 * 
 * @author golbi
 *
 */
public class SpelScriptAction extends ScriptActionBase implements BeforeAction
{
	public static final String ID = "script";
	private Expression expression;
	
	
	public SpelScriptAction(String expr, int maxWait) throws ParseException
	{
		super(maxWait);
		this.expression = TweakerConfiguration.parseSpEL(new SpelExpressionParser(),
				expr, new TemplateParserContext("${", "}"));
	}

	@Override
	public void invoke(RootCtxBean root) throws Exception
	{
		Process p = runScript(expression, root);
		
		TimeLimitedThreadBase tlthread = new TimeLimitedThreadBase(p);
		Thread thread = new Thread(tlthread);
		thread.start();
		
		waitFor(p, thread, tlthread);
	}
	
	@Override
	public String getActionDefinition()
	{
		return expression.getExpressionString();
	}

}
