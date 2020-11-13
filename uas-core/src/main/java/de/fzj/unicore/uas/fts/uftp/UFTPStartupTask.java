/*
 * Copyright (c) 2012 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package de.fzj.unicore.uas.fts.uftp;

import org.apache.logging.log4j.Logger;

import de.fzj.unicore.wsrflite.Kernel;
import eu.unicore.util.Log;

/**
 * Inits UFTP configuration and connection to UFTPD
 * 
 * @author K. Benedyczak
 */
public class UFTPStartupTask implements Runnable {

	private static Logger logger = Log.getLogger(Log.CONFIGURATION, UFTPStartupTask.class);
	
	private Kernel kernel;
	
	public UFTPStartupTask(Kernel kernel) {
		this.kernel = kernel;
	}
	public void run() {
		try {
			setupUFTPConnector();
		}catch(Throwable ex) {
			Log.logException("UFTP connector could not be initialised, UFTP will not be available.", ex, logger);
		}
	}
	
	protected void setupUFTPConnector() {
		UFTPProperties cfg = new UFTPProperties(kernel.getContainerProperties().getRawProperties());
		if(!cfg.getBooleanValue(UFTPProperties.PARAM_ENABLE_UFTP)) {
			logger.info("UFTP is disabled.");
			return;
		}
		
		kernel.addConfigurationHandler(UFTPProperties.class, cfg);
		UFTPConnector connector = new UFTPConnector(kernel, cfg);
		kernel.setAttribute(UFTPConnector.class, connector);
	}
}
