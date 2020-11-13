package de.fzj.unicore.uas.util;

import org.apache.logging.log4j.Logger;

import eu.unicore.util.Log;

public class LogMessageWriter implements MessageWriter {

	private final Logger log;
	
	public LogMessageWriter(Logger log){
		this.log = log;
	}
	
	@Override
	public void verbose(String message) {
		log.debug(message);
	}

	@Override
	public void message(String message) {
		log.info(message);
	}

	@Override
	public void error(String message, Throwable cause) {
		Log.logException(message, cause, log);
	}

	@Override
	public boolean isVerbose() {
		return log.isDebugEnabled();
	}

}
