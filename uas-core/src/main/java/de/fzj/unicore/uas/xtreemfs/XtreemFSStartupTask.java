/*
 * Copyright (c) 2012 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package de.fzj.unicore.uas.xtreemfs;

import de.fzj.unicore.wsrflite.Kernel;

/**
 * Loads configuration.
 * @author K. Benedyczak
 */
public class XtreemFSStartupTask implements Runnable {
	
	private Kernel kernel;
	
	public XtreemFSStartupTask(Kernel kernel) {
		this.kernel = kernel;
	}
	
	
	@Override
	public void run() {
		XtreemProperties cfg = new XtreemProperties(kernel.getContainerProperties().getRawProperties());
		kernel.addConfigurationHandler(XtreemProperties.class, cfg);
	}
}
