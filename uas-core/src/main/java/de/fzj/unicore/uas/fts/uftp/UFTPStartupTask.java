/*
 * Copyright (c) 2012 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package de.fzj.unicore.uas.fts.uftp;

import de.fzj.unicore.wsrflite.server.AbstractStartupTask;

/**
 * Inits UFTP configuration and connection to UFTPD
 * 
 * @author K. Benedyczak
 */
public class UFTPStartupTask extends AbstractStartupTask {

	public void run() {
		UFTPProperties cfg = new UFTPProperties(getKernel().getContainerProperties().getRawProperties());
		getKernel().addConfigurationHandler(UFTPProperties.class, cfg);
		UFTPConnector connector = new UFTPConnector(getKernel(), cfg);
		getKernel().setAttribute(UFTPConnector.class, connector);
	}
}
