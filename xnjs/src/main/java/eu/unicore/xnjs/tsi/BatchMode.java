package eu.unicore.xnjs.tsi;

import eu.unicore.xnjs.ems.ExecutionException;

public interface BatchMode {

	/**
	 * set the TSI into batch mode
	 * 
	 * @throws ExecutionException
	 */
	public void startBatch() throws ExecutionException;
	
	/**
	 * commit the batch of commands and return the reply from the TSI
	 * 
	 * @throws ExecutionException
	 */
	public String commitBatch() throws ExecutionException;
	
	/**
	 * if something goes wrong, this method can be used to reset the TSI
	 * 
	 * @throws ExecutionException
	 */
	public void cleanupBatch();
	
}
