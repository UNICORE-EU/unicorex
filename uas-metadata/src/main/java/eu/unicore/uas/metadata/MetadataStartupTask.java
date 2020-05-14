/*
 * Copyright (c) 2012 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package eu.unicore.uas.metadata;

import de.fzj.unicore.wsrflite.server.AbstractStartupTask;

/**
 * Loads configuration
 * @author K. Benedyczak
 */
public class MetadataStartupTask extends AbstractStartupTask {
	@Override
	public void run() {
		MetadataProperties props = new MetadataProperties(
				getKernel().getContainerProperties().getRawProperties());
		getKernel().addConfigurationHandler(MetadataProperties.class, props);
	}

}
