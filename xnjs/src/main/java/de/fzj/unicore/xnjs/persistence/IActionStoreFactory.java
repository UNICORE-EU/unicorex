package de.fzj.unicore.xnjs.persistence;

import de.fzj.unicore.xnjs.XNJS;

public interface IActionStoreFactory {
	
	/**
	 * Get/create the action store identified by the given ID<br>
	 * 
	 * @param identifier
	 * @return IActionStore
	 */
	public IActionStore getInstance(String identifier, XNJS config) throws Exception ;
}
