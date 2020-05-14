/*
 * Copyright (c) 2012 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package de.fzj.unicore.uas.xtreemfs;

import de.fzj.unicore.wsrflite.server.AbstractStartupTask;

/**
 * Loads configuration.
 * @author K. Benedyczak
 */
public class XtreemFSStartupTask extends AbstractStartupTask {
	@Override
	public void run() {
		XtreemProperties cfg = new XtreemProperties(getKernel().getContainerProperties().getRawProperties());
		getKernel().addConfigurationHandler(XtreemProperties.class, cfg);
	}
}
