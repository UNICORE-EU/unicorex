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
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * This action simply evaluates the SpEL expression. 
 * @author golbi
 */
public class SpelAction implements BeforeAction
{
	public static final String ID = "spel";
	
	private Expression expression;
	private StandardEvaluationContext context;
	
	public SpelAction(String expr) throws ParseException
	{
		this.expression = TweakerConfiguration.parseSpEL(
				new SpelExpressionParser(), expr);
		this.context = new StandardEvaluationContext();
	}

	@Override
	public void invoke(RootCtxBean ctx) throws Exception
	{

		expression.getValue(context, ctx);
	}

	@Override
	public String getActionDefinition()
	{
		return expression.getExpressionString();
	}
}
