package de.fzj.unicore.xnjs.tsi;

import de.fzj.unicore.xnjs.ems.ExecutionException;

public interface BatchMode {

	/**
	 * set the TSI into batch mode
	 * 
	 * @throws ExecutionException
	 */
	public void startBatch() throws ExecutionException;
	
	/**
	 * commit the batch of commands
	 * 
	 * @throws ExecutionException
	 */
	public void commitBatch() throws ExecutionException;
	
	/**
	 * if something goes wrong, this method can be used to reset the TSI
	 * 
	 * @throws ExecutionException
	 */
	public void cleanupBatch();
	
}
