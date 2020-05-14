package de.fzj.unicore.uas.impl.job;

import de.fzj.unicore.xnjs.ems.Action;

/**
 * helper interface to allow re-using renderers for different
 * purposes
 * 
 * @author schuller
 */
public interface XnjsActionBacked {

	public Action getXNJSAction();
	
}
