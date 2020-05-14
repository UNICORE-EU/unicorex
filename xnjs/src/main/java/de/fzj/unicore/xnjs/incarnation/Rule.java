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

import org.springframework.expression.Expression;

/**
 * Generic rule for use in before or after TSI script creation logic. Immutable.
 * @author golbi
 *
 */
public class Rule<T extends Action>
{
	private Expression condition;
	private boolean finishOnHit;
	private List<T> actions;

	public Rule(Expression condition, boolean finishOnHit, List<T> actions)
	{
		this.condition = condition;
		this.finishOnHit = finishOnHit;
		this.actions = Collections.unmodifiableList(actions);
	}

	public Expression getCondition()
	{
		return condition;
	}

	public boolean isFinishOnHit()
	{
		return finishOnHit;
	}

	public List<T> getActions()
	{
		return actions;
	}
}
