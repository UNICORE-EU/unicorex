package de.fzj.unicore.uas.util;

public interface MessageWriter {

	/**
	 * log a message only if the UCC is running in verbose mode
	 * @param message
	 */
	public abstract void verbose(String message);

	/**
	 * output a message
	 * @param message
	 */
	public abstract void message(String message);

	/**
	 * log an error message
	 * @param message
	 */
	public abstract void error(String message, Throwable cause);

	
	/**
	 * check if the message writer is running in verbose mode
	 * @return <code>true</code> if verbose mode is enabed
	 */
	public boolean isVerbose();
}